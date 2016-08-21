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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.freshdigitable.udonroad.StatusViewBase.OnUserIconClickedListener;
import com.freshdigitable.udonroad.databinding.FragmentTimelineBinding;
import com.freshdigitable.udonroad.datastore.TimelineStore;
import com.freshdigitable.udonroad.ffab.FlingableFAB;
import com.freshdigitable.udonroad.ffab.FlingableFABHelper;

import rx.Subscription;
import rx.functions.Action1;
import twitter4j.Paging;

public class TimelineFragment extends Fragment {
  @SuppressWarnings("unused")
  private static final String TAG = TimelineFragment.class.getSimpleName();
  public static final String BUNDLE_IS_SCROLLED_BY_USER = "is_scrolled_by_user";
  public static final String BUNDLE_STOP_SCROLL = "stop_scroll";
  private FragmentTimelineBinding binding;
  private TimelineAdapter tlAdapter;
  private LinearLayoutManager tlLayoutManager;
  private Subscription insertEventSubscription;
  private Subscription deleteEventSubscription;
  protected TimelineSubscriber<TimelineStore> timelineSubscriber;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
  }

  @Override
  public void onDetach() {
    insertEventSubscription.unsubscribe();
    deleteEventSubscription.unsubscribe();
    super.onDetach();
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    binding = FragmentTimelineBinding.inflate(inflater, container, false);

    if (savedInstanceState != null) {
      isScrolledByUser = savedInstanceState.getBoolean(BUNDLE_IS_SCROLLED_BY_USER);
      stopScroll = savedInstanceState.getBoolean(BUNDLE_STOP_SCROLL);
    }
    return binding.getRoot();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(BUNDLE_IS_SCROLLED_BY_USER, isScrolledByUser);
    outState.putBoolean(BUNDLE_STOP_SCROLL, stopScroll);
  }

  private boolean isScrolledByUser = false;

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    binding.timeline.setHasFixedSize(true);
    binding.timeline.addItemDecoration(new TimelineDecoration());
    tlLayoutManager = new LinearLayoutManager(getContext());
    tlLayoutManager.setAutoMeasureEnabled(true);
    binding.timeline.setLayoutManager(tlLayoutManager);
    binding.timeline.setItemAnimator(new TimelineAnimator());
    binding.timeline.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
//        Log.d(TAG, "onTouch: " + event.getAction());
        if (event.getAction() == MotionEvent.ACTION_UP) {
          final int firstVisibleItemPosition = tlLayoutManager.findFirstVisibleItemPosition();
          isScrolledByUser = firstVisibleItemPosition != 0;
          isAddedUntilStopped();
        }
        return false;
      }
    });

    final TimelineStore timelineStore = timelineSubscriber.getStatusStore();
    tlAdapter = new TimelineAdapter(timelineStore);
    insertEventSubscription = timelineStore.observeInsertEvent()
        .subscribe(new Action1<Integer>() {
          @Override
          public void call(Integer position) {
            tlAdapter.notifyItemInserted(position);
          }
        });
    deleteEventSubscription = timelineStore.observeDeleteEvent()
        .subscribe(new Action1<Integer>() {
          @Override
          public void call(Integer position) {
            tlAdapter.notifyItemRemoved(position);
          }
        });
    final FlingableFAB fab = fabHelper.getFab();
    tlAdapter.setOnSelectedTweetChangeListener(
        new TimelineAdapter.OnSelectedTweetChangeListener() {
          @Override
          public void onTweetSelected(long statusId) {
            fab.show();
          }

          @Override
          public void onTweetUnselected() {
            fab.hide();
          }
        });
    tlAdapter.setLastItemBoundListener(new TimelineAdapter.LastItemBoundListener() {
      @Override
      public void onLastItemBound(long statusId) {
        fetchTweet(new Paging(1, 20, 1, statusId - 1));
      }
    });
    tlAdapter.setOnUserIconClickedListener(userIconClickedListener);
    binding.timeline.setAdapter(tlAdapter);
    fetchTweet();
  }

  private final RecyclerView.AdapterDataObserver itemInsertedObserver
      = new RecyclerView.AdapterDataObserver() {
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

  private final RecyclerView.AdapterDataObserver createdAtObserver
      = new RecyclerView.AdapterDataObserver() {
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

  @Override
  public void onStart() {
    super.onStart();
    tlAdapter.registerAdapterDataObserver(itemInsertedObserver);
    tlAdapter.registerAdapterDataObserver(createdAtObserver);
    isAddedUntilStopped();
    if (tlAdapter.isStatusViewSelected()) {
      fabHelper.getFab().show();
    } else {
      fabHelper.getFab().hide();
    }
  }

  public long getSelectedTweetId() {
    return tlAdapter.getSelectedTweetId();
  }

  public void tearDownOnFlingListener() {
    if (fabHelper == null) {
      return;
    }
    fabHelper.getFab().setOnFlingListener(null);
  }

  @Override
  public void onResume() {
    super.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
  }

  @Override
  public void onStop() {
    Log.d(TAG, "onStop: ");
    super.onStop();
    tlAdapter.unregisterAdapterDataObserver(itemInsertedObserver);
    tlAdapter.unregisterAdapterDataObserver(createdAtObserver);
    tearDownOnFlingListener();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    tlAdapter.setLastItemBoundListener(null);
    tlAdapter.setOnSelectedTweetChangeListener(null);
    tlAdapter.setOnUserIconClickedListener(null);
    binding.timeline.setOnTouchListener(null);
    binding.timeline.setAdapter(null);
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
    binding.timeline.smoothScrollToPosition(position);
  }

  public void clearSelectedTweet() {
    tlAdapter.clearSelectedTweet();
  }

  public boolean isTweetSelected() {
    return tlAdapter.isStatusViewSelected();
  }

  private OnUserIconClickedListener userIconClickedListener;

  public void setUserIconClickedListener(OnUserIconClickedListener listener) {
    this.userIconClickedListener = listener;
  }

  protected void fetchTweet() {
    timelineSubscriber.fetchHomeTimeline();
  }

  protected void fetchTweet(Paging paging) {
    timelineSubscriber.fetchHomeTimeline(paging);
  }

  private FlingableFABHelper fabHelper;

  public void setFABHelper(FlingableFABHelper flingableFABHelper) {
    this.fabHelper = flingableFABHelper;
  }

  protected static <T extends Fragment> T getInstance(T fragment, long userId) {
    final Bundle args = new Bundle();
    args.putLong("user_id", userId);
    fragment.setArguments(args);
    return fragment;
  }

  protected long getUserId() {
    final Bundle arguments = getArguments();
    return arguments.getLong("user_id");
  }

  public void setTimelineSubscriber(TimelineSubscriber<TimelineStore> timelineSubscriber) {
    this.timelineSubscriber = timelineSubscriber;
  }

  public TimelineSubscriber<TimelineStore> getTimelineSubscriber() {
    return timelineSubscriber;
  }

  @Nullable
  public View getSelectedView() {
    return tlAdapter.getSelectedView();
  }
}
