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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.freshdigitable.udonroad.databinding.FragmentTimelineBinding;
import com.freshdigitable.udonroad.fab.FlingableFloatingActionButton;
import com.freshdigitable.udonroad.fab.OnFlingListener;
import com.freshdigitable.udonroad.realmdata.RealmTimelineAdapter;

import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.TwitterException;

public class TimelineFragment extends Fragment {
  @SuppressWarnings("unused")
  private static final String TAG = TimelineFragment.class.getSimpleName();
  private FragmentTimelineBinding binding;
  private final TimelineAdapter tlAdapter = new RealmTimelineAdapter();
  private LinearLayoutManager tlLayoutManager;
  private TwitterApi twitterApi;

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
    adapter.setOnSelectedTweetChangeListener(
        new TimelineAdapter.OnSelectedTweetChangeListener() {
          @Override
          public void onTweetSelected(Status status) {
            fab.getFab().show();
          }

          @Override
          public void onTweetUnselected() {
            fab.getFab().hide();
          }
        });
    adapter.setLastItemBoundListener(new TimelineAdapter.LastItemBoundListener() {
      @Override
      public void onLastItemBound(long statusId) {
        fetchTweet(new Paging(1, 20, 1, statusId - 1));
      }
    });
    binding.timeline.setAdapter(adapter);

    fab.setOnFlingListener(new OnFlingListener() {
      @Override
      public void onFling(Direction direction) {
        if (!isTweetSelected()) {
          return;
        }
        final Status status = adapter.getSelectedStatus();
        final long id = status.getId();
        if (OnFlingListener.Direction.UP.equals(direction)) {
          fetchFavorite(id);
        } else if (Direction.RIGHT.equals(direction)) {
          fetchRetweet(id);
        } else if (Direction.UP_RIGHT.equals(direction)) {
          fetchFavorite(id);
          fetchRetweet(id);
        } else if (Direction.LEFT.equals(direction)) {
          showStatusDetail(status);
        }
      }
    });
  }

  private final RecyclerView.AdapterDataObserver itemInsertedObserver
      = new RecyclerView.AdapterDataObserver() {
    @Override
    public void onItemRangeInserted(int positionStart, int itemCount) {
      super.onItemRangeInserted(positionStart, itemCount);
      if (canScroll()) {
        Log.d(TAG, "onItemRangeInserted: ");
        scrollTo(0);
      } else {
        addedUntilStopped = true;
      }
    }
  };

  @Override
  public void onStart() {
    super.onStart();
    getTimelineAdapter().registerAdapterDataObserver(itemInsertedObserver);
    twitterApi = TwitterApi.setup(getContext());
    fetchTweet();
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
    super.onStop();
    Log.d(TAG, "onStop: ");
    getTimelineAdapter().unregisterAdapterDataObserver(itemInsertedObserver);
  }

  private StatusDetailFragment statusDetail;

  private void showStatusDetail(Status status) {
    statusDetail = StatusDetailFragment.getInstance(status);
    statusDetail.setTwitterApi(twitterApi);
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
    Log.d(TAG, "scrollTo: ");
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

  private static final Action1<Status> emptyAction = new Action1<Status>() {
    @Override
    public void call(Status status) {}
  };

  private void fetchRetweet(final long tweetId) {
    twitterApi.retweetStatus(tweetId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(emptyAction,
            new Action1<Throwable>() {
              @Override
              public void call(Throwable throwable) {
                showToast("failed to retweet...");
              }
            },
            new Action0() {
              @Override
              public void call() {
                showToast("success to retweet");
              }
            });
  }

  private void fetchFavorite(final long tweetId) {
    twitterApi.createFavorite(tweetId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(emptyAction,
            new Action1<Throwable>() {
              @Override
              public void call(Throwable e) {
                if (e instanceof TwitterException) {
                  final int statusCode = ((TwitterException) e).getStatusCode();
                  if (statusCode == 403) {
                    showToast("already faved");
                    return;
                  }
                }
                Log.e(TAG, "error: ", e);
                showToast("failed to create fav...");
              }
            },
            new Action0() {
              @Override
              public void call() {
                showToast("success to create fav.");
              }
            });
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

  private void showToast(String text) {
    Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show();
  }

  private FlingableFloatingActionButton fab;

  public void setFAB(FlingableFloatingActionButton fab) {
    this.fab = fab;
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
  }

  public void setFabVisibility(boolean hidden) {
    if (hidden) {
      if (fab.getFab().getVisibility() == View.VISIBLE) {
        Log.d(TAG, "setFabVisibility: hide");
        fab.getFab().hide();
      }
    } else if (isTweetSelected()) {
      if (fab.getFab().getVisibility() != View.VISIBLE) {
        Log.d(TAG, "setFabVisibility: show");
        fab.getFab().show();
      }
    }
  }
}
