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
import android.support.annotation.NonNull;
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

import com.freshdigitable.udonroad.TimelineContainerSwitcher.ContentType;
import com.freshdigitable.udonroad.TimelineContainerSwitcher.OnContentChangedListener;
import com.freshdigitable.udonroad.TweetInputFragment.TweetSendable;
import com.freshdigitable.udonroad.TweetInputFragment.TweetType;
import com.freshdigitable.udonroad.databinding.ActivityMainBinding;
import com.freshdigitable.udonroad.databinding.NavHeaderBinding;
import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;
import com.freshdigitable.udonroad.listitem.OnUserIconClickedListener;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.subscriber.ConfigRequestWorker;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

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
    implements TweetSendable, OnUserIconClickedListener, FabHandleable, SnackbarCapable,
    TimelineFragment.OnItemClickedListener, OnSpanClickListener {
  private static final String TAG = MainActivity.class.getSimpleName();
  private ActivityMainBinding binding;
  private ActionBarDrawerToggle actionBarDrawerToggle;
  private TimelineFragment<?> tlFragment;

  @Inject
  ConfigRequestWorker configRequestWorker;
  @Inject
  AppSettingStore appSetting;
  private TimelineContainerSwitcher timelineContainerSwitcher;
  private DrawerNavigator drawerNavigator;
  private ToolbarTweetInputToggle toolbarTweetInputToggle;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    InjectionUtil.getComponent(this).inject(this);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      supportRequestWindowFeature(Window.FEATURE_CONTENT_TRANSITIONS);
    }
    binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    setupHomeTimeline();
    setupNavigationDrawer();
    setupTweetInputView();

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
    timelineContainerSwitcher = new TimelineContainerSwitcher(
        binding.mainTimelineContainer, tlFragment, binding.ffab);
  }

  private void setupNavigationDrawer() {
    attachToolbar(binding.mainToolbar);

    final NavHeaderBinding navHeaderBinding = DataBindingUtil.inflate(
        LayoutInflater.from(this), R.layout.nav_header, null, false);
    drawerNavigator = new DrawerNavigator(binding.navDrawerLayout, binding.navDrawer, navHeaderBinding, appSetting);
  }

  private void attachToolbar(Toolbar toolbar) {
    actionBarDrawerToggle = new ActionBarDrawerToggle(this,
        binding.navDrawerLayout, toolbar, R.string.navDesc_openDrawer, R.string.navDesc_closeDrawer) {
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

  @Override
  public void onPostCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
    super.onPostCreate(savedInstanceState, persistentState);
    actionBarDrawerToggle.syncState();
  }

  @Override
  protected void onStart() {
    super.onStart();
    appSetting.open();
    setupActionMap();
    drawerNavigator.changeCurrentUser();
    timelineContainerSwitcher.setOnContentChangedListener(getOnContentChangedListener());
    drawerNavigator.setOnDefaultItemSelectedListener(item -> {
      int itemId = item.getItemId();
      if (itemId == R.id.drawer_menu_home) {
        timelineContainerSwitcher.showMain();
        drawerNavigator.closeDrawer();
      } else if (itemId == R.id.drawer_menu_lists) {
        timelineContainerSwitcher.showOwnedLists();
        drawerNavigator.closeDrawer();
      } else if (itemId == R.id.drawer_menu_license) {
        startActivity(new Intent(getApplicationContext(), LicenseActivity.class));
        drawerNavigator.closeDrawer();
      } else if (itemId == R.id.drawer_menu_settings) {
        startActivity(new Intent(getApplicationContext(), UserSettingsActivity.class));
      }
    });
    drawerNavigator.setOnAccountItemSelectedListener((item, user) -> {
      if (item.getItemId() == R.id.drawer_menu_add_account) {
        OAuthActivity.start(this);
        finish();
      } else {
        drawerNavigator.closeDrawer();
        ((MainApplication) getApplication()).logout();
        timelineContainerSwitcher.clear();
        iffabItemSelectedListeners.clear();

        ((MainApplication) getApplication()).login(user.getId());
        setupHomeTimeline();
        drawerNavigator.changeCurrentUser();
        toolbarTweetInputToggle.changeCurrentUser();
        timelineContainerSwitcher.setOnContentChangedListener(getOnContentChangedListener());
        ((MainApplication) getApplication()).connectStream();
      }
    });
  }

  @NonNull
  private OnContentChangedListener getOnContentChangedListener() {
    return (type, title) -> {
      if (type == ContentType.MAIN) {
        tlFragment.startScroll();
      } else {
        tlFragment.stopScroll();
      }
      binding.mainToolbar.setTitle(title);
    };
  }

  @Override
  protected void onStop() {
    super.onStop();
    drawerNavigator.unsubscribeCurrentUser();
    drawerNavigator.setOnDefaultItemSelectedListener(null);
    drawerNavigator.setOnAccountItemSelectedListener(null);
    appSetting.close();
    timelineContainerSwitcher.setOnContentChangedListener(null);
    binding.ffab.setOnIffabItemSelectedListener(null);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (binding != null) {
      iffabItemSelectedListeners.clear();
      drawerNavigator.release();
      binding.navDrawerLayout.removeDrawerListener(actionBarDrawerToggle);
      binding.ffab.clear();
      configRequestWorker.shrink();
    }
  }

  @Override
  public void onBackPressed() {
    if (drawerNavigator.isDrawerOpen()) {
      if (!drawerNavigator.isMenuDefault()) {
        drawerNavigator.setMenuByGroupId(R.id.drawer_menu_default);
        return;
      }
      drawerNavigator.closeDrawer();
      return;
    }
    if (toolbarTweetInputToggle != null && toolbarTweetInputToggle.isOpened()) {
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
    if (itemId == R.id.action_writeTweet) {
      sendStatusSelected(TYPE_DEFAULT, -1);
    }
    return actionBarDrawerToggle.onOptionsItemSelected(item)
        || super.onOptionsItemSelected(item);
  }

  private void sendStatusSelected(@TweetType int type, long statusId) {
    if (binding.ffab.getVisibility() == View.VISIBLE) {
      binding.ffab.hide();
    }
    tlFragment.stopScroll();
    actionBarDrawerToggle.setDrawerIndicatorEnabled(false);
    actionBarDrawerToggle.setToolbarNavigationClickListener(
        toolbarTweetInputToggle.getOnCollapseClickListener());
    toolbarTweetInputToggle.setOnCloseListener(v -> cancelWritingSelected());
    toolbarTweetInputToggle.expandTweetInputView(type, statusId);
  }

  private void cancelWritingSelected() {
    tlFragment.startScroll();
    if (tlFragment.isItemSelected() && tlFragment.isVisible()) {
      binding.ffab.show();
    }
    toolbarTweetInputToggle.collapseTweetInputView();
    toolbarTweetInputToggle.setOnCloseListener(null);
    actionBarDrawerToggle.setToolbarNavigationClickListener(null);
    actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
    actionBarDrawerToggle.syncState();
  }

  private void setupTweetInputView() {
    final TweetInputFragment tweetInputFragment = TweetInputFragment.create();
    getSupportFragmentManager().beginTransaction()
        .add(R.id.main_appbar_container, tweetInputFragment)
        .commit();
    toolbarTweetInputToggle = new ToolbarTweetInputToggle(tweetInputFragment, binding.mainToolbar);
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
  public void showFab(int type) {
    if (type == TYPE_FAB) {
      binding.ffab.transToFAB(timelineContainerSwitcher.isItemSelected() ?
          View.VISIBLE : View.INVISIBLE);
    } else if (type == TYPE_TOOLBAR) {
      binding.ffab.transToToolbar();
    } else {
      binding.ffab.show();
    }
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
    if (toolbarTweetInputToggle != null && toolbarTweetInputToggle.isOpened()) {
      return;
    }
    tlFragment.stopScroll();
    UserInfoActivity.start(this, user, view);
  }

  @Override
  public View getRootView() {
    return binding.mainTimelineContainer;
  }

  @Override
  public void onItemClicked(ContentType type, long id, String query) {
    if (type == ContentType.LISTS) {
      timelineContainerSwitcher.showListTimeline(id, query);
    }
  }

  @Override
  public void onSpanClicked(View v, SpanItem item) {
    if (item.getType() == SpanItem.TYPE_HASHTAG) {
      timelineContainerSwitcher.showSearchResult(item.getQuery());
    }
  }
}