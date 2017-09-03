/*
 * Copyright (c) 2017. Matsuda, Akihit (akihito104)
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

import javax.inject.Inject;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.RealmResults;
import twitter4j.User;
import twitter4j.UserList;

/**
 * Created by akihit on 2017/08/17.
 */

public class ListsSortedCache implements SortedCache<UserList> {
  private final TypedCache<User> pool;
  private final NamingBaseCacheRealm sortedCache;
  private RealmResults<UserListRealm> userLists;
  private final UpdateSubjectFactory factory;
  private UpdateSubject updateSubject;

  @Inject
  public ListsSortedCache(UpdateSubjectFactory factory, TypedCache<User> pool) {
    this.factory = factory;
    this.pool = pool;
    sortedCache = new NamingBaseCacheRealm();
  }


  @Override
  public void open(String name) {
    pool.open();
    sortedCache.open(name);
    updateSubject = factory.getInstance(name);
    userLists = sortedCache.where(UserListRealm.class)
        .findAllSorted("order");
    userLists.addChangeListener(realmChangeListener);
  }

  private final OrderedRealmCollectionChangeListener<RealmResults<UserListRealm>> realmChangeListener
      = new OrderedRealmCollectionChangeListener<RealmResults<UserListRealm>>() {
    @Override
    public void onChange(RealmResults<UserListRealm> element, OrderedCollectionChangeSet changeSet) {
      if (updateSubject.hasSubscribers()) {
        final OrderedCollectionChangeSet.Range[] insertions = changeSet.getInsertionRanges();
        for (OrderedCollectionChangeSet.Range i : insertions) {
          updateSubject.onNext(EventType.INSERT, i.startIndex, i.length);
        }
      }
    }
  };

  @Override
  public void clear() {
    sortedCache.clear();
  }

  @Override
  public void close() {
    userLists.removeChangeListener(realmChangeListener);
    updateSubject.onComplete();
    sortedCache.close();
    pool.close();
  }

  @Override
  public void drop() {
    sortedCache.drop();
  }

  @Override
  public long getId(int position) {
    return userLists.get(position).getId();
  }

  @Override
  public UserList get(int position) {
    final UserListRealm res = userLists.get(position);
    final User user = pool.find(res.getUserId());
    res.setUser(user);
    return res;
  }

  @Override
  public int getItemCount() {
    return userLists.size();
  }

  @Override
  public int getPositionById(long id) {
    final UserListRealm userList = sortedCache.where(UserListRealm.class)
        .equalTo("id", id)
        .findFirst();
    return userList != null ? userLists.indexOf(userList) : -1;
  }

  @Override
  public Flowable<UpdateEvent> observeUpdateEvent() {
    return updateSubject.observeUpdateEvent();
  }

  @NonNull
  @Override
  public Observable<? extends UserList> observeById(long id) {
    return Observable.empty();
  }

  @NonNull
  @Override
  public Observable<? extends UserList> observeById(UserList element) {
    return Observable.empty();
  }
}
