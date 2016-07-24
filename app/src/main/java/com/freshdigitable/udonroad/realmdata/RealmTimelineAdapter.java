/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.realmdata;

import android.content.Context;
import android.util.Log;

import com.freshdigitable.udonroad.TimelineAdapter;

import java.util.Collections;
import java.util.List;

import rx.Subscription;
import rx.functions.Action1;
import twitter4j.Status;

/**
 * RecyclerView adapter for RealmResult
 *
 * Created by akihit on 2016/05/04.
 */
public class RealmTimelineAdapter extends TimelineAdapter {
  private static final String TAG = RealmTimelineAdapter.class.getSimpleName();
  private TimelineStore timelineStore;
  private Subscription insertEventSubscription;
  private Subscription updateEventSubscription;
  private Subscription deleteEventSubscription;

  public void openRealm(Context context, String config) {
    Log.d(TAG, "openRealm: ");
    timelineStore = new TimelineStore();
    timelineStore.open(context, config);
    insertEventSubscription = timelineStore.subscribeInsertEvent()
        .subscribe(new Action1<Integer>() {
          @Override
          public void call(Integer position) {
            notifyItemInserted(position);
          }
        });
    updateEventSubscription = timelineStore.subscribeUpdateEvent()
        .subscribe(new Action1<Integer>() {
          @Override
          public void call(Integer position) {
            notifyItemChanged(position);
          }
        });
    deleteEventSubscription = timelineStore.subscribeDeleteEvent()
        .subscribe(new Action1<Integer>() {
          @Override
          public void call(Integer position) {
            notifyItemRemoved(position);
          }
        });
  }

  public void closeRealm() {
    Log.d(TAG, "closeRealm: ");
    clearSelectedTweet();
    insertEventSubscription.unsubscribe();
    updateEventSubscription.unsubscribe();
    deleteEventSubscription.unsubscribe();
    timelineStore.close();
  }

  @Override
  public void addNewStatus(Status status) {
    addNewStatuses(Collections.singletonList(status));
  }

  @Override
  public void addNewStatuses(List<Status> statuses) {
    timelineStore.upsert(statuses);
  }

  @Override
  public void addNewStatusesAtLast(List<Status> statuses) {
    addNewStatuses(statuses);
  }

  @Override
  public void deleteStatus(long statusId) {
    timelineStore.delete(statusId);
  }

  @Override
  protected Status get(int position) {
    return timelineStore.get(position);
  }

  @Override
  public int getItemCount() {
    return timelineStore.getItemCount();
  }
}
