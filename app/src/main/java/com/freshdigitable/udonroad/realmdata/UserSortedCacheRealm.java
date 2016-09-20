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

package com.freshdigitable.udonroad.realmdata;

import android.content.Context;
import android.support.annotation.Nullable;

import com.freshdigitable.udonroad.datastore.SortedCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import rx.Observable;
import rx.subjects.PublishSubject;
import twitter4j.PagableResponseList;
import twitter4j.User;

/**
 * UserSortedCacheRealm implements User SortedCache for Realm.

 * Created by akihit on 2016/09/17.
 */
public class UserSortedCacheRealm implements SortedCache<User> {
  private Realm realm;
  private UserCacheRealm userCache;
  private RealmResults<ListedUserIDs> ordered;
  private PublishSubject<Integer> insertEvent;
  private PublishSubject<Integer> deletionEvent;

  @Override
  public void open(Context context, String storeName) {
    insertEvent = PublishSubject.create();
    deletionEvent = PublishSubject.create();
    final RealmConfiguration config = new RealmConfiguration.Builder(context)
        .name(storeName)
        .deleteRealmIfMigrationNeeded()
        .build();
    realm = Realm.getInstance(config);
    userCache = new UserCacheRealm();
    userCache.open(context);
    ordered = realm.where(ListedUserIDs.class)
        .findAllSorted("order");
  }

  @Override
  public void close() {
    userCache.close();
    realm.close();
    insertEvent.onCompleted();
  }

  @Override
  public void clear() {
    realm.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        realm.deleteAll();
      }
    });
  }

  @Override
  public Observable<Integer> observeInsertEvent() {
    return insertEvent.onBackpressureBuffer();
  }

  @Override
  public Observable<Integer> observeUpdateEvent() {
    return null;
  }

  @Override
  public Observable<Integer> observeDeleteEvent() {
    return deletionEvent.onBackpressureBuffer();
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
    if (inserted.isEmpty() || !insertEvent.hasObservers()) {
      return;
    }
    updateCursorList(entities);
    ordered.addChangeListener(new RealmChangeListener<RealmResults<ListedUserIDs>>() {
      @Override
      public void onChange(RealmResults<ListedUserIDs> element) {
        for (ListedUserIDs ids: inserted) {
          insertEvent.onNext(ids.order);
        }
        element.removeChangeListener(this);
      }
    });
  }

  private long lastPageCursor;

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
  public void forceUpsert(User entity) {
    upsert(entity);
  }

  @Nullable
  @Override
  public User find(long id) {
    return userCache.find(id);
  }

  @Override
  public Observable<User> observeById(long id) {
    return userCache.observeById(id);
  }

  @Override
  public void delete(long id) {
    //todo
  }
}
