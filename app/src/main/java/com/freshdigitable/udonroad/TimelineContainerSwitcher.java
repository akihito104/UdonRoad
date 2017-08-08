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
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.freshdigitable.udonroad.ffab.IndicatableFFAB;

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
    final StatusDetailFragment statusDetail = StatusDetailFragment.getInstance(statusId);
    replaceTimelineContainer("detail_" + Long.toString(statusId), statusDetail);
    setDetailIsEnabled(false);
    ffab.transToToolbar();
  }

  void showConversation(long statusId) {
    ffab.transToFAB(View.INVISIBLE);
    final TimelineFragment<?> conversationFragment
        = TimelineFragment.getInstance(CONVERSATION, statusId);
    final String name = StoreType.CONVERSATION.nameWithSuffix(statusId, "");
    replaceTimelineContainer(name, conversationFragment);
    setDetailIsEnabled(true);
  }

  private void replaceTimelineContainer(String name, Fragment fragment) {
    final FragmentManager fm = getSupportFragmentManager();
    final Fragment current = fm.findFragmentById(containerId);
    Log.d("TLContainerSwitcher", "replaceTimelineContainer: " + current.getTag());
    final String tag;
    if (current == mainFragment) {
      tag = "main";
    } else if (current instanceof StatusDetailFragment) {
      tag = "detail";
    } else {
      tag = "conv";
    }
    if (current == mainFragment) {
      listener.onMainFragmentSwitched(false);
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
      listener.onMainFragmentSwitched(true);
      setDetailIsEnabled(true);
      ffab.transToFAB(((ItemSelectable) mainFragment).isItemSelected() ? View.VISIBLE : View.INVISIBLE);
    } else if ("detail".equals(appearFragmentName)) {
      setDetailIsEnabled(false);
      ffab.transToToolbar();
    } else if ("conv".equals(appearFragmentName)) {
      setDetailIsEnabled(true);
      ffab.transToFAB();
    }
    return true;
  }

  private void setDetailIsEnabled(boolean enabled) {
    final Menu menu = ffab.getMenu();
    menu.findItem(R.id.iffabMenu_main_detail).setEnabled(enabled);
  }

  private FragmentManager getSupportFragmentManager() {
    return mainFragment.getActivity().getSupportFragmentManager();
  }

  interface OnMainFragmentSwitchedListener {
    void onMainFragmentSwitched(boolean isAppeared);
  }

  private static final OnMainFragmentSwitchedListener EMPTY_LISTENER = a -> {};
  private OnMainFragmentSwitchedListener listener = EMPTY_LISTENER;

  void setOnMainFragmentSwitchedListener(OnMainFragmentSwitchedListener listener) {
    this.listener = listener != null ? listener : EMPTY_LISTENER;
  }

  static Animation makeSwitchingAnimation(Context context, int transit, boolean enter) {
    if (transit == FragmentTransaction.TRANSIT_FRAGMENT_OPEN) {
      if (!enter) {
        return AnimationUtils.makeOutAnimation(context, true);
      } else {
        return AnimationUtils.makeInAnimation(context, false);
      }
    }
    if (transit == FragmentTransaction.TRANSIT_FRAGMENT_CLOSE) {
      if (enter) {
        return AnimationUtils.makeInAnimation(context, false);
      } else {
        return AnimationUtils.makeOutAnimation(context, true);
      }
    }
    return null;
  }
}
