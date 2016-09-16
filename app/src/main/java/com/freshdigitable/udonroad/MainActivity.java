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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.freshdigitable.udonroad.StatusViewBase.OnUserIconClickedListener;
import com.freshdigitable.udonroad.TimelineFragment.OnFetchTweets;
import com.freshdigitable.udonroad.TweetInputFragment.TweetSendable;
import com.freshdigitable.udonroad.TweetInputFragment.TweetType;
import com.freshdigitable.udonroad.databinding.ActivityMainBinding;
import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.datastore.SortedCache;
import com.freshdigitable.udonroad.ffab.OnFlingListener.Direction;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.TwitterAPIConfiguration;
import twitter4j.User;

import static com.freshdigitable.udonroad.TweetInputFragment.TYPE_DEFAULT;
import static com.freshdigitable.udonroad.TweetInputFragment.TYPE_QUOTE;
import static com.freshdigitable.udonroad.TweetInputFragment.TYPE_REPLY;

/**
 * MainActivity shows home timeline for authorized user.
 *
 * Created by akihit
 */
public class MainActivity
    extends AppCompatActivity
    implements TweetSendable, OnUserIconClickedListener, OnFetchTweets, FabHandleable {
  private static final String TAG = MainActivity.class.getSimpleName();
  private ActivityMainBinding binding;
  private ActionBarDrawerToggle actionBarDrawerToggle;
  private TimelineFragment<Status> tlFragment;
  private TweetInputFragment tweetInputFragment;

  @Inject
  TwitterApi twitterApi;
  @Inject
  ConfigStore configStore;
  @Inject
  SortedCache<Status> homeTimeline;
  private TimelineSubscriber<SortedCache<Status>> timelineSubscriber;
  private UserStreamUtil userStream;
  private final Map<Direction, UserAction> actionMap = new HashMap<>();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    InjectionUtil.getComponent(this).inject(this);
    if (!twitterApi.loadAccessToken()) {
      startActivity(new Intent(this, OAuthActivity.class));
      finish();
      return;
    }
    userStream = new UserStreamUtil(homeTimeline);
    InjectionUtil.getComponent(this).inject(userStream);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
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
        }
        return false;
      }
    });

    binding.ffab.hide();
    setupHomeTimeline();
  }

  private void setupAppBar(@TweetType int type, long statusId) {
    tweetInputFragment = TweetInputFragment.create(type, statusId);
    tweetInputFragment.setTweetSendFab(binding.mainSendTweet);
    getSupportFragmentManager().beginTransaction()
        .replace(R.id.main_appbar_container, tweetInputFragment)
        .commit();
  }

  private void tearDownTweetInputView() {
    if (tweetInputFragment == null) {
      return;
    }
    tweetInputFragment.collapseStatusInputView();
    tweetInputFragment.setTweetSendFab(null);
    getSupportFragmentManager().beginTransaction()
        .remove(tweetInputFragment)
        .commit();
    tweetInputFragment = null;
  }

  private void setupHomeTimeline() {
    homeTimeline.open(getApplicationContext(), "home");
    homeTimeline.clear();
    timelineSubscriber = new TimelineSubscriber<>(twitterApi, homeTimeline,
        new FeedbackSubscriber.SnackbarFeedback(binding.mainTimelineContainer));

    tlFragment = new TimelineFragment<>();
    tlFragment.setSortedCache(homeTimeline);
    getSupportFragmentManager().beginTransaction()
        .replace(R.id.main_timeline_container, tlFragment)
        .commit();
  }

  @Override
  public void onClicked(View view, User user) {
    if (tweetInputFragment != null && tweetInputFragment.isStatusInputViewVisible()) {
      return;
    }
    binding.ffab.hide();
    tlFragment.stopScroll();
    UserInfoActivity.start(this, user, view);
  }

  private void attachToolbar(Toolbar toolbar) {
    actionBarDrawerToggle = new ActionBarDrawerToggle(this,
        binding.navDrawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
      @Override
      public void onDrawerOpened(View drawerView) {
        super.onDrawerOpened(drawerView);
        setupNavigationDrawer();
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

  private void setupNavigationDrawer() {
    final User authenticatedUser = configStore.getAuthenticatedUser();
    setupNavigationDrawer(authenticatedUser);
  }

  private void setupNavigationDrawer(@Nullable User user) {
    if (user == null) {
      return;
    }

    final TextView account
        = (TextView) binding.navDrawer.findViewById(R.id.nav_header_account);
    account.setText(user.getScreenName());
    final ImageView icon
        = (ImageView) binding.navDrawer.findViewById(R.id.nav_header_icon);
    Picasso.with(binding.navDrawer.getContext())
        .load(user.getProfileImageURLHttps()).fit()
        .into(icon);
  }

  @Override
  public void onPostCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
    super.onPostCreate(savedInstanceState, persistentState);
    actionBarDrawerToggle.syncState();
  }

  @Override
  protected void onStart() {
    super.onStart();
    configStore.open(getApplicationContext());
    userStream.connect();
    twitterApi.verifyCredentials()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<User>() {
          @Override
          public void call(User user) {
            configStore.setAuthenticatedUser(user);
            setupNavigationDrawer(user);
          }
        }, new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            Log.e(TAG, "call: ", throwable);
          }
        });
    twitterApi.getTwitterAPIConfiguration()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<TwitterAPIConfiguration>() {
          @Override
          public void call(TwitterAPIConfiguration configuration) {
            configStore.setTwitterAPIConfig(configuration);
          }
        }, new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            Log.e(TAG, "call: ", throwable);
          }
        });

    binding.mainToolbar.setTitle("Home");
    setSupportActionBar(binding.mainToolbar);
    final ActionBar supportActionBar = getSupportActionBar();
    if (supportActionBar != null) {
      supportActionBar.setDisplayHomeAsUpEnabled(true);
      supportActionBar.setHomeButtonEnabled(true);
    }

    setupActionMap();
    UserAction.setupFlingableFAB(binding.ffab, actionMap, getApplicationContext());
  }

  private StatusDetailFragment statusDetail;

  private void showStatusDetail(long status) {
    statusDetail = StatusDetailFragment.getInstance(status);
    getSupportFragmentManager().beginTransaction()
        .hide(tlFragment)
        .add(R.id.main_timeline_container, statusDetail)
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        .commit();
    tlFragment.stopScroll();
    if (tlFragment.isTweetSelected()) {
      binding.ffab.hide();
    }
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
    if (tlFragment.isTweetSelected()) {
      binding.ffab.show();
    }
    return true;
  }

  @Override
  protected void onResume() {
    Log.d(TAG, "onResume: ");
    super.onResume();
    attachToolbar(binding.mainToolbar);
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  @Override
  protected void onStop() {
    super.onStop();
    configStore.close();
    binding.ffab.setOnFlingListener(null);
    actionMap.clear();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (userStream != null) {
      userStream.disconnect();
    }
    if (binding != null) {
      binding.ffab.setOnFlingListener(null);
      binding.navDrawer.setNavigationItemSelectedListener(null);
    }
    tearDownTweetInputView();
    homeTimeline.close();
  }

  @Override
  public void onBackPressed() {
    if (binding.navDrawerLayout.isDrawerOpen(binding.navDrawer)) {
      binding.navDrawerLayout.closeDrawer(binding.navDrawer);
      return;
    }
    if (tweetInputFragment != null && tweetInputFragment.isStatusInputViewVisible()) {
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

  private MenuItem sendStatusMenuItem;
  private MenuItem cancelMenuItem;

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.appbar_menu, menu);
    sendStatusMenuItem = menu.findItem(R.id.action_write);
    cancelMenuItem = menu.findItem(R.id.action_cancel);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int itemId = item.getItemId();
    if (itemId == R.id.action_heading) {
      headingSelected();
    } else if (itemId == R.id.action_write) {
      sendStatusSelected(TYPE_DEFAULT, -1);
    } else if (itemId == R.id.action_cancel) {
      cancelWritingSelected();
    }
    return actionBarDrawerToggle.onOptionsItemSelected(item)
        || super.onOptionsItemSelected(item);
  }

  private void headingSelected() {
    if (tlFragment.isVisible()) {
      tlFragment.scrollToTop();
    }
  }

  private void sendStatusSelected(@TweetType int type, long statusId) {
    sendStatusMenuItem.setVisible(false);
    cancelMenuItem.setVisible(true);

    if (binding.ffab.getVisibility() == View.VISIBLE) {
      binding.ffab.hide();
    }
    tlFragment.stopScroll();
    setupAppBar(type, statusId);
    if (type == TYPE_REPLY) {
      binding.mainToolbar.setTitle("返信する");
    } else if (type == TYPE_QUOTE) {
      binding.mainToolbar.setTitle("コメントする");
    } else {
      binding.mainToolbar.setTitle("いまどうしてる？");
    }
  }

  private void cancelWritingSelected() {
    sendStatusMenuItem.setVisible(true);
    cancelMenuItem.setVisible(false);

    tlFragment.startScroll();
    tearDownTweetInputView();
    if (tlFragment.isTweetSelected() && tlFragment.isVisible()) {
      binding.ffab.show();
    }
    binding.mainToolbar.setTitle("Home");
  }

  private void showToast(String text) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
  }

  @Override
  public void setupInput(@TweetType int type, long statusId) {
    sendStatusSelected(type, statusId);
  }

  @Override
  public void observeUpdateStatus(Observable<Status> updateStatusObservable) {
    updateStatusObservable.subscribe(
        new Action1<Status>() {
          @Override
          public void call(Status status) {
            cancelWritingSelected();
          }
        },
        new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            showToast("send tweet: failure...");
            Log.e(TAG, "update status: " + throwable);
          }
        }
    );
  }

  @Override
  public void fetchTweet() {
    timelineSubscriber.fetchHomeTimeline();
  }

  @Override
  public void fetchTweet(Paging paging) {
    timelineSubscriber.fetchHomeTimeline(paging);
  }

  private void setupActionMap() {
    actionMap.put(Direction.UP, new UserAction(ActionResource.FAV, new Runnable() {
      @Override
      public void run() {
        timelineSubscriber.createFavorite(tlFragment.getSelectedTweetId());
      }
    }));
    actionMap.put(Direction.RIGHT, new UserAction(ActionResource.RETWEET, new Runnable() {
      @Override
      public void run() {
        timelineSubscriber.retweetStatus(tlFragment.getSelectedTweetId());
      }
    }));
    actionMap.put(Direction.UP_RIGHT, new UserAction());
    actionMap.put(Direction.LEFT, new UserAction(ActionResource.MENU, new Runnable() {
      @Override
      public void run() {
        showStatusDetail(tlFragment.getSelectedTweetId());
      }
    }));
    actionMap.put(Direction.DOWN, new UserAction(ActionResource.REPLY, new Runnable() {
      @Override
      public void run() {
        sendStatusSelected(TYPE_REPLY, tlFragment.getSelectedTweetId());
      }
    }));
    actionMap.put(Direction.DOWN_RIGHT, new UserAction(ActionResource.QUOTE, new Runnable() {
      @Override
      public void run() {
        sendStatusSelected(TYPE_QUOTE, tlFragment.getSelectedTweetId());
      }
    }));
  }

  @Override
  public void showFab() {
    binding.ffab.show();
  }

  @Override
  public void hideFab() {
    binding.ffab.hide();
  }
}