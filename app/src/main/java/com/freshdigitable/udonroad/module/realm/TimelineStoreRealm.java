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

package com.freshdigitable.udonroad.module.realm;

import android.support.annotation.NonNull;
import android.util.Log;

import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.datastore.SortedCache;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.datastore.UpdateEvent;
import com.freshdigitable.udonroad.datastore.UpdateEvent.EventType;
import com.freshdigitable.udonroad.datastore.UpdateSubject;
import com.freshdigitable.udonroad.datastore.UpdateSubjectFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;
import twitter4j.Status;

import static com.freshdigitable.udonroad.module.realm.StatusRealm.KEY_ID;
import static com.freshdigitable.udonroad.module.realm.StatusRealm.KEY_RETWEETED_STATUS_ID;

/**
 * TimelineStoreRealm implements TimelineStore for Realm.
 *
 * Created by akihit on 2016/07/23.
 */
public class TimelineStoreRealm implements SortedCache<Status> {
  private static final String TAG = TimelineStoreRealm.class.getSimpleName();
  private RealmResults<StatusIDs> timeline;
  private final TypedCache<Status> pool;
  private final ConfigStore configStore;
  private final UpdateSubjectFactory factory;
  private UpdateSubject updateSubject;

  private final OrderedRealmCollectionChangeListener<RealmResults<StatusIDs>> addChangeListener
      = new OrderedRealmCollectionChangeListener<RealmResults<StatusIDs>>() {
    @Override
    public void onChange(RealmResults<StatusIDs> elem, OrderedCollectionChangeSet changeSet) {
      setItemCount(elem.size());
      if (updateSubject.hasSubscribers()) {
        for (OrderedCollectionChangeSet.Range range : changeSet.getInsertionRanges()) {
          updateSubject.onNext(EventType.INSERT, range.startIndex, range.length);
        }
        for (OrderedCollectionChangeSet.Range range : changeSet.getChangeRanges()) {
          updateSubject.onNext(EventType.CHANGE, range.startIndex, range.length);
        }
      }
      elem.removeChangeListener(this);
    }
  };
  private final NamingBaseCacheRealm sortedCache;

  public TimelineStoreRealm(UpdateSubjectFactory factory,
                            TypedCache<Status> statusCacheRealm, ConfigStore configStore) {
    this.factory = factory;
    this.pool = statusCacheRealm;
    this.configStore = configStore;
    sortedCache = new NamingBaseCacheRealm();
  }

  @Override
  public void open(String storeName) {
    if (sortedCache.isOpened()) {
      throw new IllegalStateException(storeName + " has already opened...");
    }
    updateSubject = factory.getInstance(storeName);
    pool.open();
    configStore.open();
    sortedCache.open(storeName);
    defaultTimeline();
  }

  @Override
  public void close() {
    timeline.removeAllChangeListeners();
    sortedCache.close();
    configStore.close();
    pool.close();
    updateSubject.onComplete();
  }

  @Override
  public void drop() {
    sortedCache.drop();
  }

  @Override
  public void clear() {
    sortedCache.clear();
  }

  private void defaultTimeline() {
    timeline = sortedCache.where(StatusIDs.class)
        .findAllSorted(KEY_ID, Sort.DESCENDING);
    setItemCount(timeline.size());
    timeline.addChangeListener(elem -> setItemCount(elem.size()));
  }

  @Override
  public void upsert(Status status) {
    if (status == null || isIgnorable(status)) {
      return;
    }
    upsert(Collections.singletonList(status));
  }

  private boolean isIgnorable(Status status) {
    return configStore.isIgnoredUser(status.getUser().getId())
        || (status.isRetweet() && configStore.isIgnoredUser(status.getRetweetedStatus().getUser().getId()));
  }

  @Override
  public void upsert(final Collection<Status> statuses) {
    if (statuses == null) {
      return;
    }
    final ArrayList<Status> targets = new ArrayList<>(statuses.size());
    final ArrayList<StatusIDs> statusIDs = new ArrayList<>(statuses.size());
    for (Status s : statuses) {
      if (isIgnorable(s)) {
        continue;
      }
      targets.add(s);
      statusIDs.add(new StatusIDs(s));
    }
    if (targets.isEmpty()) {
      return;
    }

    pool.observeUpsert(targets).subscribe(() -> {
      timeline.addChangeListener(addChangeListener);
      sortedCache.executeTransaction(r -> r.insertOrUpdate(statusIDs));
    }, throwable -> Log.e(TAG, "upsert: ", throwable));
  }

  @Override
  public void insert(Status status) {
    upsert(status);
  }

  @Override
  public void delete(long statusId) {
    final RealmResults<StatusIDs> res = sortedCache.where(StatusIDs.class)
        .beginGroup()
        .equalTo(KEY_ID, statusId)
        .or()
        .equalTo(KEY_RETWEETED_STATUS_ID, statusId)
        .endGroup()
        .findAll();
    if (res.isEmpty()) {
      return;
    }
    timeline.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<StatusIDs>>() {
      @Override
      public void onChange(RealmResults<StatusIDs> collection, OrderedCollectionChangeSet changeSet) {
        setItemCount(collection.size());
        if (updateSubject.hasSubscribers()) {
          final OrderedCollectionChangeSet.Range[] deletions = changeSet.getDeletionRanges();
          for (int i = deletions.length - 1; i >= 0; i--) {
            updateSubject.onNext(EventType.DELETE, deletions[i].startIndex, deletions[i].length);
          }
        }
        for (StatusIDs ids : res) {
          pool.delete(ids.getId());
        }
        collection.removeChangeListener(this);
      }
    });
    sortedCache.executeTransaction(r -> res.deleteAllFromRealm());
  }

  @Override
  public int getPositionById(long id) {
    final StatusIDs status = timeline.where().equalTo("id", id).findFirst();
    return timeline.indexOf(status);
  }

  @Override
  public Flowable<UpdateEvent> observeUpdateEvent() {
    return updateSubject.observeUpdateEvent();
  }

  @Override
  public Status get(int position) {
    final StatusIDs ids = timeline.get(position);
    return pool.find(ids.getId());
  }

  @Override
  public synchronized int getItemCount() {
    return itemCount;
  }

  private volatile int itemCount;

  private synchronized void setItemCount(int count) {
    itemCount = count;
  }

  @Override
  public long getLastPageCursor() {
    if (timeline.size() < 1) {
      return -1;
    }
    final StatusIDs lastStatus = timeline.last();
    return lastStatus.getId() - 1;
  }

  @NonNull
  @Override
  public Observable<Status> observeById(long statusId) {
    return pool.observeById(statusId);
  }
}
