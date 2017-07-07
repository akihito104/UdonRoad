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

import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.View;

import com.freshdigitable.udonroad.TimelineFragment.StatusListFragment;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB;

import twitter4j.Status;

import static com.freshdigitable.udonroad.StoreType.CONVERSATION;

/**
 * Created by akihit on 2017/07/07.
 */

class TimelineContainerSwitcher {
  private final Fragment mainFragment;
  private final IndicatableFFAB ffab;
  private final @IdRes int containerId;

  TimelineContainerSwitcher(View container, Fragment mainFragment, IndicatableFFAB iffab) {
    if (!(mainFragment instanceof ItemSelectable)) {
      throw new IllegalArgumentException("mainFragment should implement ItemSelectable.");
    }
    this.mainFragment = mainFragment;
    this.ffab = iffab;
    this.containerId = container.getId();
  }

  void showStatusDetail(long statusId) {
    StatusDetailFragment statusDetail = StatusDetailFragment.getInstance(statusId);
    replaceTimelineContainer("detail_" + Long.toString(statusId), statusDetail);
    switchFFABMenuTo(R.id.iffabMenu_main_conv);
    ffab.transToToolbar();
  }

  void showConversation(long statusId) {
    ffab.transToFAB(View.INVISIBLE);
    final TimelineFragment<Status> conversationFragment
        = StatusListFragment.getInstance(CONVERSATION, statusId);
    final String name = StoreType.CONVERSATION.prefix() + Long.toString(statusId);
    replaceTimelineContainer(name, conversationFragment);
    switchFFABMenuTo(R.id.iffabMenu_main_detail);
  }

  private void replaceTimelineContainer(String name, Fragment fragment) {
    final FragmentManager fm = getSupportFragmentManager();
    final Fragment current = fm.findFragmentById(containerId);
    final String tag;
    if (current == mainFragment) {
      tag = "main";
    } else if (current instanceof StatusDetailFragment) {
      tag = "detail";
    } else {
      tag = "conv";
    }
    fm.beginTransaction()
        .replace(containerId, fragment, name)
        .addToBackStack(tag)
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        .commit();
  }

  boolean clearSelectedCursorIfNeeded() {
    final Fragment fragment = getSupportFragmentManager().findFragmentById(containerId);
    return fragment instanceof ItemSelectable
        && clearSelectedCursorIfNeeded((ItemSelectable) fragment);
  }

  private boolean clearSelectedCursorIfNeeded(ItemSelectable fragment) {
    if (fragment.isItemSelected()) {
      fragment.clearSelectedItem();
      return true;
    }
    return false;
  }

  boolean popBackStackTimelineContainer() {
    final FragmentManager fm = getSupportFragmentManager();
    final int backStackEntryCount = fm.getBackStackEntryCount();
    if (backStackEntryCount <= 0) {
      return false;
    }

    fm.popBackStack();

    final FragmentManager.BackStackEntry backStack = fm.getBackStackEntryAt(backStackEntryCount - 1);
    final String appearFragmentName = backStack.getName();
    if ("main".equals(appearFragmentName)) {
      switchFFABMenuTo(R.id.iffabMenu_main_detail);
      ffab.transToFAB(((ItemSelectable) mainFragment).isItemSelected() ? View.VISIBLE : View.INVISIBLE);
    } else if ("detail".equals(appearFragmentName)) {
      switchFFABMenuTo(R.id.iffabMenu_main_conv);
      ffab.transToToolbar();
    } else if ("conv".equals(appearFragmentName)) {
      switchFFABMenuTo(R.id.iffabMenu_main_detail);
      ffab.transToFAB();
    }
    return true;
  }

  private static final int[] FFAB_MENU_LEFT_SETS
      = {R.id.iffabMenu_main_conv, R.id.iffabMenu_main_detail};

  private void switchFFABMenuTo(@IdRes int targetItem) {
    final Menu menu = ffab.getMenu();
    for (@IdRes int menuItemId : FFAB_MENU_LEFT_SETS) {
      menu.findItem(menuItemId).setEnabled(menuItemId == targetItem);
    }
  }

  private FragmentManager getSupportFragmentManager() {
    return mainFragment.getActivity().getSupportFragmentManager();
  }
}