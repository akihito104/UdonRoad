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
import android.support.annotation.Nullable;

import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.datastore.UpdateEvent.EventType;
import com.freshdigitable.udonroad.datastore.UpdateSubjectFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import rx.Observable;
import twitter4j.PagableResponseList;
import twitter4j.User;

/**
 * UserSortedCacheRealm implements User SortedCache for Realm.

 * Created by akihit on 2016/09/17.
 */
public class UserSortedCacheRealm extends BaseSortedCacheRealm<User> {
  private TypedCache<User> userCache;
  private RealmResults<ListedUserIDs> ordered;

  public UserSortedCacheRealm(UpdateSubjectFactory factory, TypedCache<User> userCacheRealm) {
    super(factory);
    this.userCache = userCacheRealm;
  }

  @Override
  public void open(String storeName) {
    super.open(storeName);
    userCache.open();
    ordered = realm.where(ListedUserIDs.class)
        .findAllSorted("order");
  }

  @Override
  public void close() {
    ordered.removeAllChangeListeners();
    super.close();
    userCache.close();
  }

  @Override
  public void clear() {
    realm.executeTransaction(_realm -> _realm.deleteAll());
  }

  @Override
  public void clearPool() {
    // nop
  }

  @Override
  public User get(int position) {
    final ListedUserIDs userIDs = ordered.get(position);
    return userCache.find(userIDs.userId);
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
  public void upsert(List<User> entities) {
    if (entities == null || entities.isEmpty()) {
      return;
    }
    userCache.upsert(entities);
    final List<ListedUserIDs> inserts = new ArrayList<>();
    for (User user: entities) {
      final ListedUserIDs userIds = realm.where(ListedUserIDs.class)
          .equalTo("userId", user.getId())
          .findFirst();
      if (userIds == null) {
        inserts.add(new ListedUserIDs(user, order));
        order++;
      }
    }
    realm.beginTransaction();
    final List<ListedUserIDs> inserted = realm.copyToRealmOrUpdate(inserts);
    realm.commitTransaction();
    if (inserted.isEmpty() || !updateSubject.hasObservers()) {
      return;
    }
    updateCursorList(entities);
    ordered.addChangeListener(new RealmChangeListener<RealmResults<ListedUserIDs>>() {
      @Override
      public void onChange(RealmResults<ListedUserIDs> element) {
        for (ListedUserIDs ids: inserted) {
          updateSubject.onNext(EventType.INSERT, ids.order);
        }
        element.removeChangeListener(this);
      }
    });
  }

  private long lastPageCursor = -1;

  @Override
  public long getLastPageCursor() {
    return lastPageCursor;
  }

  private void updateCursorList(List<User> users) {
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

  @Nullable
  @Override
  public User find(long id) {
    return userCache.find(id);
  }

  @NonNull
  @Override
  public Observable<User> observeById(long id) {
    return userCache.observeById(id);
  }

  @Override
  public void delete(long id) {
    //todo
  }
}
