/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.datastore;

import android.content.Context;

import java.util.List;

import rx.Observable;
import twitter4j.Status;

/**
 * Created by akihit on 2016/07/25.
 */
public interface TimelineStore {
  void open(Context context, String storeName);

  void close();

  void clear();

  Observable<Integer> subscribeInsertEvent();

  Observable<Integer> subscribeUpdateEvent();

  Observable<Integer> subscribeDeleteEvent();

  void upsert(List<Status> statuses);

  void delete(long statusId);

  Status get(int position);

  int getItemCount();
}
