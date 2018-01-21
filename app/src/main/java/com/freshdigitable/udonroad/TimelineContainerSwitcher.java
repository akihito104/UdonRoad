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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.freshdigitable.udonroad.ffab.IndicatableFFAB;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by akihit on 2017/07/07.
 */

class TimelineContainerSwitcher {
  static final String MAIN_FRAGMENT_TAG = ContentType.MAIN.createTag(-1, null);
  private final Fragment mainFragment;
  private final IndicatableFFAB ffab;
  private final @IdRes int containerId;

  TimelineContainerSwitcher(@NonNull View container, @NonNull Fragment mainFragment, @NonNull IndicatableFFAB iffab) {
    if (!(mainFragment instanceof ItemSelectable)) {
      throw new IllegalArgumentException("mainFragment should implement ItemSelectable.");
    }
    this.mainFragment = mainFragment;
    this.ffab = iffab;
    this.containerId = container.getId();
  }

  void showMain() {
    final Fragment currentFragment = getCurrentFragment();
    if (currentFragment == mainFragment) {
      return;
    }
    final List<String> dropList = getDropList();
    final FragmentManager fm = getSupportFragmentManager();
    final int backStackEntryCount = fm.getBackStackEntryCount();
    for (int i = backStackEntryCount - 1; i > 0; i--) {
      final FragmentManager.BackStackEntry bs = fm.getBackStackEntryAt(i);
      dropList.add(bs.getName());
    }
    registerDropCacheListener(dropList);
    fm.popBackStack(MAIN_FRAGMENT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    ContentType.MAIN.onShow(this, "", false);
  }

  void showStatusDetail(long statusId) {
    final StatusDetailFragment statusDetail = StatusDetailFragment.getInstance(statusId);
    replaceTimelineContainer(ContentType.DETAIL, statusId, null, statusDetail);
  }

  void showConversation(long statusId) {
    final TimelineFragment conversationFragment
        = TimelineFragment.getInstance(StoreType.CONVERSATION, statusId);
    replaceTimelineContainer(ContentType.CONV, statusId, null, conversationFragment);
  }

  void showSearchResult(String query) {
    final TimelineFragment fragment = TimelineFragment.getInstance(StoreType.SEARCH, query);
    replaceTimelineContainer(ContentType.SEARCH, -1, query, fragment);
  }

  void showOwnedLists() {
    final TimelineFragment fragment = TimelineFragment.getInstance(StoreType.OWNED_LIST, -1);
    replaceTimelineContainer(ContentType.LISTS, -1, null, fragment);
  }

  void showListTimeline(long listId, String query) {
    final TimelineFragment fragment = TimelineFragment.getInstance(StoreType.LIST_TL, listId);
    replaceTimelineContainer(ContentType.LIST_TL, listId, query, fragment);
  }

  private void replaceTimelineContainer(ContentType type, long id, String query, Fragment fragment) {
    final Fragment current = getCurrentFragment();
    if (current == null) {
      return;
    }
    final String tag = current.getTag();
    final String name = type.createTag(id, query);
    getSupportFragmentManager().beginTransaction()
        .replace(containerId, fragment, name)
        .addToBackStack(tag)
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        .commit();
    type.onShow(this, name, true);
  }

  boolean popBackStackTimelineContainer() {
    final FragmentManager fm = getSupportFragmentManager();
    if (fm == null) {
      return false;
    }
    registerDropCacheListener();
    final int backStackEntryCount = fm.getBackStackEntryCount();
    if (backStackEntryCount <= 0) {
      return false;
    }

    fm.popBackStack();
    final FragmentManager.BackStackEntry backStack = fm.getBackStackEntryAt(backStackEntryCount - 1);
    final String appearFragmentName = backStack.getName();
    ContentType.findByTag(appearFragmentName).onShow(this, appearFragmentName, false);
    return true;
  }

  private void registerDropCacheListener() {
    final List<String> dropList = getDropList();
    registerDropCacheListener(dropList);
  }

  private void registerDropCacheListener(@NonNull List<String> dropList) {
    if (dropList.isEmpty()) {
      return;
    }
    final Fragment currentFragment = getCurrentFragment();
    if (currentFragment == null) {
      return;
    }
    // FragmentManager of current Fragment is needed
    final FragmentManager fm = currentFragment.getFragmentManager();
    fm.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
      @Override
      public void onFragmentDetached(FragmentManager fm, Fragment f) {
        if (f instanceof TimelineFragment) {
          final TimelineFragment timelineFragment = (TimelineFragment) f;
          if (dropList.remove(timelineFragment.getStoreName())) {
            timelineFragment.dropCache();
          }
        }
        if (dropList.isEmpty()) {
          fm.unregisterFragmentLifecycleCallbacks(this);
        }
      }
    }, true);
  }

  private List<String> getDropList() {
    final Fragment currentFragment = getCurrentFragment();
    final ArrayList<String> dropList = new ArrayList<>();
    if (currentFragment instanceof TimelineFragment) {
      dropList.add(((TimelineFragment) currentFragment).getStoreName());
    } else if (currentFragment instanceof UserInfoPagerFragment) {
      final List<Fragment> fragments = currentFragment.getChildFragmentManager().getFragments();
      for (Fragment f : fragments) {
        if (f instanceof TimelineFragment) {
          dropList.add(((TimelineFragment) f).getStoreName());
        }
      }
    }
    return dropList;
  }

  void syncState() {
    final Fragment currentFragment = getCurrentFragment();
    if (currentFragment == null) {
      return;
    }
    final String tag = currentFragment.getTag();
    ContentType.findByTag(tag).onShow(this, tag, false);
  }

  private void setDetailIsEnabled(boolean enabled) {
    final Menu menu = ffab.getMenu();
    menu.findItem(R.id.iffabMenu_main_detail).setEnabled(enabled);
  }

  boolean clearSelectedCursorIfNeeded() {
    final Fragment fragment = getCurrentFragment();
    return fragment instanceof ItemSelectable
        && clearSelectedCursorIfNeeded((ItemSelectable) fragment);
  }

  private boolean clearSelectedCursorIfNeeded(@Nullable ItemSelectable fragment) {
    if (fragment != null && fragment.isItemSelected()) {
      fragment.clearSelectedItem();
      return true;
    }
    return false;
  }

  long getSelectedTweetId() {
    final Fragment current = getCurrentFragment();
    if (current instanceof ItemSelectable) {
      return ((ItemSelectable) current).getSelectedItemId();
    } else if (current instanceof StatusDetailFragment) {
      return ((StatusDetailFragment) current).getStatusId();
    }
    throw new IllegalStateException("unknown fragment is shown now...");
  }

  private FragmentManager getSupportFragmentManager() {
    final FragmentActivity activity = mainFragment.getActivity();
    return activity != null ? activity.getSupportFragmentManager()
        : mainFragment.getFragmentManager();
  }

  private Context getContext() {
    final Fragment fragment = getCurrentFragment();
    return fragment != null ? fragment.getContext() : null;
  }

  @Nullable
  private Fragment getCurrentFragment() {
    final FragmentManager fm = getSupportFragmentManager();
    return fm != null ? fm.findFragmentById(containerId) : null;
  }

  boolean isItemSelected() {
    final Fragment current = getCurrentFragment();
    if (current instanceof ItemSelectable) {
      return ((ItemSelectable) current).isItemSelected();
    } else if (current instanceof StatusDetailFragment){
      return true;
    }
    return false;
  }

  void clear() {
    setOnContentChangedListener(null);
    showMain();
    getSupportFragmentManager().beginTransaction()
        .remove(mainFragment)
        .commitNow();
  }

  interface OnContentChangedListener {
    void onContentChanged(ContentType type, String title);
  }

  private static final OnContentChangedListener EMPTY_LISTENER = (type, title) -> {};
  private OnContentChangedListener listener = EMPTY_LISTENER;

  void setOnContentChangedListener(OnContentChangedListener listener) {
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

  enum ContentType {
    MAIN(R.string.title_home, "main") {
      @Override
      String createTag(long id, String query) {
        return tagPrefix;
      }

      @Override
      void onShow(TimelineContainerSwitcher switcher, String tag, boolean isNew) {
        switcher.listener.onContentChanged(this, getTitle(switcher.getContext()));
        switcher.setDetailIsEnabled(true);
      }
    },
    CONV(R.string.title_conv, StoreType.CONVERSATION.prefix()) {
      @Override
      String createTag(long id, String query) {
        return StoreType.CONVERSATION.nameWithSuffix(id, "");
      }

      @Override
      void onShow(TimelineContainerSwitcher switcher, String tag, boolean isNew) {
        switcher.listener.onContentChanged(this, getTitle(switcher.getContext()));
        switcher.setDetailIsEnabled(true);
      }
    },
    DETAIL(R.string.title_detail, "detail") {
      @Override
      String createTag(long id, String query) {
        return tagPrefix + "_" + Long.toString(id);
      }

      @Override
      void onShow(TimelineContainerSwitcher switcher, String tag, boolean isNew) {
        switcher.listener.onContentChanged(this, getTitle(switcher.getContext()));
        switcher.setDetailIsEnabled(false);
      }
    },
    SEARCH(R.string.title_search, StoreType.SEARCH.prefix()) {
      @Override
      String createTag(long id, String query) {
        return StoreType.SEARCH.nameWithSuffix(-1, query);
      }

      @Override
      void onShow(TimelineContainerSwitcher switcher, String tag, boolean isNew) {
        switcher.listener.onContentChanged(this, tag.substring(tagPrefix.length()));
        switcher.setDetailIsEnabled(true);
      }
    }, LISTS(R.string.title_owned_list, StoreType.OWNED_LIST.storeName) {
      @Override
      String createTag(long id, String query) {
        return StoreType.OWNED_LIST.storeName;
      }

      @Override
      void onShow(TimelineContainerSwitcher switcher, String tag, boolean isNew) {
        switcher.listener.onContentChanged(this, getTitle(switcher.getContext()));
        switcher.setDetailIsEnabled(false);
      }
    }, LIST_TL(0, StoreType.LIST_TL.prefix()) {
      @Override
      String createTag(long id, String query) {
        return tagPrefix + "_" + query;
      }

      @Override
      void onShow(TimelineContainerSwitcher switcher, String tag, boolean isNew) {
        switcher.listener.onContentChanged(this, tag.substring(tagPrefix.length() + 1));
        switcher.setDetailIsEnabled(true);
      }
    },;

    final int titleRes;
    final String tagPrefix;

    ContentType(int titleRes, String tagPrefix) {
      this.titleRes = titleRes;
      this.tagPrefix = tagPrefix;
    }

    String getTitle(Context context) {
      return context.getString(titleRes);
    }

    abstract String createTag(long id, String query);

    abstract void onShow(TimelineContainerSwitcher switcher, String tag, boolean isNew);

    static ContentType findByTag(String tag) {
      for (ContentType type : ContentType.values()) {
        if (tag.startsWith(type.tagPrefix)) {
          return type;
        }
      }
      throw new IllegalStateException("unknown fragment type...: " + tag);
    }
  }
}
