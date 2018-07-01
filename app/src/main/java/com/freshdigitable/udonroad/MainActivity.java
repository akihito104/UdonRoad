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

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.freshdigitable.udonroad.TimelineContainerSwitcher.ContentType;
import com.freshdigitable.udonroad.TimelineContainerSwitcher.OnContentChangedListener;
import com.freshdigitable.udonroad.databinding.ActivityMainBinding;
import com.freshdigitable.udonroad.databinding.NavHeaderBinding;
import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB;
import com.freshdigitable.udonroad.input.TweetInputFragment;
import com.freshdigitable.udonroad.input.TweetInputFragment.TweetType;
import com.freshdigitable.udonroad.listitem.OnUserIconClickedListener;
import com.freshdigitable.udonroad.oauth.OAuthActivity;
import com.freshdigitable.udonroad.repository.ImageRepository;
import com.freshdigitable.udonroad.subscriber.ConfigRequestWorker;
import com.freshdigitable.udonroad.timeline.TimelineFragment;
import com.freshdigitable.udonroad.user.UserInfoActivity;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import timber.log.Timber;
import twitter4j.User;

import static com.freshdigitable.udonroad.StoreType.HOME;
import static com.freshdigitable.udonroad.input.TweetInputFragment.TYPE_QUOTE;
import static com.freshdigitable.udonroad.input.TweetInputFragment.TYPE_REPLY;

/**
 * MainActivity shows home timeline for authorized user.
 *
 * Created by akihit
 */
public class MainActivity extends AppCompatActivity
    implements OnUserIconClickedListener, SnackbarCapable,
    TimelineFragment.OnItemClickedListener, OnSpanClickListener, HasSupportFragmentInjector {
  private static final String TAG = MainActivity.class.getSimpleName();
  private ActivityMainBinding binding;
  private ActionBarDrawerToggle actionBarDrawerToggle;
  private TimelineFragment tlFragment;

  @Inject
  ConfigRequestWorker configRequestWorker;
  @Inject
  AppSettingStore appSetting;
  @Inject
  ImageRepository imageRepository;
  @Inject
  ViewModelProvider.Factory factory;
  private TimelineContainerSwitcher timelineContainerSwitcher;
  private DrawerNavigator drawerNavigator;
  private ToolbarTweetInputToggle toolbarTweetInputToggle;
  private FabViewModel fabViewModel;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    AndroidInjection.inject(this);
    super.onCreate(savedInstanceState);
    if (savedInstanceState == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      supportRequestWindowFeature(Window.FEATURE_CONTENT_TRANSITIONS);
    }

    final View v = findViewById(R.id.nav_drawer_layout);
    if (v == null) {
      binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
    } else {
      binding = DataBindingUtil.findBinding(v);
    }

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    setSupportActionBar(binding.mainToolbar);
    final ActionBar supportActionBar = getSupportActionBar();
    if (supportActionBar != null) {
      supportActionBar.setDisplayHomeAsUpEnabled(true);
      supportActionBar.setHomeButtonEnabled(true);
    }

    fabViewModel = ViewModelProviders.of(this).get(FabViewModel.class);
    fabViewModel.getFabState().observe(this, type -> {
      if (type == FabViewModel.Type.FAB) {
        binding.ffab.transToFAB(timelineContainerSwitcher.isItemSelected() ?
            View.VISIBLE : View.INVISIBLE);
      } else if (type == FabViewModel.Type.TOOLBAR) {
        binding.ffab.transToToolbar();
      } else if (type == FabViewModel.Type.HIDE) {
        binding.ffab.hide();
      } else {
        binding.ffab.show();
      }
    });
    fabViewModel.getMenuState().observe(this, FabViewModel.createMenuStateObserver(binding.ffab));

    setupHomeTimeline();
    setupNavigationDrawer();
    setupTweetInputView();
  }

  private void setupHomeTimeline() {
    final TimelineFragment timelineFragment = getTimelineFragment();
    if (timelineFragment == null) {
      tlFragment = TimelineFragment.getInstance(HOME);
      configRequestWorker.setup().subscribe(
          () -> Timber.tag(TAG).d("setupHomeTimeline: config.setup"),
          throwable -> Timber.tag(TAG).e(throwable, "config.setup: "));
      getSupportFragmentManager().beginTransaction()
          .replace(R.id.main_timeline_container, tlFragment, TimelineContainerSwitcher.MAIN_FRAGMENT_TAG)
          .commit();
    } else {
      tlFragment = timelineFragment;
    }
    timelineContainerSwitcher = new TimelineContainerSwitcher(
        binding.mainTimelineContainer, tlFragment, binding.ffab, factory);
  }

  @Nullable
  private TimelineFragment getTimelineFragment() {
    return ((TimelineFragment) getSupportFragmentManager().findFragmentByTag(TimelineContainerSwitcher.MAIN_FRAGMENT_TAG));
  }

  private void setupNavigationDrawer() {
    actionBarDrawerToggle = new ActionBarDrawerToggle(this,
        binding.navDrawerLayout, R.string.navDesc_openDrawer, R.string.navDesc_closeDrawer) {
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

    final NavHeaderBinding navHeaderBinding = DataBindingUtil.inflate(
        LayoutInflater.from(this), R.layout.nav_header, null, false);
    drawerNavigator = new DrawerNavigator(binding.navDrawerLayout, binding.navDrawer, navHeaderBinding, appSetting, imageRepository);
  }

  private void setupTweetInputView() {
    final TweetInputFragment tweetInputFragment = getTweetInputFragment();
    toolbarTweetInputToggle = new ToolbarTweetInputToggle(binding.mainToolbar, tweetInputFragment);
    if (tweetInputFragment == null) {
      getSupportFragmentManager().beginTransaction()
          .add(R.id.main_appbar_container, toolbarTweetInputToggle.getFragment())
          .commit();
    }
  }

  @Nullable
  private TweetInputFragment getTweetInputFragment() {
    final Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_appbar_container);
    return fragment instanceof TweetInputFragment ? ((TweetInputFragment) fragment) : null;
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
    return (type, title) -> binding.mainToolbar.setTitle(title);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    toolbarTweetInputToggle.onSaveInstanceState(outState);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    timelineContainerSwitcher.syncState();
    toolbarTweetInputToggle.onRestoreInstanceState(savedInstanceState);
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
      toolbarTweetInputToggle.cancelInput();
      onTweetInputClosed();
      return;
    }
    if (timelineContainerSwitcher.clearSelectedCursorIfNeeded()) {
      return;
    }
    if (binding.ffab.getFabMode() == IndicatableFFAB.MODE_SHEET) {
      binding.ffab.transToFAB();
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
      onTweetInputOpened();
      toolbarTweetInputToggle.onOptionMenuSelected(item);
    } else if (itemId == R.id.action_resumeTweet) {
      onTweetInputOpened();
      toolbarTweetInputToggle.onOptionMenuSelected(item);
    } else if (itemId == R.id.action_sendTweet) {
      toolbarTweetInputToggle.onOptionMenuSelected(item);
      onTweetInputClosed();
      return super.onOptionsItemSelected(item);
    } else if (itemId == android.R.id.home && toolbarTweetInputToggle.onOptionMenuSelected(item)) {
      onTweetInputClosed();
      return super.onOptionsItemSelected(item);
    }
    return actionBarDrawerToggle.onOptionsItemSelected(item)
        || super.onOptionsItemSelected(item);
  }

  private void sendStatusSelected(@TweetType int type, long statusId) {
    onTweetInputOpened();
    toolbarTweetInputToggle.expandTweetInputView(type, statusId);
  }

  public void onTweetInputOpened() {
    if (binding.ffab.getVisibility() == View.VISIBLE) {
      binding.ffab.hide();
    }
    tlFragment.stopScroll();
    actionBarDrawerToggle.setDrawerIndicatorEnabled(false);
  }

  public void onTweetInputClosed() {
    tlFragment.startScroll();
    if (tlFragment.isItemSelected() && tlFragment.isVisible()) {
      binding.ffab.show();
    }
    actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
    actionBarDrawerToggle.syncState();
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
      fabViewModel.onMenuItemSelected(item);
    });
  }

  @Override
  public void onUserIconClicked(View view, User user) {
    if (toolbarTweetInputToggle != null && toolbarTweetInputToggle.isOpened()) {
      return;
    }
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

  @Inject
  DispatchingAndroidInjector<Fragment> androidInjector;

  @Override
  public AndroidInjector<Fragment> supportFragmentInjector() {
    return androidInjector;
  }
}