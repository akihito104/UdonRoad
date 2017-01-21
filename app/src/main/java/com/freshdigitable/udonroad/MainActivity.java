/*
 * Copyright (c) 2016. Matsuda, Akihit (akihito104)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.freshdigitable.udonroad;

import android.content.Intent;
import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.freshdigitable.udonroad.StatusViewBase.OnUserIconClickedListener;
import com.freshdigitable.udonroad.TimelineFragment.OnFetchTweets;
import com.freshdigitable.udonroad.TweetInputFragment.TweetSendable;
import com.freshdigitable.udonroad.TweetInputFragment.TweetType;
import com.freshdigitable.udonroad.databinding.ActivityMainBinding;
import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.SortedCache;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;
import com.freshdigitable.udonroad.subscriber.ConfigRequestWorker;
import com.freshdigitable.udonroad.subscriber.RequestWorkerBase;
import com.freshdigitable.udonroad.subscriber.StatusRequestWorker;
import com.freshdigitable.udonroad.subscriber.UserFeedbackSubscriber;
import com.squareup.picasso.Picasso;

import java.util.Arrays;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.User;
import twitter4j.auth.AccessToken;

import static com.freshdigitable.udonroad.TweetInputFragment.TYPE_DEFAULT;
import static com.freshdigitable.udonroad.TweetInputFragment.TYPE_QUOTE;
import static com.freshdigitable.udonroad.TweetInputFragment.TYPE_REPLY;

/**
 * MainActivity shows home timeline for authorized user.
 *
 * Created by akihit
 */
public class MainActivity extends AppCompatActivity
    implements TweetSendable, OnUserIconClickedListener, OnFetchTweets, FabHandleable {
  private static final String TAG = MainActivity.class.getSimpleName();
  private ActivityMainBinding binding;
  private ActionBarDrawerToggle actionBarDrawerToggle;
  private TimelineFragment<Status> tlFragment;
  private TweetInputFragment tweetInputFragment;

  @Inject
  TwitterApi twitterApi;
  private SortedCache<Status> homeTimeline;
  @Inject
  StatusRequestWorker<SortedCache<Status>> statusRequestWorker;
  @Inject
  UserStreamUtil userStream;
  @Inject
  ConfigRequestWorker configRequestWorker;
  @Inject
  UserFeedbackSubscriber userFeedback;
  @Inject
  AppSettingStore appSettings;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    InjectionUtil.getComponent(this).inject(this);
    appSettings.open();
    final AccessToken accessToken = appSettings.getCurrentUserAccessToken();
    if (accessToken == null) {
      startActivity(new Intent(this, OAuthActivity.class));
      finish();
      return;
    }
    twitterApi.setOAuthAccessToken(accessToken);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      supportRequestWindowFeature(Window.FEATURE_CONTENT_TRANSITIONS);
    }
    binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    binding.navDrawer.setNavigationItemSelectedListener(new OnNavigationItemSelectedListener() {
      @Override
      public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_home) {
          Log.d(TAG, "home is selected");
          binding.navDrawerLayout.closeDrawer(binding.navDrawer);
        } else if (itemId == R.id.menu_mention) {
          Log.d(TAG, "mention is selected");
          binding.navDrawerLayout.closeDrawer(binding.navDrawer);
        } else if (itemId == R.id.menu_fav) {
          Log.d(TAG, "fav is selected");
          binding.navDrawerLayout.closeDrawer(binding.navDrawer);
        } else if (itemId == R.id.menu_license) {
          startActivity(new Intent(getApplicationContext(), LicenseActivity.class));
          binding.navDrawerLayout.closeDrawer(binding.navDrawer);
        }
        return false;
      }
    });

    binding.ffab.hide();
    setupHomeTimeline();
    setupTweetInputView();
    setupNavigationDrawer();
  }

  private void setupHomeTimeline() {
    statusRequestWorker.open("home");
    homeTimeline = statusRequestWorker.getCache();
    homeTimeline.clearPool();

    tlFragment = new TimelineFragment<>();
    tlFragment.setSortedCache(homeTimeline);

    configRequestWorker.open();
    configRequestWorker.setup(new Action0() {
      @Override
      public void call() {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.main_timeline_container, tlFragment)
            .commit();
      }
    });
  }

  private Subscription subscription;

  private void setupNavigationDrawer() {
    attachToolbar(binding.mainToolbar);
    subscription = configRequestWorker.getAuthenticatedUser()
        .subscribe(new Action1<User>() {
          @Override
          public void call(User user) {
            setupNavigationDrawer(user);
          }
        });
  }

  private void attachToolbar(Toolbar toolbar) {
    actionBarDrawerToggle = new ActionBarDrawerToggle(this,
        binding.navDrawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
      @Override
      public void onDrawerOpened(View drawerView) {
        super.onDrawerOpened(drawerView);
        tlFragment.stopScroll();
      }

      @Override
      public void onDrawerClosed(View drawerView) {
        super.onDrawerClosed(drawerView);
        tlFragment.startScroll();
      }
    };
    actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
    binding.navDrawerLayout.addDrawerListener(actionBarDrawerToggle);
    actionBarDrawerToggle.syncState();
  }

  private void setupNavigationDrawer(@Nullable User user) {
    if (user == null) {
      return;
    }

    final CombinedScreenNameTextView account
        = (CombinedScreenNameTextView) binding.navDrawer.findViewById(R.id.nav_header_account);
    if (account != null) {
      account.setNames(user);
    }
    final ImageView icon
        = (ImageView) binding.navDrawer.findViewById(R.id.nav_header_icon);
    if (icon != null) {
      Picasso.with(getApplicationContext())
          .load(user.getProfileImageURLHttps())
          .resizeDimen(R.dimen.nav_drawer_header_icon, R.dimen.nav_drawer_header_icon)
          .into(icon);
    }
  }

  @Override
  public void onPostCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
    super.onPostCreate(savedInstanceState, persistentState);
    actionBarDrawerToggle.syncState();
  }

  @Override
  protected void onStart() {
    super.onStart();
    userStream.connect(homeTimeline);

    binding.mainToolbar.setTitle("Home");
    setSupportActionBar(binding.mainToolbar);
    final ActionBar supportActionBar = getSupportActionBar();
    if (supportActionBar != null) {
      supportActionBar.setDisplayHomeAsUpEnabled(true);
      supportActionBar.setHomeButtonEnabled(true);
    }

    setupActionMap();
    userFeedback.registerRootView(binding.mainTimelineContainer);
  }

  private StatusDetailFragment statusDetail;

  private void showStatusDetail(long status) {
    if (statusDetail != null && statusDetail.isVisible()) {
      return;
    }
    statusDetail = StatusDetailFragment.getInstance(status);
    getSupportFragmentManager().beginTransaction()
        .hide(tlFragment)
        .add(R.id.main_timeline_container, statusDetail)
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        .commit();
    tlFragment.stopScroll();
    binding.ffab.getMenu().findItem(R.id.iffabMenu_main_detail).setEnabled(false);
//    detailAction = actionMap.remove(Direction.LEFT);
//    UserAction.setupFlingableFAB(binding.ffab, actionMap, getApplicationContext());
  }

  private boolean hideStatusDetail() {
    if (statusDetail == null) {
      return false;
    }
    getSupportFragmentManager().beginTransaction()
        .remove(statusDetail)
        .show(tlFragment)
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
        .commit();
    statusDetail = null;
    tlFragment.startScroll();
//    actionMap.put(Direction.LEFT, detailAction);
//    UserAction.setupFlingableFAB(binding.ffab, actionMap, getApplicationContext());
    binding.ffab.getMenu().findItem(R.id.iffabMenu_main_detail).setEnabled(true);
    return true;
  }

  @Override
  protected void onStop() {
    super.onStop();
    binding.ffab.setOnIffabItemSelectedListener(null);
    userFeedback.unregisterRootView(binding.mainTimelineContainer);
    if (subscription != null && !subscription.isUnsubscribed()) {
      subscription.unsubscribe();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (binding != null) {
      userStream.disconnect();
      binding.ffab.clear();
      binding.navDrawer.setNavigationItemSelectedListener(null);
      configRequestWorker.close();
      statusRequestWorker.close();
      appSettings.close();
      userFeedback.unsubscribe();
    }
  }

  @Override
  public void onBackPressed() {
    if (binding.navDrawerLayout.isDrawerOpen(binding.navDrawer)) {
      binding.navDrawerLayout.closeDrawer(binding.navDrawer);
      return;
    }
    if (tweetInputFragment != null && tweetInputFragment.isStatusInputViewVisible()) {
      tweetInputFragment.collapseStatusInputView();
      cancelWritingSelected();
      return;
    }
    if (statusDetail != null && statusDetail.isVisible()) {
      hideStatusDetail();
      return;
    }
    if (tlFragment.isTweetSelected()) {
      tlFragment.clearSelectedTweet();
      return;
    }
    super.onBackPressed();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    actionBarDrawerToggle.onConfigurationChanged(newConfig);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int itemId = item.getItemId();
    if (itemId == R.id.action_write) {
      sendStatusSelected(TYPE_DEFAULT, -1);
    } else if (itemId == R.id.action_cancel) {
      cancelWritingSelected();
    }
    return actionBarDrawerToggle.onOptionsItemSelected(item)
        || super.onOptionsItemSelected(item);
  }

  private void sendStatusSelected(@TweetType int type, long statusId) {
    if (binding.ffab.getVisibility() == View.VISIBLE) {
      binding.ffab.hide();
    }
    tlFragment.stopScroll();
    if (type != TYPE_DEFAULT) {
      tweetInputFragment.stretchTweetInputView(type, statusId);
    }
    if (type == TYPE_REPLY) {
      binding.mainToolbar.setTitle("返信する");
    } else if (type == TYPE_QUOTE) {
      binding.mainToolbar.setTitle("コメントする");
    } else {
      binding.mainToolbar.setTitle("いまどうしてる？");
    }
  }

  private void cancelWritingSelected() {
    tlFragment.startScroll();
    if (tlFragment.isTweetSelected() && tlFragment.isVisible()) {
      binding.ffab.show();
    }
    binding.mainToolbar.setTitle("Home");
  }

  private void setupTweetInputView() {
    tweetInputFragment = TweetInputFragment.create();
    tweetInputFragment.setTweetSendFab(binding.mainSendTweet);
    getSupportFragmentManager().beginTransaction()
        .add(R.id.main_appbar_container, tweetInputFragment)
        .commit();
  }

  @Override
  public void setupInput(@TweetType int type, long statusId) {
    sendStatusSelected(type, statusId);
  }

  @Override
  public Observable<Status> observeUpdateStatus(Observable<Status> updateStatusObservable) {
    return updateStatusObservable.doOnNext(new Action1<Status>() {
      @Override
      public void call(Status status) {
        cancelWritingSelected();
      }
    });
  }

  @Override
  public void fetchTweet() {
    statusRequestWorker.fetchHomeTimeline();
  }

  @Override
  public void fetchTweet(Paging paging) {
    statusRequestWorker.fetchHomeTimeline(paging);
  }

  private void setupActionMap() {
    binding.ffab.setOnIffabItemSelectedListener(new OnIffabItemSelectedListener() {
      @Override
      public void onItemSelected(@NonNull MenuItem item) {
        final int itemId = item.getItemId();
        final long selectedTweetId = tlFragment.getSelectedTweetId();
        if (itemId == R.id.iffabMenu_main_fav) {
          statusRequestWorker.createFavorite(selectedTweetId);
        } else if (itemId == R.id.iffabMenu_main_rt) {
          statusRequestWorker.retweetStatus(selectedTweetId);
        } else if (itemId == R.id.iffabMenu_main_favRt) {
          Observable.concatDelayError(Arrays.asList(
              statusRequestWorker.observeCreateFavorite(selectedTweetId),
              statusRequestWorker.observeRetweetStatus(selectedTweetId))
          ).subscribe(RequestWorkerBase.<Status>nopSubscriber());
        } else if (itemId == R.id.iffabMenu_main_detail) {
          showStatusDetail(selectedTweetId);
        } else if (itemId == R.id.iffabMenu_main_reply) {
          sendStatusSelected(TYPE_REPLY, selectedTweetId);
        } else if (itemId == R.id.iffabMenu_main_quote) {
          sendStatusSelected(TYPE_QUOTE, selectedTweetId);
        }
      }
    });
  }

  @Override
  public void showFab() {
    binding.ffab.show();
  }

  @Override
  public void hideFab() {
    binding.ffab.hide();
  }

  @Override
  public void onUserIconClicked(View view, User user) {
    if (tweetInputFragment != null && tweetInputFragment.isStatusInputViewVisible()) {
      return;
    }
    binding.ffab.hide();
    tlFragment.stopScroll();
    UserInfoActivity.start(this, user, view);
  }
}