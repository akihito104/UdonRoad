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
import twitter4j.User;

/**
 * UserSortedCacheRealm implements User SortedCache for Realm.

 * Created by akihit on 2016/09/17.
 */
public class UserSortedCacheRealm implements SortedCache<User> {
  private final NamingBaseCacheRealm sortedCache;
  private final TypedCache<User> pool;
  private RealmResults<ListedUserIDs> ordered;
  private final UpdateSubjectFactory factory;
  private UpdateSubject updateSubject;

  public UserSortedCacheRealm(UpdateSubjectFactory factory, TypedCache<User> userCacheRealm) {
    this.factory = factory;
    this.pool = userCacheRealm;
    this.sortedCache = new NamingBaseCacheRealm();
  }

  @Override
  public void open(String storeName) {
    if (sortedCache.isOpened()) {
      throw new IllegalStateException(storeName + " has already opened...");
    }
    updateSubject = factory.getInstance(storeName);
    pool.open();
    sortedCache.open(storeName);
    ordered = sortedCache.where(ListedUserIDs.class)
        .findAllSorted("order");
    ordered.addChangeListener(realmChangeListener);
  }

  @Override
  public void close() {
    ordered.removeAllChangeListeners();
    sortedCache.close();
    pool.close();
    updateSubject.onComplete();
  }

  @Override
  public void clear() {
    sortedCache.clear();
  }

  @Override
  public void drop() {
    sortedCache.drop();
  }

  @Override
  public int getPositionById(long id) {
    final ListedUserIDs status = ordered.where().equalTo("id", id).findFirst();
    return ordered.indexOf(status);
  }

  @Override
  public Flowable<UpdateEvent> observeUpdateEvent() {
    return updateSubject.observeUpdateEvent();
  }

  @Override
  public long getId(int position) {
    return ordered.get(position).userId;
  }

  @Override
  public User get(int position) {
    final ListedUserIDs userIDs = ordered.get(position);
    return pool.find(userIDs.userId);
  }

  @Override
  public int getItemCount() {
    return ordered.size();
  }

  @Override
  public long getLastPageCursor() {
    final PageCursor cursor = sortedCache.where(PageCursor.class)
        .equalTo("type", PageCursor.TYPE_NEXT)
        .findFirst();
    return cursor != null ?
        cursor.cursor
        : -1;
  }

  @NonNull
  @Override
  public Observable<? extends User> observeById(long id) {
    return pool.observeById(id);
  }

  private final OrderedRealmCollectionChangeListener<RealmResults<ListedUserIDs>> realmChangeListener
      = new OrderedRealmCollectionChangeListener<RealmResults<ListedUserIDs>>() {
    @Override
    public void onChange(RealmResults<ListedUserIDs> element, OrderedCollectionChangeSet changeSet) {
      if (updateSubject.hasSubscribers()) {
        final OrderedCollectionChangeSet.Range[] insertions = changeSet.getInsertionRanges();
        for (OrderedCollectionChangeSet.Range i : insertions) {
          updateSubject.onNext(EventType.INSERT, i.startIndex, i.length);
        }
      }
    }
  };
}
