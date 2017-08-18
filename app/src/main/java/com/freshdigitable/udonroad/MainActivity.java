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
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.freshdigitable.udonroad.TweetInputFragment.TweetSendable;
import com.freshdigitable.udonroad.TweetInputFragment.TweetType;
import com.freshdigitable.udonroad.databinding.ActivityMainBinding;
import com.freshdigitable.udonroad.databinding.NavHeaderBinding;
import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;
import com.freshdigitable.udonroad.listitem.OnUserIconClickedListener;
import com.freshdigitable.udonroad.listitem.TwitterCombinedName;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.subscriber.AppSettingRequestWorker;
import com.freshdigitable.udonroad.subscriber.ConfigRequestWorker;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.disposables.Disposable;
import twitter4j.Status;
import twitter4j.User;

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
    implements TweetSendable, OnUserIconClickedListener, FabHandleable, SnackbarCapable {
  private static final String TAG = MainActivity.class.getSimpleName();
  private ActivityMainBinding binding;
  private ActionBarDrawerToggle actionBarDrawerToggle;
  private TimelineFragment<?> tlFragment;
  private TweetInputFragment tweetInputFragment;

  @Inject
  ConfigRequestWorker configRequestWorker;
  @Inject
  AppSettingRequestWorker appSettingRequestWorker;
  @Inject
  AppSettingStore appSetting;
  private TimelineContainerSwitcher timelineContainerSwitcher;
  private NavHeaderBinding navHeaderBinding;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    InjectionUtil.getComponent(this).inject(this);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      supportRequestWindowFeature(Window.FEATURE_CONTENT_TRANSITIONS);
    }
    binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
    navHeaderBinding = DataBindingUtil.inflate(LayoutInflater.from(getApplicationContext()), R.layout.nav_header, null, false);
    binding.navDrawer.addHeaderView(navHeaderBinding.getRoot());

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    setupHomeTimeline();
    timelineContainerSwitcher = new TimelineContainerSwitcher(binding.mainTimelineContainer, tlFragment, binding.ffab);
    setupTweetInputView();
    setupNavigationDrawer();

    setSupportActionBar(binding.mainToolbar);
    final ActionBar supportActionBar = getSupportActionBar();
    if (supportActionBar != null) {
      supportActionBar.setDisplayHomeAsUpEnabled(true);
      supportActionBar.setHomeButtonEnabled(true);
    }
  }

  private void setupHomeTimeline() {
    tlFragment = TimelineFragment.getInstance(HOME);
    configRequestWorker.setup().subscribe(
        () -> getSupportFragmentManager().beginTransaction()
            .replace(R.id.main_timeline_container, tlFragment)
            .commit(),
        throwable -> Log.e(TAG, "config.setup: ", throwable));
  }

  private Disposable subscription;

  private void setupNavigationDrawer() {
    attachToolbar(binding.mainToolbar);
    appSettingRequestWorker.verifyCredentials();
    binding.navDrawer.setNavigationItemSelectedListener(item -> {
      int itemId = item.getItemId();
      if (itemId == R.id.drawer_menu_home) {
        Log.d(TAG, "home is selected");
        timelineContainerSwitcher.showMain();
        binding.navDrawerLayout.closeDrawer(binding.navDrawer);
      } else if (itemId == R.id.drawer_menu_lists) {
        timelineContainerSwitcher.showOwnedLists(appSetting.getCurrentUserId());
        binding.navDrawerLayout.closeDrawer(binding.navDrawer);
      } else {
        if (itemId == R.id.drawer_menu_license) {
          startActivity(new Intent(getApplicationContext(), LicenseActivity.class));
          binding.navDrawerLayout.closeDrawer(binding.navDrawer);
        }
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

  private void setupNavigationDrawerHeader(User user) {
    navHeaderBinding.navHeaderAccount.setNames(new TwitterCombinedName(user));
    Picasso.with(getApplicationContext())
        .load(user.getProfileImageURLHttps())
        .resizeDimen(R.dimen.nav_drawer_header_icon, R.dimen.nav_drawer_header_icon)
        .into(navHeaderBinding.navHeaderIcon);
    navHeaderBinding.navHeaderIcon.setOnClickListener(v -> UserInfoActivity.start(this, user, v));
  }

  @Override
  public void onPostCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
    super.onPostCreate(savedInstanceState, persistentState);
    actionBarDrawerToggle.syncState();
  }

  @Override
  protected void onStart() {
    super.onStart();
    appSetting.open();
    subscription = appSetting.observeCurrentUser()
        .subscribe(this::setupNavigationDrawerHeader,
            e -> Log.e(TAG, "setupNavigationDrawer: ", e));

    setupActionMap();
    timelineContainerSwitcher.setOnContentChangedListener((type, title) -> {
      if (type == TimelineContainerSwitcher.ContentType.MAIN) {
        tlFragment.startScroll();
      } else {
        tlFragment.stopScroll();
      }
      binding.mainToolbar.setTitle(title);
    });
  }

  @Override
  protected void onStop() {
    super.onStop();
    navHeaderBinding.navHeaderIcon.setOnClickListener(null);
    if (subscription != null && !subscription.isDisposed()) {
      subscription.dispose();
    }
    appSetting.close();
    timelineContainerSwitcher.setOnContentChangedListener(null);
    binding.ffab.setOnIffabItemSelectedListener(null);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (binding != null) {
      iffabItemSelectedListeners.clear();
      binding.ffab.clear();
      binding.navDrawer.setNavigationItemSelectedListener(null);
      configRequestWorker.shrink();
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
    if (timelineContainerSwitcher.clearSelectedCursorIfNeeded()) {
      return;
    }
    if (timelineContainerSwitcher.popBackStackTimelineContainer()) {
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

  private CharSequence prevTitle;

  private void sendStatusSelected(@TweetType int type, long statusId) {
    if (binding.ffab.getVisibility() == View.VISIBLE) {
      binding.ffab.hide();
    }
    tlFragment.stopScroll();
    if (type != TYPE_DEFAULT) {
      tweetInputFragment.stretchTweetInputView(type, statusId);
    }
    prevTitle = binding.mainToolbar.getTitle();
    if (type == TYPE_REPLY) {
      binding.mainToolbar.setTitle(R.string.title_reply);
    } else if (type == TYPE_QUOTE) {
      binding.mainToolbar.setTitle(R.string.title_comment);
    } else {
      binding.mainToolbar.setTitle(R.string.title_tweet);
    }
  }

  private void cancelWritingSelected() {
    tlFragment.startScroll();
    if (tlFragment.isItemSelected() && tlFragment.isVisible()) {
      binding.ffab.show();
    }
    binding.mainToolbar.setTitle(prevTitle);
  }

  private void setupTweetInputView() {
    tweetInputFragment = TweetInputFragment.create();
    tweetInputFragment.setTweetSendFab(binding.mainSendTweet);
    getSupportFragmentManager().beginTransaction()
        .add(R.id.main_appbar_container, tweetInputFragment)
        .commit();
  }

  @Override
  public void onTweetComplete(Status updated) {
    cancelWritingSelected();
  }

  private void setupActionMap() {
    binding.ffab.setOnIffabItemSelectedListener(item -> {
      final int itemId = item.getItemId();
      final long selectedTweetId = timelineContainerSwitcher.getSelectedTweetId();
      if (itemId == R.id.iffabMenu_main_detail) {
        timelineContainerSwitcher.showStatusDetail(selectedTweetId);
      } else if (itemId == R.id.iffabMenu_main_reply) {
        sendStatusSelected(TYPE_REPLY, selectedTweetId);
        tlFragment.scrollToSelectedItem();
      } else if (itemId == R.id.iffabMenu_main_quote) {
        sendStatusSelected(TYPE_QUOTE, selectedTweetId);
        tlFragment.scrollToSelectedItem();
      } else if (itemId == R.id.iffabMenu_main_conv) {
        timelineContainerSwitcher.showConversation(selectedTweetId);
      }
      for (OnIffabItemSelectedListener l : iffabItemSelectedListeners) {
        l.onItemSelected(item);
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
  public void setCheckedFabMenuItem(@IdRes int itemId, boolean checked) {
    binding.ffab.getMenu().findItem(itemId).setChecked(checked);
  }

  private final List<OnIffabItemSelectedListener> iffabItemSelectedListeners = new ArrayList<>();

  @Override
  public void addOnItemSelectedListener(OnIffabItemSelectedListener listener) {
    iffabItemSelectedListeners.add(listener);
  }

  @Override
  public void removeOnItemSelectedListener(OnIffabItemSelectedListener listener) {
    iffabItemSelectedListeners.remove(listener);
  }

  @Override
  public void onUserIconClicked(View view, User user) {
    if (tweetInputFragment != null && tweetInputFragment.isStatusInputViewVisible()) {
      return;
    }
    tlFragment.stopScroll();
    UserInfoActivity.start(this, user, view);
  }

  @Override
  public View getRootView() {
    return binding.mainTimelineContainer;
  }
}