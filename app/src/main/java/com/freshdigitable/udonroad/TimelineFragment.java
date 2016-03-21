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
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.freshdigitable.udonroad.databinding.FragmentTimelineBinding;
import com.freshdigitable.udonroad.fab.OnFlingListener;

import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import twitter4j.Paging;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.UserStreamAdapter;
import twitter4j.UserStreamListener;

public class TimelineFragment extends Fragment {
  @SuppressWarnings("unused")
  private static final String TAG = TimelineFragment.class.getSimpleName();
  private FragmentTimelineBinding binding;
  private final TimelineAdapter tlAdapter = new TimelineAdapter();
  private LinearLayoutManager tlLayoutManager;
  private TwitterApi twitterApi;

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
    tlLayoutManager = new LinearLayoutManager(getContext());
    tlLayoutManager.setAutoMeasureEnabled(true);
    binding.timeline.setLayoutManager(tlLayoutManager);
    binding.timeline.setItemAnimator(new TimelineAnimator());
    tlAdapter.setOnSelectedTweetChangeListener(
        new TimelineAdapter.OnSelectedTweetChangeListener() {
          @Override
          public void onTweetSelected(Status status) {
            binding.fab.setVisibility(View.VISIBLE);
          }

          @Override
          public void onTweetUnselected() {
            binding.fab.setVisibility(View.GONE);
          }
        });
    tlAdapter.setLastItemBoundListener(new TimelineAdapter.LastItemBoundListener() {
      @Override
      public void onLastItemBound(long statusId) {
        fetchTweet(new Paging(1, 20, 1, statusId - 1));
      }
    });
    binding.timeline.setAdapter(tlAdapter);

    binding.fab.setVisibility(View.GONE);
    binding.fab.setOnFlingListener(new OnFlingListener() {
      @Override
      public void onFling(Direction direction) {
        if (!isTweetSelected()) {
          return;
        }
        final Status status = tlAdapter.getSelectedStatus();
        final long id = status.getId();
        if (Direction.UP.equals(direction)) {
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
    fetchTweet();
  }

  @Override
  public void onResume() {
    super.onResume();
    twitterApi.connectUserStream(statusListener);
  }

  @Override
  public void onPause() {
    twitterApi.disconnectStreamListener();
    super.onPause();
  }

  private StatusDetailFragment statusDetail;

  private void showStatusDetail(Status status) {
    statusDetail = StatusDetailFragment.getInstance(status);
    statusDetail.setTwitterApi(twitterApi);
    getActivity().getSupportFragmentManager().beginTransaction()
        .hide(this)
        .add(R.id.main_timeline, statusDetail)
        .commit();
    setStopScroll(true);
  }

  public boolean hideStatusDetail() {
    if (statusDetail == null) {
      return false;
    }
    getActivity().getSupportFragmentManager().beginTransaction()
        .remove(statusDetail)
        .show(this)
        .commit();
    setStopScroll(false);
    statusDetail = null;
    return true;
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

  public void setTwitterApi(TwitterApi twitterApi) {
    this.twitterApi = twitterApi;
  }

  public void setUserIconClickedListener(TimelineAdapter.OnUserIconClickedListener listener) {
    tlAdapter.setOnUserIconClickedListener(listener);
  }

  private final UserStreamListener statusListener = new UserStreamAdapter() {
    @Override
    public void onStatus(final Status status) {
      Observable.just(status)
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(new Action1<Status>() {
            @Override
            public void call(Status status) {
              addNewStatus(status);
            }
          });
    }

    @Override
    public void onDeletionNotice(final StatusDeletionNotice statusDeletionNotice) {
      Log.d(TAG, statusDeletionNotice.toString());
      Observable.just(statusDeletionNotice.getStatusId())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(new Action1<Long>() {
            @Override
            public void call(Long deletedStatusId) {
              deleteStatus(deletedStatusId);
            }
          });
    }

    @Override
    public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
      Log.d(TAG, "onTrackLimitationNotice: " + numberOfLimitedStatuses);
    }

    @Override
    public void onScrubGeo(long userId, long upToStatusId) {
      Log.d(TAG, "onScrubGeo: " + userId + ", " + upToStatusId);
    }

    @Override
    public void onStallWarning(StallWarning warning) {
      Log.d(TAG, "onStallWarning: " + warning.toString());
    }

    @Override
    public void onException(Exception ex) {
      Log.d(TAG, "onException: " + ex.toString());
    }
  };

  private void fetchRetweet(final long tweetId) {
    twitterApi.retweetStatus(tweetId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<Status>() {
          @Override
          public void onNext(Status status) {
          }

          @Override
          public void onCompleted() {
            showToast("success to retweet");
          }

          @Override
          public void onError(Throwable e) {
            showToast("failed to retweet...");
          }
        });
  }

  private void fetchFavorite(final long tweetId) {
    twitterApi.createFavorite(tweetId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<Status>() {
          @Override
          public void onNext(Status user) {
          }

          @Override
          public void onCompleted() {
            showToast("success to create fav.");
          }

          @Override
          public void onError(Throwable e) {
            Log.e(TAG, "error: ", e);
            showToast("failed to create fav...");
          }
        });
  }

  private void fetchTweet() {
    twitterApi.getHomeTimeline()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<List<Status>>() {
          @Override
          public void onNext(List<Status> status) {
            addNewStatuses(status);
          }

          @Override
          public void onCompleted() {
          }

          @Override
          public void onError(Throwable e) {
            Log.e(TAG, "home timeline is not downloaded.", e);
          }
        });
  }

  private void fetchTweet(Paging paging) {
    twitterApi.getHomeTimeline(paging)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<List<Status>>() {
          @Override
          public void onNext(List<Status> status) {
            addStatusesAtLast(status);
          }

          @Override
          public void onCompleted() {
          }

          @Override
          public void onError(Throwable e) {
            Log.e(TAG, "home timeline is not downloaded.", e);
          }
        });
  }

  private void showToast(String text) {
    Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show();
  }
}
