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
import android.support.v4.app.FragmentTransaction;
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
import android.view.animation.AnimationUtils;

import com.freshdigitable.udonroad.listitem.OnUserIconClickedListener;
import com.freshdigitable.udonroad.TimelineAdapter.OnSelectedItemChangeListener;
import com.freshdigitable.udonroad.databinding.FragmentTimelineBinding;
import com.freshdigitable.udonroad.datastore.UpdateEvent;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;
import com.freshdigitable.udonroad.listitem.StatusView;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.subscriber.ListFetchStrategy;
import com.freshdigitable.udonroad.subscriber.ListRequestWorker;

import javax.inject.Inject;

import rx.Subscription;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.User;

/**
 * TimelineFragment provides RecyclerView to show timeline.
 *
 * Created by Akihit.
 */
public abstract class TimelineFragment<T> extends Fragment {
  @SuppressWarnings("unused")
  private static final String TAG = TimelineFragment.class.getSimpleName();
  public static final String BUNDLE_IS_SCROLLED_BY_USER = "is_scrolled_by_user";
  public static final String BUNDLE_STOP_SCROLL = "stop_scroll";
  private FragmentTimelineBinding binding;
  private TimelineAdapter<T> tlAdapter;
  private LinearLayoutManager tlLayoutManager;
  private Subscription updateEventSubscription;
  @Inject
  ListRequestWorker<T> requestWorker;
  private TimelineDecoration timelineDecoration;
  private TimelineAnimator timelineAnimator;

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
    Log.d(TAG, "onCreateView: ");
    if (savedInstanceState != null) {
      isScrolledByUser = savedInstanceState.getBoolean(BUNDLE_IS_SCROLLED_BY_USER);
      stopScroll = savedInstanceState.getBoolean(BUNDLE_STOP_SCROLL);
    }
    if (binding == null) {
      binding = DataBindingUtil.inflate(inflater, R.layout.fragment_timeline, container, false);
    }
    return binding.getRoot();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    Log.d(TAG, "onSaveInstanceState: ");
    super.onSaveInstanceState(outState);
    outState.putBoolean(BUNDLE_IS_SCROLLED_BY_USER, isScrolledByUser);
    outState.putBoolean(BUNDLE_STOP_SCROLL, stopScroll);
  }

  private boolean isScrolledByUser = false;

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    Log.d(TAG, "onActivityCreated: ");
    super.onActivityCreated(savedInstanceState);
    binding.timeline.setHasFixedSize(true);
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
    if (tlAdapter == null) {
      requestWorker.open(getStoreType(), getEntityId() > 0 ? Long.toString(getEntityId()) : null);
      tlAdapter = new TimelineAdapter<>(requestWorker.getCache());
      binding.timeline.setAdapter(tlAdapter);
      fetchTweet(null);
    }
    tlAdapter.registerAdapterDataObserver(itemInsertedObserver);
    tlAdapter.registerAdapterDataObserver(createdAtObserver);

    if (updateEventSubscription == null || updateEventSubscription.isUnsubscribed()) {
      updateEventSubscription = requestWorker.getCache().observeUpdateEvent()
          .subscribe(event -> {
            if (event.type == UpdateEvent.EventType.INSERT) {
              tlAdapter.notifyItemRangeInserted(event.index, event.length);
            } else if (event.type == UpdateEvent.EventType.CHANGE) {
              tlAdapter.notifyItemRangeChanged(event.index, event.length);
            } else if (event.type == UpdateEvent.EventType.DELETE) {
              tlAdapter.notifyItemRangeRemoved(event.index, event.length);
            }
          });
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

  private final AdapterDataObserver createdAtObserver
      = new AdapterDataObserver() {
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
      final int childCount = binding.timeline.getChildCount();
      for (int i = 0; i < childCount; i++) {
        final View v = binding.timeline.getChildAt(i);
        ((StatusView) v).updateTime();
      }
    }
  };

  private RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
      if (newState == RecyclerView.SCROLL_STATE_IDLE) {
        final int firstVisibleItemPosition = tlLayoutManager.findFirstVisibleItemPosition();
        isScrolledByUser = firstVisibleItemPosition != 0;
        // if first visible item is updated, isAddedUntilStopped also should be updated.
        isAddedUntilStopped();
      } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
        isScrolledByUser = true;
      }
    }
  };

  @Override
  public void onStart() {
    super.onStart();
    if (firstVisibleItemPosOnStop >= 0) {
      tlLayoutManager.scrollToPositionWithOffset(firstVisibleItemPosOnStop, firstVisibleItemTopOnStop);
      firstVisibleItemPosOnStop = -1;
      tlAdapter.unregisterAdapterDataObserver(firstItemObserver);
      firstItemObserver = null;
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
    tlAdapter.setLastItemBoundListener(
        lastPageCursor -> fetchTweet(new Paging(1, 20, 1, lastPageCursor)));
    final OnUserIconClickedListener userIconClickedListener = createUserIconClickedListener();
    tlAdapter.setOnUserIconClickedListener(userIconClickedListener);
    isAddedUntilStopped();

    if (isVisible()) {
      if (tlAdapter.isItemSelected()) {
        showFab();
      } else {
        hideFab();
      }
    }
  }

  public long getSelectedTweetId() {
    return tlAdapter.getSelectedItemId();
  }

  private int firstVisibleItemPosOnStop = -1;
  private int firstVisibleItemTopOnStop;

  @Override
  public void onStop() {
    super.onStop();
    tlAdapter.setLastItemBoundListener(null);
    tlAdapter.setOnSelectedItemChangeListener(null);
    tlAdapter.setOnUserIconClickedListener(null);
    binding.timeline.setOnTouchListener(null);
    binding.timeline.removeOnScrollListener(onScrollListener);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    firstVisibleItemPosOnStop = tlLayoutManager.findFirstVisibleItemPosition();
    if (firstVisibleItemPosOnStop >= 0) {
      final RecyclerView.ViewHolder vh = binding.timeline.findViewHolderForAdapterPosition(firstVisibleItemPosOnStop);
      if (vh != null) {
        firstVisibleItemTopOnStop = vh.itemView.getTop();
        firstItemObserver = getFirstItemObserver();
        tlAdapter.registerAdapterDataObserver(firstItemObserver);
      }
    }
    tlAdapter.unregisterAdapterDataObserver(itemInsertedObserver);
    tlAdapter.unregisterAdapterDataObserver(createdAtObserver);
  }

  private AdapterDataObserver firstItemObserver;

  private AdapterDataObserver getFirstItemObserver() {
    return new AdapterDataObserver() {
      @Override
      public void onItemRangeInserted(int positionStart, int itemCount) {
        if (positionStart <= firstVisibleItemPosOnStop) {
          Log.d(TAG, "onItemRangeInserted: inserted above");
          firstVisibleItemPosOnStop += itemCount;
        }
      }

      @Override
      public void onItemRangeRemoved(int positionStart, int itemCount) {
        if (positionStart <= firstVisibleItemPosOnStop) {
          Log.d(TAG, "onItemRangeRemoved: removed above");
          firstVisibleItemPosOnStop -= itemCount;
          if (firstVisibleItemPosOnStop < 0) {
            firstVisibleItemPosOnStop = 0;
            firstVisibleItemTopOnStop = 0;
          }
        }
      }
    };
  }

  @Override
  public void onDetach() {
    Log.d(TAG, "onDetach: ");
    super.onDetach();
    if (firstItemObserver != null) {
      tlAdapter.unregisterAdapterDataObserver(firstItemObserver);
      firstItemObserver = null;
    }
    binding.timeline.setItemAnimator(null);
    timelineAnimator = null;
    binding.timeline.removeItemDecoration(timelineDecoration);
    timelineDecoration = null;
    tlLayoutManager.removeAllViews();
    binding.timeline.setLayoutManager(null);
    tlLayoutManager = null;
    binding.timeline.setAdapter(null);
    updateEventSubscription.unsubscribe();
    requestWorker.close();
    requestWorker.drop();
    tlAdapter = null;
  }

  private OnIffabItemSelectedListener iffabItemSelectedListener;

  private void showFab() {
    final FragmentActivity activity = getActivity();
    if (activity instanceof FabHandleable) {
      ((FabHandleable) activity).showFab();
      if (iffabItemSelectedListener != null) {
        ((FabHandleable) activity).removeOnItemSelectedListener(iffabItemSelectedListener);
      }
      iffabItemSelectedListener = requestWorker.getOnIffabItemSelectedListener(getSelectedTweetId());
      ((FabHandleable) activity).addOnItemSelectedListener(iffabItemSelectedListener);
    }
  }

  private void hideFab() {
    final FragmentActivity activity = getActivity();
    if (activity instanceof FabHandleable) {
      ((FabHandleable) activity).hideFab();
      ((FabHandleable) activity).removeOnItemSelectedListener(iffabItemSelectedListener);
      iffabItemSelectedListener = null;
    }
  }

  private boolean stopScroll = false;
  private boolean addedUntilStopped = false;

  private void isAddedUntilStopped() {
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
    clearSelectedTweet();
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

  public void clearSelectedTweet() {
    tlAdapter.clearSelectedItem();
  }

  public boolean isTweetSelected() {
    return tlAdapter.isItemSelected();
  }

  private OnUserIconClickedListener createUserIconClickedListener() {
    final FragmentActivity activity = getActivity();
    if (activity instanceof OnUserIconClickedListener) {
      return ((OnUserIconClickedListener) activity);
    } else {
      return (view, user) -> { /* nop */ };
    }
  }

  private void fetchTweet(@Nullable Paging paging) {
    final ListFetchStrategy fetcher = requestWorker.getFetchStrategy(getEntityId());
    if (paging == null) {
      fetcher.fetch();
    } else {
      fetcher.fetch(paging);
    }
  }

  private static final String ARGS_STORE_NAME = "store_name";
  private static final String ARGS_ENTITY_ID = "entity_id";

  private static Bundle createArgs(StoreType storeType, long entityId) {
    final Bundle args = new Bundle();
    args.putSerializable(ARGS_STORE_NAME, storeType);
    if (entityId > 0) {
      args.putLong(ARGS_ENTITY_ID, entityId);
    }
    return args;
  }

  private StoreType getStoreType() {
    return (StoreType) getArguments().getSerializable(ARGS_STORE_NAME);
  }

  private long getEntityId() {
    return getArguments().getLong(ARGS_ENTITY_ID, -1);
  }

  public static class StatusListFragment extends TimelineFragment<Status> {
    public static StatusListFragment getInstance(StoreType storeType) {
      return getInstance(storeType, -1);
    }

    public static StatusListFragment getInstance(StoreType storeType, long entityId) {
      final Bundle args = TimelineFragment.createArgs(storeType, entityId);
      final StatusListFragment fragment = new StatusListFragment();
      fragment.setArguments(args);
      return fragment;
    }

    @Override
    public void onAttach(Context context) {
      super.onAttach(context);
      InjectionUtil.getComponent(this).inject(this);
    }
  }

  public static class UserListFragment extends TimelineFragment<User> {
    public static UserListFragment getInstance(StoreType storeType) {
      return getInstance(storeType, -1);
    }

    public static UserListFragment getInstance(StoreType storeType, long entityId) {
      final Bundle args = TimelineFragment.createArgs(storeType, entityId);
      final UserListFragment fragment = new UserListFragment();
      fragment.setArguments(args);
      return fragment;
    }

    @Override
    public void onAttach(Context context) {
      super.onAttach(context);
      InjectionUtil.getComponent(this).inject(this);
    }
  }

  @Override
  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    if (transit == FragmentTransaction.TRANSIT_FRAGMENT_OPEN) {
      if (!enter) {
        return AnimationUtils.makeOutAnimation(getContext(), true);
      } else {
        return AnimationUtils.makeInAnimation(getContext(), false);
      }
    }
    if (transit == FragmentTransaction.TRANSIT_FRAGMENT_CLOSE) {
      if (enter) {
        return AnimationUtils.makeInAnimation(getContext(), false);
      } else {
        return AnimationUtils.makeOutAnimation(getContext(), true);
      }
    }
    return super.onCreateAnimation(transit, enter, nextAnim);
  }
}
