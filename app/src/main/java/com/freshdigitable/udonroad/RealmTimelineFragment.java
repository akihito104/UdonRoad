/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.freshdigitable.udonroad.datastore.TimelineStore;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import twitter4j.Paging;
import twitter4j.Status;

/**
 * Created by akihit on 2016/06/06.
 */
public abstract class RealmTimelineFragment extends TimelineFragment {
  private static final String TAG = RealmTimelineFragment.class.getSimpleName();

  @Inject
  TimelineStore timelineStore;
  private Subscription insertEventSubscription;
  private Subscription updateEventSubscription;
  private Subscription deleteEventSubscription;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    InjectionUtil.getComponent(this).inject(this);
    timelineStore.open(context, getStoreName());
    timelineStore.clear();
    final TimelineAdapter timelineAdapter = getTimelineAdapter();
    timelineAdapter.setTimelineStore(timelineStore);
    insertEventSubscription = timelineStore.subscribeInsertEvent()
        .subscribe(new Action1<Integer>() {
          @Override
          public void call(Integer position) {
            timelineAdapter.notifyItemInserted(position);
          }
        });
    updateEventSubscription = timelineStore.subscribeUpdateEvent()
        .subscribe(new Action1<Integer>() {
          @Override
          public void call(Integer position) {
            timelineAdapter.notifyItemChanged(position);
          }
        });
    deleteEventSubscription = timelineStore.subscribeDeleteEvent()
        .subscribe(new Action1<Integer>() {
          @Override
          public void call(Integer position) {
            timelineAdapter.notifyItemRemoved(position);
          }
        });
  }

  @Override
  public void onStart() {
    super.onStart();
  }

  @Override
  public void onStop() {
    Log.d(TAG, "onStop: ");
    clearSelectedTweet();
    super.onStop();
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

  public abstract String getStoreName();

  protected abstract void fetchTweet();

  protected abstract void fetchTweet(final Paging page);

  protected void fetchTweet(final Observable.OnSubscribe<List<Status>> onSubscribe) {
    Observable.create(onSubscribe)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            new Action1<List<Status>>() {
              @Override
              public void call(List<Status> statuses) {
                timelineStore.upsert(statuses);
              }
            },
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
