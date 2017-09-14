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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.freshdigitable.udonroad.TimelineContainerSwitcher.ContentType;
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
    implements TweetSendable, OnUserIconClickedListener, FabHandleable, SnackbarCapable,
    TimelineFragment.OnItemClickedListener, OnSpanClickListener {
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
    navHeaderBinding = DataBindingUtil.inflate(
        LayoutInflater.from(getApplicationContext()), R.layout.nav_header, null, false);
    binding.navDrawer.addHeaderView(navHeaderBinding.getRoot());

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    setupHomeTimeline();
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
    timelineContainerSwitcher = new TimelineContainerSwitcher(
        binding.mainTimelineContainer, tlFragment, binding.ffab);
  }

  private Disposable subscription;

  private void setupNavigationDrawer() {
    attachToolbar(binding.mainToolbar);
    appSettingRequestWorker.verifyCredentials();
    binding.navDrawer.setNavigationItemSelectedListener(item -> {
      int itemId = item.getItemId();
      if (item.getGroupId() == R.id.drawer_menu_default) {
        if (itemId == R.id.drawer_menu_home) {
          Log.d(TAG, "home is selected");
          timelineContainerSwitcher.showMain();
          binding.navDrawerLayout.closeDrawer(binding.navDrawer);
        } else if (itemId == R.id.drawer_menu_lists) {
          timelineContainerSwitcher.showOwnedLists(appSetting.getCurrentUserId());
          binding.navDrawerLayout.closeDrawer(binding.navDrawer);
        } else if (itemId == R.id.drawer_menu_license) {
          startActivity(new Intent(getApplicationContext(), LicenseActivity.class));
          binding.navDrawerLayout.closeDrawer(binding.navDrawer);
        }
      } else if (item.getGroupId() == R.id.drawer_menu_accounts) {
        if (item.getItemId() == R.id.drawer_menu_add_account) {
          OAuthActivity.start(this);
          finish();
        } else {
          final List<? extends User> users = appSetting.getAllAuthenticatedUsers();
          for (User user : users) {
            if (item.getTitle().toString().endsWith(user.getScreenName())) {
              ((MainApplication) getApplication()).logout();
              subscription.dispose();
              timelineContainerSwitcher.clear();
              iffabItemSelectedListeners.clear();

              ((MainApplication) getApplication()).login(user.getId());
              subscription = appSetting.observeCurrentUser()
                  .subscribe(this::setupNavigationDrawerHeader,
                      e -> Log.e(TAG, "setupNavigationDrawer: ", e));
              setupNavigationDrawerMenu();
              setupHomeTimeline();
              timelineContainerSwitcher.setOnContentChangedListener((type, title) -> {
                if (type == ContentType.MAIN) {
                  tlFragment.startScroll();
                } else {
                  tlFragment.stopScroll();
                }
                binding.mainToolbar.setTitle(title);
              });
              ((MainApplication) getApplication()).connectStream();
              binding.navDrawerLayout.closeDrawer(binding.navDrawer);
            }
          }
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
        if (!isNavDrawerMenuDefault()) {
          setNavDrawerMenuByGroupId(R.id.drawer_menu_default);
        }
      }
    };
    actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
    binding.navDrawerLayout.addDrawerListener(actionBarDrawerToggle);
    actionBarDrawerToggle.syncState();
  }

  private boolean isNavDrawerMenuDefault() {
    return binding.navDrawer.getMenu().findItem(R.id.drawer_menu_home).isVisible();
  }

  private void setNavDrawerMenuByGroupId(@IdRes int groupId) {
    final Menu menu = binding.navDrawer.getMenu();
    for (int i = 0; i < menu.size(); i++) {
      final MenuItem item = menu.getItem(i);
      item.setVisible(item.getGroupId() == groupId);
    }
  }

  private void setupNavigationDrawerHeader(User user) {
    navHeaderBinding.navHeaderAccount.setNames(new TwitterCombinedName(user));
    Picasso.with(getApplicationContext())
        .load(user.getProfileImageURLHttps())
        .resizeDimen(R.dimen.nav_drawer_header_icon, R.dimen.nav_drawer_header_icon)
        .into(navHeaderBinding.navHeaderIcon);
    navHeaderBinding.navHeaderIcon.setOnClickListener(v -> UserInfoActivity.start(this, user, v));
    navHeaderBinding.navHeaderAccount.setOnClickListener(v -> {
      setNavDrawerMenuByGroupId(R.id.drawer_menu_accounts);
      final Menu menu = binding.navDrawer.getMenu();
      final String userScreenName = "@" + appSetting.getCurrentUserScreenName();
      for (int i = 0; i < menu.size(); i++) {
        final MenuItem item = menu.getItem(i);
        if (item.getGroupId() == R.id.drawer_menu_accounts
            && userScreenName.equals(item.getTitle().toString())) {
          item.setVisible(false);
        }
      }
    });
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
    subscription = appSetting.observeCurrentUser()
        .subscribe(this::setupNavigationDrawerHeader,
            e -> Log.e(TAG, "setupNavigationDrawer: ", e));

    setupNavigationDrawerMenu();

    timelineContainerSwitcher.setOnContentChangedListener((type, title) -> {
      if (type == ContentType.MAIN) {
        tlFragment.startScroll();
      } else {
        tlFragment.stopScroll();
      }
      binding.mainToolbar.setTitle(title);
    });
  }

  private void setupNavigationDrawerMenu() {
    final List<? extends User> users = appSetting.getAllAuthenticatedUsers();
    final Menu menu = binding.navDrawer.getMenu();
    for (int i = 0; i < users.size(); i++) {
      final User user = users.get(i);
      if (user.getId() == appSetting.getCurrentUserId()
          || isAccountRegistered(menu, user.getScreenName())) {
        continue;
      }
      final int id = i != R.id.drawer_menu_add_account ? i
          : ((int) ((R.id.drawer_menu_add_account + 1L + i) % Integer.MAX_VALUE));
      final MenuItem item = menu.add(R.id.drawer_menu_accounts, id, i, "@" + user.getScreenName());
      item.setVisible(false);
    }
  }

  private static boolean isAccountRegistered(Menu menu, String screenName) {
    for (int i = 0; i < menu.size(); i++) {
      final MenuItem item = menu.getItem(i);
      if (item.getGroupId() == R.id.drawer_menu_accounts
          && item.getTitle().toString().endsWith(screenName)) {
        return true;
      }
    }
    return false;
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
      if (!isNavDrawerMenuDefault()) {
        setNavDrawerMenuByGroupId(R.id.drawer_menu_default);
        return;
      }
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