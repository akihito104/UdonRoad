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
import com.freshdigitable.udonroad.datastore.WritableSortedCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.RealmResults;
import twitter4j.PagableResponseList;
import twitter4j.User;

/**
 * UserSortedCacheRealm implements User SortedCache for Realm.

 * Created by akihit on 2016/09/17.
 */
public class UserSortedCacheRealm implements SortedCache<User>, WritableSortedCache<User> {
  private final NamingBaseCacheRealm sortedCache;
  private TypedCache<User> pool;
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
  public User get(int position) {
    final ListedUserIDs userIDs = ordered.get(position);
    return pool.find(userIDs.userId);
  }

  @Override
  public int getItemCount() {
    return ordered.size();
  }

  @Override
  public void upsert(User entity) {
    if (entity == null) {
      return;
    }
    upsert(Collections.singletonList(entity));
  }

  private int order = 0;

  @Override
  public void upsert(Collection<User> entities) {
    if (entities == null || entities.isEmpty()) {
      return;
    }
    pool.upsert(entities);
    final List<ListedUserIDs> inserts = new ArrayList<>();
    for (User user: entities) {
      final ListedUserIDs userIds = sortedCache.where(ListedUserIDs.class)
          .equalTo("userId", user.getId())
          .findFirst();
      if (userIds == null) {
        inserts.add(new ListedUserIDs(user, order));
        order++;
      }
    }
    ordered.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<ListedUserIDs>>() {
      @Override
      public void onChange(RealmResults<ListedUserIDs> element, OrderedCollectionChangeSet changeSet) {
        if (updateSubject.hasSubscribers()) {
          final OrderedCollectionChangeSet.Range[] insertions = changeSet.getInsertionRanges();
          for (OrderedCollectionChangeSet.Range i : insertions) {
            updateSubject.onNext(EventType.INSERT, i.startIndex, i.length);
          }
        }
        element.removeChangeListener(this);
      }
    });
    sortedCache.executeTransaction(r -> r.insertOrUpdate(inserts));
    updateCursorList(entities);
  }

  private long lastPageCursor = -1;

  @Override
  public long getLastPageCursor() {
    return lastPageCursor;
  }

  private void updateCursorList(Collection<User> users) {
    if (!(users instanceof PagableResponseList)) {
      return;
    }
    final PagableResponseList page = (PagableResponseList) users;
    lastPageCursor = page.getNextCursor();
  }

  @Override
  public void insert(User entity) {
    upsert(entity);
  }

  @NonNull
  @Override
  public Observable<User> observeById(long id) {
    return pool.observeById(id);
  }

  @Override
  public void delete(long id) {
    //todo
  }
}
