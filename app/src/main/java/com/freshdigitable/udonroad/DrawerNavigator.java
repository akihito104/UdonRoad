/*
 * Copyright (c) 2017. Matsuda, Akihit (akihito104)
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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v4.widget.DrawerLayout.SimpleDrawerListener;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.content.res.AppCompatResources;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.freshdigitable.udonroad.databinding.NavHeaderBinding;
import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.listitem.TwitterCombinedName;
import com.freshdigitable.udonroad.repository.ImageRepository;

import java.util.List;

import io.reactivex.disposables.Disposable;
import twitter4j.User;

/**
 * Created by akihit on 2017/09/15.
 */

class DrawerNavigator {
  private static String TAG = DrawerNavigator.class.getSimpleName();
  private final DrawerLayout drawerLayout;
  private final NavigationView navigationView;
  private final NavHeaderBinding navHeaderBinding;
  private final Drawable upArrow, downArrow;
  private final AppSettingStore appSettings;
  private final ImageRepository imageRepository;
  private final DrawerListener drawerListener = new SimpleDrawerListener() {
    @Override
    public void onDrawerClosed(View drawerView) {
      if (!isMenuDefault()) {
        setMenuByGroupId(R.id.drawer_menu_default);
      }
    }
  };
  private boolean navMenuPerforming = false;

  DrawerNavigator(@NonNull DrawerLayout drawerLayout, @NonNull NavigationView navigationView,
                  @NonNull NavHeaderBinding navHeaderBinding, @NonNull AppSettingStore appSetting,
                  @NonNull ImageRepository imageRepository) {
    this.drawerLayout = drawerLayout;
    this.navigationView = navigationView;
    this.navHeaderBinding = navHeaderBinding;
    this.appSettings = appSetting;
    this.imageRepository = imageRepository;

    final Context context = drawerLayout.getContext();
    downArrow = AppCompatResources.getDrawable(context, R.drawable.ic_arrow_drop_down);
    upArrow = AppCompatResources.getDrawable(context, R.drawable.ic_arrow_drop_up);

    TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(this.navHeaderBinding.navHeaderAccount,
        null, null, downArrow, null);
    this.navigationView.addHeaderView(this.navHeaderBinding.getRoot());
    this.navigationView.setNavigationItemSelectedListener(item -> {
      if (navMenuPerforming) {
        return false;
      }
      navMenuPerforming = true;
      try {
        if (item.getGroupId() == R.id.drawer_menu_default) {
          defaultItemSelectedListener.onDefaultItemSelected(item);
        } else if (item.getGroupId() == R.id.drawer_menu_accounts) {
          final User user = findUserByAccount(item.getTitle().toString());
          accountItemSelectedListener.onAccountItemSelected(item, user);
        }
      } finally {
        navMenuPerforming = false;
      }
      return false;
    });
    this.drawerLayout.addDrawerListener(drawerListener);
  }

  @Nullable
  private User findUserByAccount(String account) {
    final List<? extends User> users = appSettings.getAllAuthenticatedUsers();
    for (User user : users) {
      if (account.equals("@" + user.getScreenName())) {
        return user;
      }
    }
    return null;
  }

  boolean isMenuDefault() {
    return navigationView.getMenu().findItem(R.id.drawer_menu_home).isVisible();
  }

  void setMenuByGroupId(@IdRes int groupId) {
    TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(navHeaderBinding.navHeaderAccount,
        null, null, groupId == R.id.drawer_menu_default ? downArrow : upArrow, null);
    final Menu menu = navigationView.getMenu();
    for (int i = 0; i < menu.size(); i++) {
      final MenuItem item = menu.getItem(i);
      item.setVisible(item.getGroupId() == groupId);
    }
  }

  private Disposable subscription;
  private Disposable userIconSubs;

  void changeCurrentUser() {
    unsubscribeCurrentUser();
    subscription = appSettings.observeCurrentUser()
        .subscribe(this::setupHeader,
            e -> Log.e(TAG, "setupNavigationDrawer: ", e));
    setAccountList();
  }

  void unsubscribeCurrentUser() {
    if (subscription != null && !subscription.isDisposed()) {
      subscription.dispose();
    }
  }

  private void setupHeader(User currentUser) {
    disposeUserIconSubscription();
    userIconSubs = imageRepository.queryUserIcon(currentUser, R.dimen.nav_drawer_header_icon, "currentUser")
        .subscribe(navHeaderBinding.navHeaderIcon::setImageDrawable);

    final long userId = currentUser.getId();
    navHeaderBinding.navHeaderIcon.setOnClickListener(v ->
      UserInfoActivity.start(v.getContext(), userId)
    );

    navHeaderBinding.navHeaderAccount.setNames(new TwitterCombinedName(currentUser));

    final String userScreenName = "@" + currentUser.getScreenName();
    navHeaderBinding.navHeaderAccount.setOnClickListener(v -> {
      if (isMenuDefault()) {
        setMenuByGroupId(R.id.drawer_menu_accounts);
        final Menu menu = navigationView.getMenu();
        for (int i = 0; i < menu.size(); i++) {
          final MenuItem item = menu.getItem(i);
          if (item.getGroupId() == R.id.drawer_menu_accounts
              && userScreenName.equals(item.getTitle().toString())) {
            item.setVisible(false);
          }
        }
      } else {
        setMenuByGroupId(R.id.drawer_menu_default);
      }
    });
  }

  private void disposeUserIconSubscription() {
    if (userIconSubs != null && !userIconSubs.isDisposed()) {
      userIconSubs.dispose();
    }
  }

  private void setAccountList() {
    final long currentUserId = appSettings.getCurrentUserId();
    final List<? extends User> users = appSettings.getAllAuthenticatedUsers();
    final Menu menu = navigationView.getMenu();
    for (int i = 0; i < users.size(); i++) {
      final User user = users.get(i);
      if (user.getId() == currentUserId || isAccountRegistered(menu, user.getScreenName())) {
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

  boolean isDrawerOpen() {
    return drawerLayout.isDrawerOpen(navigationView);
  }

  void closeDrawer() {
    drawerLayout.closeDrawer(navigationView);
  }

  void release() {
    final RoundedCornerImageView navHeaderIcon = navHeaderBinding.navHeaderIcon;
    disposeUserIconSubscription();
    navHeaderIcon.setOnClickListener(null);
    navHeaderBinding.navHeaderAccount.setOnClickListener(null);
    setOnDefaultItemSelectedListener(null);
    setOnAccountItemSelectedListener(null);
    navigationView.setNavigationItemSelectedListener(null);
    drawerLayout.removeDrawerListener(drawerListener);
  }

  interface OnDefaultItemSelectedListener {
    void onDefaultItemSelected(MenuItem item);
  }

  private OnDefaultItemSelectedListener defaultItemSelectedListener = item -> {};

  void setOnDefaultItemSelectedListener(OnDefaultItemSelectedListener listener) {
    this.defaultItemSelectedListener = listener != null ? listener : item -> {};
  }

  interface OnAccountItemSelectedListener {
    void onAccountItemSelected(MenuItem item, User user);
  }

  private OnAccountItemSelectedListener accountItemSelectedListener = (item, user) -> {};

  void setOnAccountItemSelectedListener(OnAccountItemSelectedListener listener) {
    this.accountItemSelectedListener = listener != null ? listener : (item, user) -> {};
  }
}
