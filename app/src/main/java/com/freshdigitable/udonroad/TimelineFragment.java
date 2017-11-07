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

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.AdapterDataObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import com.freshdigitable.udonroad.TimelineAdapter.OnSelectedItemChangeListener;
import com.freshdigitable.udonroad.TimelineContainerSwitcher.ContentType;
import com.freshdigitable.udonroad.databinding.FragmentTimelineBinding;
import com.freshdigitable.udonroad.datastore.SortedCache;
import com.freshdigitable.udonroad.datastore.UpdateEvent;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;
import com.freshdigitable.udonroad.listitem.OnUserIconClickedListener;
import com.freshdigitable.udonroad.listitem.StatusView;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.subscriber.ListFetchStrategy;
import com.freshdigitable.udonroad.subscriber.ListRequestWorker;

import javax.inject.Inject;

import io.reactivex.disposables.Disposable;
import twitter4j.Status;
import twitter4j.User;
import twitter4j.UserList;

/**
 * TimelineFragment provides RecyclerView to show timeline.
 *
 * Created by Akihit.
 */
public abstract class TimelineFragment<T> extends Fragment implements ItemSelectable {
  @SuppressWarnings("unused")
  private static final String TAG = TimelineFragment.class.getSimpleName();
  private static final String SS_SCROLLED_BY_USER = "ss_scrolledByUser";
  private static final String SS_STOP_SCROLL = "ss_stopScroll";
  private static final String SS_DONE_FIRST_FETCH = "ss_doneFirstFetch";
  private static final String SS_TOP_ITEM_ID = "ss_topItemId";
  private static final String SS_TOP_ITEM_TOP = "ss_topItemTop";
  public static final String SS_ADAPTER = "ss_adapter";
  private FragmentTimelineBinding binding;
  TimelineAdapter<T> tlAdapter;
  private LinearLayoutManager tlLayoutManager;
  private Disposable updateEventSubscription;
  @Inject
  ListRequestWorker<T> requestWorker;
  @Inject
  SortedCache<T> sortedCache;
  private TimelineDecoration timelineDecoration;
  private TimelineAnimator timelineAnimator;
  private ListFetchStrategy fetcher;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    sortedCache.open(getStoreName());
    updateEventSubscription = sortedCache.observeUpdateEvent()
        .retry()
        .doOnSubscribe(subs -> Log.d(TAG, "onAttach: updateEvent is subscribed"))
        .subscribe(event -> {
              if (event.type == UpdateEvent.EventType.INSERT) {
                tlAdapter.notifyItemRangeInserted(event.index, event.length);
              } else if (event.type == UpdateEvent.EventType.CHANGE) {
                tlAdapter.notifyItemRangeChanged(event.index, event.length);
              } else if (event.type == UpdateEvent.EventType.DELETE) {
                tlAdapter.notifyItemRangeRemoved(event.index, event.length);
              }
            },
            e -> Log.e(TAG, "updateEvent: ", e));
    fetcher = requestWorker.getFetchStrategy(getStoreType(), getEntityId(), getQuery());
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.timeline, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    final int itemId = item.getItemId();
    if (itemId == R.id.action_heading) {
      scrollToTop();
    }
    return false;
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView: " + getStoreName());
    if (savedInstanceState != null) {
      isScrolledByUser = savedInstanceState.getBoolean(SS_SCROLLED_BY_USER);
      stopScroll = savedInstanceState.getBoolean(SS_STOP_SCROLL);
      doneFirstFetch = savedInstanceState.getBoolean(SS_DONE_FIRST_FETCH);
      tlAdapter.onRestoreInstanceState(savedInstanceState.getParcelable(SS_ADAPTER));
    }
    if (binding == null) {
      binding = DataBindingUtil.inflate(inflater, R.layout.fragment_timeline, container, false);
      binding.timeline.setHasFixedSize(true);
      binding.timeline.setAdapter(tlAdapter);
      binding.timelineSwipeLayout.setColorSchemeResources(R.color.accent, R.color.twitter_primary,
          R.color.twitter_action_retweeted, R.color.twitter_action_faved);
    }
    return binding.getRoot();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    Log.d(TAG, "onSaveInstanceState: " + getStoreName());
    super.onSaveInstanceState(outState);
    outState.putBoolean(SS_SCROLLED_BY_USER, isScrolledByUser);
    outState.putBoolean(SS_STOP_SCROLL, stopScroll);
    outState.putBoolean(SS_DONE_FIRST_FETCH, doneFirstFetch);
    outState.putLong(SS_TOP_ITEM_ID, topItemId);
    outState.putInt(SS_TOP_ITEM_TOP, firstVisibleItemTopOnStop);
    outState.putParcelable(SS_ADAPTER, tlAdapter.onSaveInstanceState());
  }

  private boolean isScrolledByUser = false;

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    Log.d(TAG, "onActivityCreated: " + getStoreName());
    super.onActivityCreated(savedInstanceState);
    if (timelineDecoration == null) {
      timelineDecoration = new TimelineDecoration(getContext());
      binding.timeline.addItemDecoration(timelineDecoration);
    }
    if (tlLayoutManager == null) {
      tlLayoutManager = new LinearLayoutManager(getContext());
      tlLayoutManager.setAutoMeasureEnabled(true);
      binding.timeline.setLayoutManager(tlLayoutManager);
    }
    if (timelineAnimator == null) {
      timelineAnimator = new TimelineAnimator();
      binding.timeline.setItemAnimator(timelineAnimator);
    }
    if (!doneFirstFetch && getUserVisibleHint()) {
      fetcher.fetch();
      doneFirstFetch = true;
    }
  }

  private boolean doneFirstFetch = false;

  @Override
  public void setUserVisibleHint(boolean isVisibleToUser) {
    super.setUserVisibleHint(isVisibleToUser);
    if (isVisibleToUser && !doneFirstFetch && fetcher != null) {
      fetcher.fetch();
      doneFirstFetch = true;
    }
  }

  private final AdapterDataObserver itemInsertedObserver
      = new AdapterDataObserver() {
    @Override
    public void onItemRangeInserted(int positionStart, int itemCount) {
      super.onItemRangeInserted(positionStart, itemCount);
      if (positionStart != 0) {
        return;
      }
      if (canScroll()) {
//        Log.d(TAG, "onItemRangeInserted: ");
        scrollTo(0);
      } else {
        addedUntilStopped = true;
      }
    }
  };

  private final RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
      if (newState == RecyclerView.SCROLL_STATE_IDLE) {
        final int firstVisibleItemPosition = tlLayoutManager.findFirstVisibleItemPosition();
        isScrolledByUser = firstVisibleItemPosition != 0;
        // if first visible item is updated, setAddedUntilStopped also should be updated.
        setAddedUntilStopped();
      } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
        isScrolledByUser = true;
      }
    }
  };

  private long topItemId = -1;
  private int firstVisibleItemTopOnStop;

  @Override
  public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
    Log.d(TAG, "onViewStateRestored: " + getStoreName());
    super.onViewStateRestored(savedInstanceState);
    if (savedInstanceState == null) {
      return;
    }
    topItemId = savedInstanceState.getLong(SS_TOP_ITEM_ID, -1);
    firstVisibleItemTopOnStop = savedInstanceState.getInt(SS_TOP_ITEM_TOP, 0);
  }

  @Override
  public void onStart() {
    Log.d(TAG, "onStart: " + getStoreName());
    super.onStart();
    if (topItemId >= 0) {
      final int pos = sortedCache.getPositionById(topItemId);
      tlLayoutManager.scrollToPositionWithOffset(pos, firstVisibleItemTopOnStop);
      topItemId = -1;
      if (pos > 0) {
        addedUntilStopped = true;
      }
    }
    binding.timeline.addOnScrollListener(onScrollListener);

    if (getActivity() instanceof FabHandleable) {
      tlAdapter.setOnSelectedItemChangeListener(new OnSelectedItemChangeListener() {
        @Override
        public void onItemSelected(long entityId) {
          showFab();
        }

        @Override
        public void onItemUnselected() {
          hideFab();
        }
      });
    }
    tlAdapter.setLastItemBoundListener(() -> fetcher.fetchNext());
    final OnUserIconClickedListener userIconClickedListener = createUserIconClickedListener();
    tlAdapter.setOnUserIconClickedListener(userIconClickedListener);
    tlAdapter.registerAdapterDataObserver(itemInsertedObserver);
  }

  @Override
  public void onResume() {
    super.onResume();
    if (!isChildOfViewPager() || isVisibleOnViewPager()) {
      if (tlAdapter.isItemSelected()) {
        showFab();
      } else {
        hideFab();
      }
    }
    final SwipeRefreshLayout swipeLayout = binding.timelineSwipeLayout;
    swipeLayout.setOnRefreshListener(() -> {
      fetcher.fetch();
      swipeLayout.setRefreshing(false);
    });
  }

  private boolean isChildOfViewPager() {
    final Fragment parent = getParentFragment();
    return parent instanceof UserInfoPagerFragment;
  }

  private boolean isVisibleOnViewPager() {
    final Fragment parent = getParentFragment();
    final UserInfoPagerFragment pager = (UserInfoPagerFragment) parent;
    return this == pager.getCurrentFragment();
  }

  @Override
  public void onPause() {
    super.onPause();
    int adapterPos = tlLayoutManager.findFirstVisibleItemPosition();
    if (adapterPos >= 0) {
      final RecyclerView.ViewHolder vh = binding.timeline.findViewHolderForAdapterPosition(adapterPos);
      if (vh != null) {
        topItemId = sortedCache.getId(adapterPos);
        firstVisibleItemTopOnStop = vh.itemView.getTop();
      }
    }
    binding.timelineSwipeLayout.setOnRefreshListener(null);
  }

  @Override
  public void onStop() {
    super.onStop();
    removeOnItemSelectedListener();
    tlAdapter.setLastItemBoundListener(null);
    tlAdapter.setOnSelectedItemChangeListener(null);
    tlAdapter.setOnUserIconClickedListener(null);
    binding.timeline.setOnTouchListener(null);
    binding.timeline.removeOnScrollListener(onScrollListener);
    tlAdapter.unregisterAdapterDataObserver(itemInsertedObserver);
  }

  @Override
  public void onDetach() {
    Log.d(TAG, "onDetach: " + getStoreName());
    super.onDetach();
    if (binding != null) {
      binding.timeline.setItemAnimator(null);
      timelineAnimator = null;
      binding.timeline.removeItemDecoration(timelineDecoration);
      timelineDecoration = null;
      tlLayoutManager.removeAllViews();
      binding.timeline.setLayoutManager(null);
      tlLayoutManager = null;
      binding.timeline.setAdapter(null);
      tlAdapter = null;
    }
    updateEventSubscription.dispose();
    sortedCache.close();
  }

  private OnIffabItemSelectedListener iffabItemSelectedListener;

  private void showFab() {
    final FragmentActivity activity = getActivity();
    if (activity instanceof FabHandleable) {
      ((FabHandleable) activity).showFab(FabHandleable.TYPE_FAB);
      removeOnItemSelectedListener();
      iffabItemSelectedListener = requestWorker.getOnIffabItemSelectedListener(getSelectedItemId());
      ((FabHandleable) activity).addOnItemSelectedListener(iffabItemSelectedListener);
    }
  }

  private void hideFab() {
    final FragmentActivity activity = getActivity();
    if (activity instanceof FabHandleable) {
      ((FabHandleable) activity).hideFab();
    }
    removeOnItemSelectedListener();
  }

  private void removeOnItemSelectedListener() {
    if (getActivity() instanceof FabHandleable) {
      ((FabHandleable) getActivity()).removeOnItemSelectedListener(iffabItemSelectedListener);
      iffabItemSelectedListener = null;
    }
  }

  private boolean stopScroll = false;
  private boolean addedUntilStopped = false;

  private void setAddedUntilStopped() {
    addedUntilStopped = tlLayoutManager.getChildCount() > 0
        && tlLayoutManager.findFirstVisibleItemPosition() != 0;
  }

  public void stopScroll() {
    stopScroll = true;
  }

  public void startScroll() {
    stopScroll = false;
  }

  private boolean canScroll() {
    return isVisible()
        && !tlAdapter.isItemSelected()
        && !stopScroll
        && !isScrolledByUser
        && !addedUntilStopped;
  }

  public void scrollToTop() {
    clearSelectedItem();
    binding.timeline.setLayoutFrozen(false);
    stopScroll = false;
    isScrolledByUser = false;
    addedUntilStopped = false;
    scrollTo(0);
  }

  public void scrollToSelectedItem() {
    binding.timeline.setLayoutFrozen(false);
    tlLayoutManager.scrollToPositionWithOffset(tlAdapter.getSelectedItemViewPosition(), 0);
  }

  private void scrollTo(int position) {
//    Log.d(TAG, "scrollTo: " + position);
    final int firstVisibleItemPosition = tlLayoutManager.findFirstVisibleItemPosition();
    if (firstVisibleItemPosition - position < 4) {
      binding.timeline.smoothScrollToPosition(position);
    } else {
      binding.timeline.scrollToPosition(position + 3);
      binding.timeline.smoothScrollToPosition(position);
    }
  }

  @Override
  public void clearSelectedItem() {
    tlAdapter.clearSelectedItem();
  }

  @Override
  public boolean isItemSelected() {
    return tlAdapter != null && tlAdapter.isItemSelected();
  }

  @Override
  public long getSelectedItemId() {
    return tlAdapter.getSelectedItemId();
  }

  private OnUserIconClickedListener createUserIconClickedListener() {
    final FragmentActivity activity = getActivity();
    if (activity instanceof OnUserIconClickedListener) {
      return ((OnUserIconClickedListener) activity);
    } else {
      return (view, user) -> { /* nop */ };
    }
  }

  public void dropCache() {
    sortedCache.drop();
  }

  interface OnItemClickedListener {
    void onItemClicked(ContentType type, long id, String query);
  }

  private static final String ARGS_STORE_NAME = "store_name";
  private static final String ARGS_ENTITY_ID = "entity_id";
  private static final String ARGS_QUERY = "query";

  static Bundle createArgs(StoreType storeType, long entityId, String query) {
    final Bundle args = new Bundle();
    args.putSerializable(ARGS_STORE_NAME, storeType);
    if (entityId > 0) {
      args.putLong(ARGS_ENTITY_ID, entityId);
    }
    args.putString(ARGS_QUERY, query);
    return args;
  }

  public static TimelineFragment<?> getInstance(StoreType storeType) {
    return getInstance(storeType, -1);
  }

  public static TimelineFragment<?> getInstance(StoreType storeType, long entityId) {
    final TimelineFragment<?> fragment;
    if (storeType.isForStatus()) {
      fragment = new StatusListFragment();
    } else if (storeType.isForUser()) {
      fragment = new UserListFragment();
    } else if (storeType.isForLists()) {
      fragment = new ListsListFragment();
    } else {
      throw new IllegalArgumentException("storeType: " + storeType.name() + " is not capable...");
    }
    final Bundle args = TimelineFragment.createArgs(storeType, entityId, "");
    fragment.setArguments(args);
    return fragment;
  }

  public static TimelineFragment<Status> getInstance(StoreType storeType, String query) {
    if (!storeType.isForStatus()) {
      throw new IllegalArgumentException("storeType: " + storeType.name() + " is not capable...");
    }
    final StatusListFragment fragment = new StatusListFragment();
    final Bundle args = TimelineFragment.createArgs(storeType, -1, query);
    fragment.setArguments(args);
    return fragment;
  }

  String getStoreName() {
    return getStoreType().nameWithSuffix(getEntityId(), getQuery());
  }

  private StoreType getStoreType() {
    return (StoreType) getArguments().getSerializable(ARGS_STORE_NAME);
  }

  private long getEntityId() {
    return getArguments().getLong(ARGS_ENTITY_ID, -1);
  }

  private String getQuery() {
    return getArguments().getString(ARGS_QUERY, "");
  }

  public static class StatusListFragment extends TimelineFragment<Status> {
    @Override
    public void onAttach(Context context) {
      InjectionUtil.getComponent(this).inject(this);
      super.onAttach(context);
      super.tlAdapter = new TimelineAdapter.StatusTimelineAdapter(super.sortedCache);
    }

    @Override
    public void onStart() {
      super.onStart();
      super.tlAdapter.registerAdapterDataObserver(createdAtObserver);
    }

    @Override
    public void onStop() {
      super.onStop();
      super.tlAdapter.unregisterAdapterDataObserver(createdAtObserver);
    }

    private final AdapterDataObserver createdAtObserver = new AdapterDataObserver() {
      @Override
      public void onItemRangeChanged(int positionStart, int itemCount) {
        super.onItemRangeChanged(positionStart, itemCount);
        updateTime();
      }

      @Override
      public void onItemRangeInserted(int positionStart, int itemCount) {
        super.onItemRangeInserted(positionStart, itemCount);
        updateTime();
      }

      @Override
      public void onItemRangeRemoved(int positionStart, int itemCount) {
        super.onItemRangeRemoved(positionStart, itemCount);
        updateTime();
      }

      private void updateTime() {
        final int childCount = StatusListFragment.super.binding.timeline.getChildCount();
        for (int i = 0; i < childCount; i++) {
          final View v = StatusListFragment.super.binding.timeline.getChildAt(i);
          if (v instanceof StatusView) {
            ((StatusView) v).updateTime();
          }
        }
      }
    };
  }

  public static class UserListFragment extends TimelineFragment<User> {
    @Override
    public void onAttach(Context context) {
      InjectionUtil.getComponent(this).inject(this);
      super.onAttach(context);
      super.tlAdapter = new TimelineAdapter.UserListAdapter(super.sortedCache);
    }

    @Override
    public void onStart() {
      super.onStart();
      final OnUserIconClickedListener userIconClickedListener = super.createUserIconClickedListener();
      super.tlAdapter.setOnItemViewClickListener((viewHolder, itemId, clickedView) -> {
        final int pos = sortedCache.getPositionById(itemId);
        final User user = sortedCache.get(pos);
        userIconClickedListener.onUserIconClicked(viewHolder.getUserIcon(), user);
      });
    }

    @Override
    public void onStop() {
      super.onStop();
      super.tlAdapter.setOnItemViewClickListener(null);
    }
  }

  public static class ListsListFragment extends TimelineFragment<UserList> {
    @Override
    public void onAttach(Context context) {
      InjectionUtil.getComponent(this).inject(this);
      super.onAttach(context);
      super.tlAdapter = new TimelineAdapter.ListListAdapter(super.sortedCache);
    }

    @Override
    public void onStart() {
      super.onStart();
      super.tlAdapter.setOnItemViewClickListener((viewHolder, itemId, clickedView) -> {
        final FragmentActivity activity = getActivity();
        if (!(activity instanceof TimelineFragment.OnItemClickedListener)) {
          return;
        }
        final OnItemClickedListener listener = (OnItemClickedListener) activity;
        final int pos = sortedCache.getPositionById(itemId);
        final UserList userList = sortedCache.get(pos);
        listener.onItemClicked(ContentType.LISTS, itemId, userList.getName());
      });
    }

    @Override
    public void onStop() {
      super.onStop();
      super.tlAdapter.setOnItemViewClickListener(null);
    }
  }

  @Override
  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    final Animation animation = TimelineContainerSwitcher.makeSwitchingAnimation(getContext(), transit, enter);
    return animation != null ? animation
        : super.onCreateAnimation(transit, enter, nextAnim);
  }
}
