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
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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

import com.freshdigitable.udonroad.StatusViewBase.OnUserIconClickedListener;
import com.freshdigitable.udonroad.TimelineFragment.OnFetchTweets;
import com.freshdigitable.udonroad.TweetInputFragment.TweetSendable;
import com.freshdigitable.udonroad.TweetInputFragment.TweetType;
import com.freshdigitable.udonroad.databinding.ActivityMainBinding;
import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.SortedCache;
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
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.User;
import twitter4j.auth.AccessToken;

import static com.freshdigitable.udonroad.StoreType.HOME;
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

    setupHomeTimeline();
    setupTweetInputView();
    setupNavigationDrawer();
  }

  private void setupHomeTimeline() {
    statusRequestWorker.open(HOME.storeName);
    SortedCache<Status> homeTimeline = statusRequestWorker.getCache();
    homeTimeline.clearPool();

    tlFragment = new TimelineFragment<>();
    tlFragment.setSortedCache(homeTimeline);

    configRequestWorker.open();
    configRequestWorker.setup(() -> getSupportFragmentManager().beginTransaction()
        .replace(R.id.main_timeline_container, tlFragment)
        .commit());
  }

  private Subscription subscription;

  private void setupNavigationDrawer() {
    attachToolbar(binding.mainToolbar);
    subscription = configRequestWorker.getAuthenticatedUser()
        .subscribe(this::setupNavigationDrawerHeader);
    binding.navDrawer.setNavigationItemSelectedListener(item -> {
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

  private void setupNavigationDrawerHeader(@Nullable User user) {
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
    userStream.connect(StoreType.HOME.storeName);

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
    if (popBackStackTimelineContainer()) {
      return;
    }
    if (clearSelectedCursorIfNeeded(tlFragment)) {
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

  private void showStatusDetail(long statusId) {
    tlFragment.stopScroll();
    StatusDetailFragment statusDetail = StatusDetailFragment.getInstance(statusId);
    replaceTimelineContainer("detail_" + Long.toString(statusId), statusDetail);
    switchFFABMenuTo(R.id.iffabMenu_main_conv);
    binding.ffab.transToToolbar();
  }

  private void hideStatusDetail() {
    switchFFABMenuTo(R.id.iffabMenu_main_detail);
    binding.ffab.transToFAB();
  }

  @Inject
  StatusRequestWorker<SortedCache<Status>> conversationRequestWorker;

  private void showConversation(long statusId) {
    if (conversationRequestWorker.isOpened()) {
      conversationRequestWorker.close();
    }
    final String name = StoreType.CONVERSATION.prefix() + Long.toString(statusId);
    conversationRequestWorker.open(name);

    final TimelineFragment<Status> conversationFragment = new TimelineFragment<>();
    conversationFragment.setSortedCache(conversationRequestWorker.getCache());
    replaceTimelineContainer(name, conversationFragment);
    conversationRequestWorker.fetchConversations(statusId);
    binding.ffab.hide();
    switchFFABMenuTo(R.id.iffabMenu_main_detail);
  }

  private void hideConversation() {
    conversationRequestWorker.getCache().clear();
    conversationRequestWorker.close();
    switchFFABMenuTo(R.id.iffabMenu_main_conv);
    binding.ffab.show();
  }

  private void replaceTimelineContainer(String name, Fragment fragment) {
    getSupportFragmentManager().beginTransaction()
        .replace(R.id.main_timeline_container, fragment, name)
        .addToBackStack(null)
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        .commit();
  }

  private boolean popBackStackTimelineContainer() {
    final FragmentManager fm = getSupportFragmentManager();
    final int backStackEntryCount = fm.getBackStackEntryCount();
    if (backStackEntryCount <= 0) {
      return false;
    }
    final Fragment fragment = fm.findFragmentById(R.id.main_timeline_container);
    if (fragment instanceof TimelineFragment) {
      if (clearSelectedCursorIfNeeded((TimelineFragment) fragment)) {
        return true;
      }
      fm.popBackStack();
      hideConversation();
      return true;
    } else if (fragment instanceof StatusDetailFragment) {
      fm.popBackStack();
      if (backStackEntryCount == 1) {
        tlFragment.startScroll();
      }
      hideStatusDetail();
      return true;
    }
    throw new IllegalStateException("unknown fragment is added...");
  }

  private boolean clearSelectedCursorIfNeeded(TimelineFragment tf) {
    if (tf.isTweetSelected()) {
      tf.clearSelectedTweet();
      return true;
    }
    return false;
  }

  @Override
  public void setupInput(@TweetType int type, long statusId) {
    sendStatusSelected(type, statusId);
  }

  @Override
  public Observable<Status> observeUpdateStatus(Observable<Status> updateStatusObservable) {
    return updateStatusObservable.doOnNext(status -> cancelWritingSelected());
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
    binding.ffab.setOnIffabItemSelectedListener(item -> {
      final int itemId = item.getItemId();
      final long selectedTweetId = tlFragment.getSelectedTweetId();
      if (itemId == R.id.iffabMenu_main_fav) {
        if (!item.isChecked()) {
          statusRequestWorker.createFavorite(selectedTweetId);
        } else {
          statusRequestWorker.destroyFavorite(selectedTweetId);
        }
      } else if (itemId == R.id.iffabMenu_main_rt) {
        if (!item.isChecked()) {
          statusRequestWorker.retweetStatus(selectedTweetId);
        } else {
          statusRequestWorker.destroyRetweet(selectedTweetId);
        }
      } else if (itemId == R.id.iffabMenu_main_favRt) {
        Observable.concatDelayError(Arrays.asList(
            statusRequestWorker.observeCreateFavorite(selectedTweetId),
            statusRequestWorker.observeRetweetStatus(selectedTweetId))
        ).subscribe(RequestWorkerBase.nopSubscriber());
      } else if (itemId == R.id.iffabMenu_main_detail) {
        showStatusDetail(selectedTweetId);
      } else if (itemId == R.id.iffabMenu_main_reply) {
        sendStatusSelected(TYPE_REPLY, selectedTweetId);
      } else if (itemId == R.id.iffabMenu_main_quote) {
        sendStatusSelected(TYPE_QUOTE, selectedTweetId);
      } else if (itemId == R.id.iffabMenu_main_conv) {
        showConversation(selectedTweetId);
      }
    });
  }

  private static final int[] FFAB_MENU_LEFT_SETS
      = {R.id.iffabMenu_main_conv, R.id.iffabMenu_main_detail};

  private void switchFFABMenuTo(@IdRes int targetItem) {
    final Menu menu = binding.ffab.getMenu();
    for (@IdRes int menuItemId : FFAB_MENU_LEFT_SETS) {
      menu.findItem(menuItemId).setEnabled(menuItemId == targetItem);
    }
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
  public void setCheckedFabMenuItem(@IdRes int itemId, boolean checked) {
    binding.ffab.getMenu().findItem(itemId).setChecked(checked);
  }

  @Override
  public void onUserIconClicked(View view, User user) {
    if (tweetInputFragment != null && tweetInputFragment.isStatusInputViewVisible()) {
      return;
    }
    tlFragment.stopScroll();
    UserInfoActivity.start(this, user, view);
  }
}