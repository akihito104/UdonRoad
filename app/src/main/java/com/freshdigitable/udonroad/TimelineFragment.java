/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */
package com.freshdigitable.udonroad;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.freshdigitable.udonroad.TimelineAdapter.OnUserIconClickedListener;
import com.freshdigitable.udonroad.databinding.FragmentTimelineBinding;
import com.freshdigitable.udonroad.datastore.TimelineStore;
import com.freshdigitable.udonroad.ffab.FlingableFAB;
import com.freshdigitable.udonroad.ffab.FlingableFABHelper;
import com.freshdigitable.udonroad.ffab.OnFlingAdapter;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.TwitterException;

public class TimelineFragment extends Fragment {
  @SuppressWarnings("unused")
  private static final String TAG = TimelineFragment.class.getSimpleName();
  private FragmentTimelineBinding binding;
  private TimelineAdapter tlAdapter;
  private LinearLayoutManager tlLayoutManager;
  @Inject
  TwitterApi twitterApi;
  @Inject
  TimelineStore timelineStore;
  private Subscription insertEventSubscription;
  private Subscription updateEventSubscription;
  private Subscription deleteEventSubscription;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    InjectionUtil.getComponent(this).inject(this);
    tlAdapter = new TimelineAdapter(timelineStore);
    timelineStore.open(context, getStoreName());
    timelineStore.clear();
    insertEventSubscription = timelineStore.subscribeInsertEvent()
        .subscribe(new Action1<Integer>() {
          @Override
          public void call(Integer position) {
            tlAdapter.notifyItemInserted(position);
          }
        });
    updateEventSubscription = timelineStore.subscribeUpdateEvent()
        .subscribe(new Action1<Integer>() {
          @Override
          public void call(Integer position) {
            tlAdapter.notifyItemChanged(position);
          }
        });
    deleteEventSubscription = timelineStore.subscribeDeleteEvent()
        .subscribe(new Action1<Integer>() {
          @Override
          public void call(Integer position) {
            tlAdapter.notifyItemRemoved(position);
          }
        });
  }

  @Override
  public void onDetach() {
    insertEventSubscription.unsubscribe();
    updateEventSubscription.unsubscribe();
    deleteEventSubscription.unsubscribe();
    timelineStore.clear();
    timelineStore.close();
    super.onDetach();
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
    setupOnFlingListener();
  }

  public void setupOnFlingListener() {
    fabHelper.getFab().setOnFlingListener(new OnFlingAdapter() {
      @Override
      public void onFling(Direction direction) {
        if (!isTweetSelected()) {
          return;
        }
        final long id = tlAdapter.getSelectedTweetId();
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
    clearSelectedTweet();
    tlAdapter.unregisterAdapterDataObserver(itemInsertedObserver);
    tlAdapter.unregisterAdapterDataObserver(createdAtObserver);
    tearDownOnFlingListener();
  }

  @Override
  public void onDestroyView() {
    tlAdapter.setLastItemBoundListener(null);
    tlAdapter.setOnSelectedTweetChangeListener(null);
    tlAdapter.setOnUserIconClickedListener(null);
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
        && !tlAdapter.isStatusViewSelected()
        && statusDetail == null
        && !stopScroll
        && !isScrolledByUser
        && !addedUntilStopped;
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
    tlAdapter.clearSelectedTweet();
  }

  public boolean isTweetSelected() {
    return tlAdapter.isStatusViewSelected();
  }

  private OnUserIconClickedListener userIconClickedListener;

  public void setUserIconClickedListener(OnUserIconClickedListener listener) {
    this.userIconClickedListener = listener;
  }

  private void fetchRetweet(final long tweetId) {
    final View rootView = binding.getRoot();
    twitterApi.retweetStatus(tweetId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            upsertAction(),
            new Action1<Throwable>() {
              @Override
              public void call(Throwable throwable) {
                SnackbarUtil.show(rootView, "failed to retweet...");
              }
            },
            SnackbarUtil.action(rootView, "success to retweet"));
  }

  private void fetchFavorite(final long tweetId) {
    final View rootView = binding.getRoot();
    twitterApi.createFavorite(tweetId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            upsertAction(),
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
            listUpsertAction(),
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
            listUpsertAction(),
            new Action1<Throwable>() {
              @Override
              public void call(Throwable e) {
                Log.e(TAG, "home timeline is not downloaded.", e);
              }
            });
  }

  @NonNull
  private Action1<Status> upsertAction() {
    return new Action1<Status>() {
      @Override
      public void call(Status status) {
        timelineStore.upsert(status);
      }
    };
  }

  @NonNull
  private Action1<List<Status>> listUpsertAction() {
    return new Action1<List<Status>>() {
      @Override
      public void call(List<Status> statuses) {
        timelineStore.upsert(statuses);
      }
    };
  }

  protected TwitterApi getTwitterApi() {
    return twitterApi;
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

  public String getStoreName() {
    return "home";
  }

  protected void fetchTweet(final Observable.OnSubscribe<List<Status>> onSubscribe) {
    Observable.create(onSubscribe)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            listUpsertAction(),
            new Action1<Throwable>() {
              @Override
              public void call(Throwable throwable) {
                Log.e(TAG, "fetch: ", throwable);
              }
            });
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
}
