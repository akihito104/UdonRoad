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
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.freshdigitable.udonroad.StatusViewBase.OnUserIconClickedListener;
import com.freshdigitable.udonroad.databinding.FragmentTimelineBinding;
import com.freshdigitable.udonroad.datastore.TimelineStore;

import rx.Subscription;
import rx.functions.Action1;
import twitter4j.Paging;
import twitter4j.User;

/**
 * TimelineFragment provides RecyclerView to show timeline.
 */
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

    if (getActivity() instanceof FabHandleable) {
      tlAdapter.setOnSelectedTweetChangeListener(
          new TimelineAdapter.OnSelectedTweetChangeListener() {
            @Override
            public void onTweetSelected(long statusId) {
              showFab();
            }

            @Override
            public void onTweetUnselected() {
              hideFab();
            }
          });
    }
    tlAdapter.setLastItemBoundListener(new TimelineAdapter.LastItemBoundListener() {
      @Override
      public void onLastItemBound(long statusId) {
        fetchTweet(new Paging(1, 20, 1, statusId - 1));
      }
    });
    final OnUserIconClickedListener userIconClickedListener = createUserIconClickedListener();
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

    if (isVisible()) {
      if (tlAdapter.isStatusViewSelected()) {
        showFab();
      } else {
        hideFab();
      }
    }
  }

  public long getSelectedTweetId() {
    return tlAdapter.getSelectedTweetId();
  }

  @Override
  public void onStop() {
    Log.d(TAG, "onStop: ");
    super.onStop();
    tlAdapter.unregisterAdapterDataObserver(itemInsertedObserver);
    tlAdapter.unregisterAdapterDataObserver(createdAtObserver);
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
      binding.timeline.scrollToPosition(position + 1);
      binding.timeline.smoothScrollToPosition(position);
    }
  }

  public void clearSelectedTweet() {
    tlAdapter.clearSelectedTweet();
  }

  public boolean isTweetSelected() {
    return tlAdapter.isStatusViewSelected();
  }

  private OnUserIconClickedListener createUserIconClickedListener() {
    final FragmentActivity activity = getActivity();
    if (activity instanceof OnUserIconClickedListener) {
      return ((OnUserIconClickedListener) activity);
    } else {
      return new OnUserIconClickedListener() {
        @Override
        public void onClicked(View view, User user) {
          // nop
        }
      };
    }
  }

  protected void fetchTweet() {
    fetchTweet(null);
  }

  public static final String EXTRA_PAGING = "paging";

  protected void fetchTweet(@Nullable Paging paging) {
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

  public static TimelineFragment getInstance(Fragment fragment, int requestCode) {
    final TimelineFragment timelineFragment = new TimelineFragment();
    timelineFragment.setTargetFragment(fragment, requestCode);
    return timelineFragment;
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
