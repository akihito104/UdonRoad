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

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.SortedCache;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.datastore.UpdateEvent;
import com.freshdigitable.udonroad.datastore.UpdateEvent.EventType;
import com.freshdigitable.udonroad.datastore.UpdateSubject;
import com.freshdigitable.udonroad.datastore.UpdateSubjectFactory;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;
import twitter4j.Status;

import static com.freshdigitable.udonroad.module.realm.StatusRealm.KEY_ID;

/**
 * TimelineStoreRealm implements TimelineStore for Realm.
 *
 * Created by akihit on 2016/07/23.
 */
public class TimelineStoreRealm implements SortedCache<Status> {
//  private static final String TAG = TimelineStoreRealm.class.getSimpleName();
  private RealmResults<StatusIDs> timeline;
  private final TypedCache<Status> pool;
  private final UpdateSubjectFactory factory;
  private UpdateSubject updateSubject;

  private final OrderedRealmCollectionChangeListener<RealmResults<StatusIDs>> addChangeListener
      = new OrderedRealmCollectionChangeListener<RealmResults<StatusIDs>>() {
    @Override
    public void onChange(@NonNull RealmResults<StatusIDs> elem, @NonNull OrderedCollectionChangeSet changeSet) {
      setItemCount(elem.size());
      if (updateSubject.hasSubscribers()) {
        for (OrderedCollectionChangeSet.Range range : changeSet.getInsertionRanges()) {
          updateSubject.onNext(EventType.INSERT, range.startIndex, range.length);
        }
        for (OrderedCollectionChangeSet.Range range : changeSet.getChangeRanges()) {
          updateSubject.onNext(EventType.CHANGE, range.startIndex, range.length);
        }
        final OrderedCollectionChangeSet.Range[] deletions = changeSet.getDeletionRanges();
        for (int i = deletions.length - 1; i >= 0; i--) {
          updateSubject.onNext(EventType.DELETE, deletions[i].startIndex, deletions[i].length);
        }
      }
    }
  };
  private final NamingBaseCacheRealm sortedCache;

  public TimelineStoreRealm(UpdateSubjectFactory factory,
                            TypedCache<Status> statusCacheRealm, AppSettingStore appSetting) {
    this.factory = factory;
    this.pool = statusCacheRealm;
    sortedCache = new NamingBaseCacheRealm(appSetting);
  }

  @Override
  public void open(String storeName) {
    if (sortedCache.isOpened()) {
      throw new IllegalStateException(storeName + " has already opened...");
    }
    updateSubject = factory.getInstance(storeName);
    pool.open();
    sortedCache.open(storeName);
    defaultTimeline();
  }

  @Override
  public void close() {
    timeline.removeAllChangeListeners();
    sortedCache.close();
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
//    timeline.addChangeListener(elem -> setItemCount(elem.size()));
    timeline.addChangeListener(addChangeListener);
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
  public long getId(int position) {
    return timeline.get(position).getId();
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

  @NonNull
  @Override
  public Observable<? extends Status> observeById(long statusId) {
    return pool.observeById(statusId);
  }

  @NonNull
  @Override
  public Observable<? extends Status> observeById(Status element) {
    return pool.observeById(element);
  }
}
