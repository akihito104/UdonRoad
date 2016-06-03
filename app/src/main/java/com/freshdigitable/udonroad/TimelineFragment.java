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
import com.freshdigitable.udonroad.fab.OnFlingListener;
import com.freshdigitable.udonroad.realmdata.RealmTimelineAdapter;

import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import twitter4j.Paging;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.TwitterException;
import twitter4j.User;
import twitter4j.UserStreamAdapter;
import twitter4j.UserStreamListener;

public class TimelineFragment extends Fragment {
  @SuppressWarnings("unused")
  private static final String TAG = TimelineFragment.class.getSimpleName();
  private FragmentTimelineBinding binding;
  private final RealmTimelineAdapter tlAdapter = new RealmTimelineAdapter();
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

    tlAdapter.setOnSelectedTweetChangeListener(
        new TimelineAdapter.OnSelectedTweetChangeListener() {
          @Override
          public void onTweetSelected(Status status) {
            binding.fab.show();
          }

          @Override
          public void onTweetUnselected() {
            binding.fab.hide();
          }
        });
    tlAdapter.setLastItemBoundListener(new TimelineAdapter.LastItemBoundListener() {
      @Override
      public void onLastItemBound(long statusId) {
        fetchTweet(new Paging(1, 20, 1, statusId - 1));
      }
    });
    binding.timeline.setAdapter(tlAdapter);

    binding.fab.hide();
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
    binding.fabIndicator.setImageResource(R.drawable.ic_add_white_36dp);
    binding.fab.setActionIndicator(binding.fabIndicator);
  }

  private TwitterStreamApi streamApi;
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
    tlAdapter.registerAdapterDataObserver(itemInsertedObserver);
    tlAdapter.openRealm(getContext());
    twitterApi = TwitterApi.setup(getContext());
    fetchTweet();
    streamApi = TwitterStreamApi.setup(getContext());
    if (streamApi == null) {
      return;
    }
    streamApi.connectUserStream(statusListener);
//    Log.d(TAG, "onStart: Realm.path: " + realm.getPath());
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
    streamApi.disconnectStreamListener();
    tlAdapter.closeRealm();
    tlAdapter.unregisterAdapterDataObserver(itemInsertedObserver);
    super.onStop();
  }

  private StatusDetailFragment statusDetail;

  private void showStatusDetail(Status status) {
    statusDetail = StatusDetailFragment.getInstance(status);
    statusDetail.setTwitterApi(twitterApi);
    getActivity().getSupportFragmentManager().beginTransaction()
        .hide(this)
        .add(R.id.main_timeline, statusDetail)
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
    return !tlAdapter.isStatusViewSelected()
        && statusDetail == null
        && !stopScroll
        && !isScrolledByUser
        && !addedUntilStopped;
  }

  private void addNewStatus(Status status) {
    tlAdapter.addNewStatus(status);
  }

  private void addNewStatuses(List<Status> statuses) {
    tlAdapter.addNewStatuses(statuses);
  }

  private void addStatusesAtLast(List<Status> statuses) {
    tlAdapter.addNewStatusesAtLast(statuses);
  }

  private void deleteStatus(long id) {
    tlAdapter.deleteStatus(id);
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
    tlAdapter.clearSelectedTweet();
  }

  public boolean isTweetSelected() {
    return tlAdapter.isStatusViewSelected();
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
            Log.d(TAG, "onNext: " + status.size());
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

  private static final int TIMELINE_DEFAULT = 0;
  private static final int TIMELINE_USER = 1;

  private int timelineMode = TIMELINE_DEFAULT;

  public boolean onBackPressed() {
    if (isTweetSelected()) {
      clearSelectedTweet();
      return true;
    }
    if (timelineMode == TIMELINE_USER) {
      tlAdapter.defaultTimeline();
      timelineMode = TIMELINE_DEFAULT;
      return true;
    }
    return false;
  }

  public void showDefaultTimeline() {
    tlAdapter.defaultTimeline();
    timelineMode = TIMELINE_DEFAULT;
  }

  public void showUserTimeline(final User user) {
    twitterApi.getUserTimeline(user)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(new Action1<List<Status>>() {
          @Override
          public void call(List<Status> statuses) {
            tlAdapter.addNewStatuses(statuses);
          }
        })
        .doOnCompleted(new Action0() {
          @Override
          public void call() {
            tlAdapter.userTimeline(user);
            timelineMode = TIMELINE_USER;
          }
        })
        .doOnError(new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            Toast.makeText(getContext(), "failed to download...", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "showUserTimeline: ", throwable);
          }
        })
        .subscribe();
  }
}
