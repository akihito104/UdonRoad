/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */
package com.freshdigitable.udonroad;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.freshdigitable.udonroad.databinding.FragmentTimelineBinding;

import java.util.List;

import twitter4j.Status;

public class TimelineFragment extends Fragment {
  @SuppressWarnings("unused")
  private static final String TAG = TimelineFragment.class.getSimpleName();
  private FragmentTimelineBinding binding;
  private final TimelineAdapter tlAdapter = new TimelineAdapter();
  private LinearLayoutManager tlLayoutManager;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_timeline, container, false);
    binding = DataBindingUtil.bind(view);
    return view;
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    binding.timeline.setHasFixedSize(true);
    RecyclerView.ItemDecoration itemDecoration = new TimelineDecoration();
    binding.timeline.addItemDecoration(itemDecoration);
    tlLayoutManager = new LinearLayoutManager(getActivity());
    binding.timeline.setLayoutManager(tlLayoutManager);
    binding.timeline.setItemAnimator(new TimelineAnimator());
    tlAdapter.setOnSelectedTweetChangeListener(
        new TimelineAdapter.OnSelectedTweetChangeListener() {
          @Override
          public void onTweetSelected(Status status) {
            if (selectedTweetChangeListener != null) {
              selectedTweetChangeListener.onTweetSelected(status);
            }
            binding.fab.setVisibility(View.VISIBLE);
          }

          @Override
          public void onTweetUnselected() {
            if (selectedTweetChangeListener != null) {
              selectedTweetChangeListener.onTweetUnselected();
            }
            binding.fab.setVisibility(View.GONE);
          }
        });
    tlAdapter.setLastItemBoundListener(lastItemBoundListener);
    binding.timeline.setAdapter(tlAdapter);

    binding.fab.setVisibility(View.GONE);
    binding.fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
      }
    });
    binding.fab.setOnFlingListener(new FlingableFloatingActionButton.OnFlingListener() {
      @Override
      public void onFling(FlingableFloatingActionButton.Direction direction) {
        if (flingListener == null || !isTweetSelected()) {
          return;
        }
        flingListener.onFling(direction, tlAdapter.getSelectedStatus());
      }
    });
  }

  private TimelineAdapter.LastItemBoundListener lastItemBoundListener;

  public void setLastItemBoundListener(TimelineAdapter.LastItemBoundListener lastItemBoundListener) {
    this.lastItemBoundListener = lastItemBoundListener;
  }

  private boolean stopScroll = false;

  public void setStopScroll(boolean isStopScroll) {
    stopScroll = isStopScroll;
  }

  private boolean canScrollToAdd() {
    int firstVisibleItem = tlLayoutManager.findFirstVisibleItemPosition();
    return firstVisibleItem == 0
        && !tlAdapter.isStatusViewSelected()
        && !stopScroll;
  }

  public void addNewStatus(Status status) {
    if (canScrollToAdd()) {
      tlAdapter.addNewStatus(status);
      scrollTo(0);
    } else {
      tlAdapter.addNewStatus(status);
    }
  }

  public void addNewStatuses(List<Status> statuses) {
    tlAdapter.addNewStatuses(statuses);
  }

  public void addStatusesAtLast(List<Status> statuses) {
    tlAdapter.addNewStatusesAtLast(statuses);
  }

  public void deleteStatus(long id) {
    tlAdapter.deleteStatus(id);
    tlAdapter.notifyDataSetChanged();
  }

  public void scrollTo(int position) {
    binding.timeline.smoothScrollToPosition(position);
    stopScroll = false;
  }

  public void clearSelectedTweet() {
    tlAdapter.clearSelectedTweet();
  }

  public boolean isTweetSelected() {
    return tlAdapter.isStatusViewSelected();
  }

  interface OnFlingForSelectedStatusListener {
    void onFling(FlingableFloatingActionButton.Direction direction, Status status);
  }

  private OnFlingForSelectedStatusListener flingListener;

  public void setOnFlingForSelectedStatusListener(OnFlingForSelectedStatusListener listener) {
    this.flingListener = listener;
  }

  private TimelineAdapter.OnSelectedTweetChangeListener selectedTweetChangeListener;

  public void setOnSelectedTweetChangeListener(TimelineAdapter.OnSelectedTweetChangeListener listener) {
    this.selectedTweetChangeListener = listener;
  }

  public void setUserIconClickedListener(TimelineAdapter.OnUserIconClickedListener listener) {
    tlAdapter.setOnUserIconClickedListener(listener);
  }
}
