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
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import com.freshdigitable.udonroad.StatusViewBase.OnUserIconClickedListener;
import com.freshdigitable.udonroad.TimelineAdapter.OnSelectedEntityChangeListener;
import com.freshdigitable.udonroad.databinding.FragmentTimelineBinding;
import com.freshdigitable.udonroad.datastore.SortedCache;

import rx.Subscription;
import twitter4j.Paging;

/**
 * TimelineFragment provides RecyclerView to show timeline.
 *
 * Created by Akihit.
 */
public class TimelineFragment<T> extends Fragment {
  @SuppressWarnings("unused")
  private static final String TAG = TimelineFragment.class.getSimpleName();
  public static final String BUNDLE_IS_SCROLLED_BY_USER = "is_scrolled_by_user";
  public static final String BUNDLE_STOP_SCROLL = "stop_scroll";
  private FragmentTimelineBinding binding;
  private TimelineAdapter<T> tlAdapter;
  private LinearLayoutManager tlLayoutManager;
  private Subscription insertEventSubscription;
  private Subscription deleteEventSubscription;
  private SortedCache<T> timelineStore;
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
    }
    binding.timeline.addItemDecoration(timelineDecoration);

    if (tlLayoutManager == null) {
      tlLayoutManager = new LinearLayoutManager(getContext());
      tlLayoutManager.setAutoMeasureEnabled(true);
    }
    binding.timeline.setLayoutManager(tlLayoutManager);

    if (timelineAnimator == null) {
      timelineAnimator = new TimelineAnimator();
      binding.timeline.setItemAnimator(timelineAnimator);
    }
    if (tlAdapter == null) {
      tlAdapter = new TimelineAdapter<>(timelineStore);
      fetchTweet(null);
    }
    tlAdapter.registerAdapterDataObserver(itemInsertedObserver);
    tlAdapter.registerAdapterDataObserver(createdAtObserver);
    binding.timeline.setAdapter(tlAdapter);

    if (insertEventSubscription == null || insertEventSubscription.isUnsubscribed()) {
      insertEventSubscription = timelineStore.observeInsertEvent()
          .subscribe(position -> tlAdapter.notifyItemInserted(position));
    }
    if (deleteEventSubscription == null || deleteEventSubscription.isUnsubscribed()) {
      deleteEventSubscription = timelineStore.observeDeleteEvent()
          .subscribe(position -> tlAdapter.notifyItemRemoved(position));
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
    }
    binding.timeline.addOnScrollListener(onScrollListener);

    if (getActivity() instanceof FabHandleable) {
      tlAdapter.setOnSelectedEntityChangeListener(new OnSelectedEntityChangeListener() {
        @Override
        public void onEntitySelected(long entityId) {
          showFab();
        }

        @Override
        public void onEntityUnselected() {
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
      if (tlAdapter.isStatusViewSelected()) {
        showFab();
      } else {
        hideFab();
      }
    }
  }

  public long getSelectedTweetId() {
    return tlAdapter.getSelectedEntityId();
  }

  private int firstVisibleItemPosOnStop = -1;
  private int firstVisibleItemTopOnStop;

  @Override
  public void onStop() {
    super.onStop();
    tlAdapter.setLastItemBoundListener(null);
    tlAdapter.setOnSelectedEntityChangeListener(null);
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
      firstVisibleItemTopOnStop = vh.itemView.getTop();
      tlAdapter.registerAdapterDataObserver(firstItemObserver);
    }
    tlAdapter.unregisterAdapterDataObserver(itemInsertedObserver);
    tlAdapter.unregisterAdapterDataObserver(createdAtObserver);
    binding.timeline.removeItemDecoration(timelineDecoration);
    tlLayoutManager.removeAllViews();
    binding.timeline.setLayoutManager(null);
    binding.timeline.setAdapter(null);
  }

  private final AdapterDataObserver firstItemObserver = new AdapterDataObserver() {
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

  @Override
  public void onDetach() {
    Log.d(TAG, "onDetach: ");
    super.onDetach();
    if (firstVisibleItemPosOnStop >= 0) {
      tlAdapter.unregisterAdapterDataObserver(firstItemObserver);
    }
    insertEventSubscription.unsubscribe();
    deleteEventSubscription.unsubscribe();
  }

  private void showFab() {
    final FragmentActivity activity = getActivity();
    if (activity instanceof FabHandleable) {
      ((FabHandleable) activity).showFab();
    }
  }

  private void hideFab() {
    final FragmentActivity activity = getActivity();
    if (activity instanceof FabHandleable) {
      ((FabHandleable) activity).hideFab();
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
        && !tlAdapter.isStatusViewSelected()
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

  private void scrollTo(int position) {
//    Log.d(TAG, "scrollTo: ");
    final int firstVisibleItemPosition = tlLayoutManager.findFirstVisibleItemPosition();
    if (firstVisibleItemPosition - position < 4) {
      binding.timeline.smoothScrollToPosition(position);
    } else {
      binding.timeline.scrollToPosition(position + 3);
      binding.timeline.smoothScrollToPosition(position);
    }
  }

  public void clearSelectedTweet() {
    tlAdapter.clearSelectedEntity();
  }

  public boolean isTweetSelected() {
    return tlAdapter.isStatusViewSelected();
  }

  private OnUserIconClickedListener createUserIconClickedListener() {
    final FragmentActivity activity = getActivity();
    if (activity instanceof OnUserIconClickedListener) {
      return ((OnUserIconClickedListener) activity);
    } else {
      return (view, user) -> { /* nop */ };
    }
  }

  public static final String EXTRA_PAGING = "paging";

  private void fetchTweet(@Nullable Paging paging) {
    final FragmentActivity activity = getActivity();
    if (activity instanceof OnFetchTweets) {
      fetchTweet((OnFetchTweets) activity, paging);
      return;
    }
    final Fragment targetFragment = getTargetFragment();
    if (targetFragment != null) {
      final Intent intent = new Intent();
      intent.putExtra(EXTRA_PAGING, paging);
      targetFragment.onActivityResult(getTargetRequestCode(), 1, intent);
    }
  }

  private void fetchTweet(@NonNull OnFetchTweets fetcher, @Nullable Paging paging) {
    if (paging == null) {
      fetcher.fetchTweet();
    } else {
      fetcher.fetchTweet(paging);
    }
  }

  public static <T> TimelineFragment<T> getInstance(Fragment fragment, int requestCode) {
    final TimelineFragment<T> timelineFragment = new TimelineFragment<>();
    timelineFragment.setTargetFragment(fragment, requestCode);
    return timelineFragment;
  }

  public void setSortedCache(SortedCache<T> sortedCache) {
    this.timelineStore = sortedCache;
  }

  @Nullable @SuppressWarnings("unused")
  public View getSelectedView() {
    return tlAdapter.getSelectedView();
  }

  @Override
  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    if (transit == FragmentTransaction.TRANSIT_FRAGMENT_OPEN) {
      if (!enter) {
        return AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_out_right);
      }
    }
    if (transit == FragmentTransaction.TRANSIT_FRAGMENT_CLOSE) {
      if (enter) {
        return AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_in_left);
      }
    }
    return super.onCreateAnimation(transit, enter, nextAnim);
  }

  interface OnFetchTweets {
    void fetchTweet();

    void fetchTweet(Paging paging);
  }
}
