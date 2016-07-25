/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */
package com.freshdigitable.udonroad;

import android.content.Context;
import android.databinding.DataBindingUtil;
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

import com.freshdigitable.udonroad.databinding.FragmentTimelineBinding;
import com.freshdigitable.udonroad.ffab.FlingableFAB;
import com.freshdigitable.udonroad.ffab.FlingableFABHelper;
import com.freshdigitable.udonroad.ffab.OnFlingAdapter;

import java.util.List;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.TwitterException;

public class TimelineFragment extends Fragment {
  @SuppressWarnings("unused")
  private static final String TAG = TimelineFragment.class.getSimpleName();
  private FragmentTimelineBinding binding;
  private final TimelineAdapter tlAdapter = new TimelineAdapter();
  private LinearLayoutManager tlLayoutManager;
  @Inject
  TwitterApi twitterApi;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    InjectionUtil.getComponent(this).inject(this);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_timeline, container, false);
    binding = DataBindingUtil.bind(view);
    return view;
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
          addedUntilStopped = firstVisibleItemPosition != 0;
        }
        return false;
      }
    });

    final TimelineAdapter adapter = getTimelineAdapter();
    final FlingableFAB fab = fabHelper.getFab();
    adapter.setOnSelectedTweetChangeListener(
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
    adapter.setLastItemBoundListener(new TimelineAdapter.LastItemBoundListener() {
      @Override
      public void onLastItemBound(long statusId) {
        fetchTweet(new Paging(1, 20, 1, statusId - 1));
      }
    });
    binding.timeline.setAdapter(adapter);
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
    final TimelineAdapter adapter = getTimelineAdapter();
    adapter.registerAdapterDataObserver(itemInsertedObserver);
    adapter.registerAdapterDataObserver(createdAtObserver);
    fetchTweet();
    setupOnFlingListener();
  }

  public void setupOnFlingListener() {
    final TimelineAdapter adapter = getTimelineAdapter();
    fabHelper.getFab().setOnFlingListener(new OnFlingAdapter() {
      @Override
      public void onFling(Direction direction) {
        if (!isTweetSelected()) {
          return;
        }
        final long id = adapter.getSelectedTweetId();
        if (Direction.UP.equals(direction)) {
          fetchFavorite(id);
        } else if (Direction.RIGHT.equals(direction)) {
          fetchRetweet(id);
        } else if (Direction.UP_RIGHT.equals(direction)) {
          fetchFavorite(id);
          fetchRetweet(id);
        } else if (Direction.LEFT.equals(direction)) {
          showStatusDetail(id);
        }
      }
    });
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
    final TimelineAdapter adapter = getTimelineAdapter();
    adapter.unregisterAdapterDataObserver(itemInsertedObserver);
    adapter.unregisterAdapterDataObserver(createdAtObserver);
    tearDownOnFlingListener();
  }

  @Override
  public void onDestroyView() {
    final TimelineAdapter adapter = getTimelineAdapter();
    adapter.setLastItemBoundListener(null);
    adapter.setOnSelectedTweetChangeListener(null);
    adapter.setOnUserIconClickedListener(null);
    binding.timeline.setOnTouchListener(null);
    binding.timeline.setAdapter(null);
    super.onDestroyView();
  }

  private StatusDetailFragment statusDetail;

  private void showStatusDetail(long status) {
    statusDetail = StatusDetailFragment.getInstance(status);
    getActivity().getSupportFragmentManager().beginTransaction()
        .hide(this)
        .add(R.id.main_timeline_container, statusDetail)
        .commit();
  }

  public boolean hideStatusDetail() {
    if (statusDetail == null) {
      return false;
    }
    getActivity().getSupportFragmentManager().beginTransaction()
        .remove(statusDetail)
        .show(this)
        .commit();
    statusDetail = null;
    return true;
  }

  private boolean stopScroll = false;
  private boolean addedUntilStopped = false;

  public void setStopScroll(boolean isStopScroll) {
    stopScroll = isStopScroll;
  }

  private boolean canScroll() {
    return isVisible()
        && !getTimelineAdapter().isStatusViewSelected()
        && statusDetail == null
        && !stopScroll
        && !isScrolledByUser
        && !addedUntilStopped;
  }

  private void addNewStatuses(List<Status> statuses) {
    getTimelineAdapter().addNewStatuses(statuses);
  }

  private void addStatusesAtLast(List<Status> statuses) {
    getTimelineAdapter().addNewStatusesAtLast(statuses);
  }

  public void scrollToTop() {
    clearSelectedTweet();
    binding.timeline.setLayoutFrozen(false);
    isScrolledByUser = false;
    addedUntilStopped = false;
    scrollTo(0);
  }

  private void scrollTo(int position) {
//    Log.d(TAG, "scrollTo: ");
    binding.timeline.smoothScrollToPosition(position);
  }

  public void clearSelectedTweet() {
    getTimelineAdapter().clearSelectedTweet();
  }

  public boolean isTweetSelected() {
    return getTimelineAdapter().isStatusViewSelected();
  }

  public void setUserIconClickedListener(TimelineAdapter.OnUserIconClickedListener listener) {
    getTimelineAdapter().setOnUserIconClickedListener(listener);
  }

  private void fetchRetweet(final long tweetId) {
    final TimelineAdapter adapter = getTimelineAdapter();
    final View rootView = binding.getRoot();
    twitterApi.retweetStatus(tweetId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            new Action1<Status>() {
              @Override
              public void call(Status status) {
                adapter.addNewStatus(status);
              }
            },
            new Action1<Throwable>() {
              @Override
              public void call(Throwable throwable) {
                SnackbarUtil.show(rootView, "failed to retweet...");
              }
            },
            SnackbarUtil.action(rootView, "success to retweet"));
  }

  private void fetchFavorite(final long tweetId) {
    final TimelineAdapter adapter = getTimelineAdapter();
    final View rootView = binding.getRoot();
    twitterApi.createFavorite(tweetId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            new Action1<Status>() {
              @Override
              public void call(Status status) {
                adapter.addNewStatus(status);
              }
            },
            new Action1<Throwable>() {
              @Override
              public void call(Throwable e) {
                if (e instanceof TwitterException) {
                  final int statusCode = ((TwitterException) e).getStatusCode();
                  if (statusCode == 403) {
                    SnackbarUtil.show(rootView, "already faved");
                    return;
                  }
                }
                Log.e(TAG, "error: ", e);
                SnackbarUtil.show(rootView, "failed to create fav...");
              }
            },
            SnackbarUtil.action(rootView, "success to create fav."));
  }

  protected void fetchTweet() {
    twitterApi.getHomeTimeline()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            new Action1<List<Status>>() {
              @Override
              public void call(List<Status> statuses) {
                addNewStatuses(statuses);
              }
            },
            new Action1<Throwable>() {
              @Override
              public void call(Throwable e) {
                Log.e(TAG, "home timeline is not downloaded.", e);
              }
            });
  }

  protected void fetchTweet(Paging paging) {
    twitterApi.getHomeTimeline(paging)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            new Action1<List<Status>>() {
              @Override
              public void call(List<Status> statuses) {
                Log.d(TAG, "onNext: " + statuses.size());
                addStatusesAtLast(statuses);
              }
            },
            new Action1<Throwable>() {
              @Override
              public void call(Throwable e) {
                Log.e(TAG, "home timeline is not downloaded.", e);
              }
            });
  }

  protected TwitterApi getTwitterApi() {
    return twitterApi;
  }

  protected TimelineAdapter getTimelineAdapter() {
    return tlAdapter;
  }

  @Override
  public void onHiddenChanged(boolean hidden) {
    super.onHiddenChanged(hidden);
    setFabVisibility(hidden);
    if (!hidden) {
      setupOnFlingListener();
    }
  }

  public void setFabVisibility(boolean hidden) {
    final FlingableFAB fab = fabHelper.getFab();
    if (hidden) {
      if (fab.getVisibility() == View.VISIBLE) {
        Log.d(TAG, "setFabVisibility: hide");
        fab.hide();
      }
    } else if (isTweetSelected()) {
      if (fab.getVisibility() != View.VISIBLE) {
        Log.d(TAG, "setFabVisibility: show");
        fab.show();
      }
    }
  }

  private FlingableFABHelper fabHelper;

  public void setFABHelper(FlingableFABHelper flingableFABHelper) {
    this.fabHelper = flingableFABHelper;
  }
}
