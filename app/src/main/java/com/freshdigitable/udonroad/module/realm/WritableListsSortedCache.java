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

import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.datastore.WritableSortedCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;

import io.reactivex.Completable;
import io.realm.RealmResults;
import twitter4j.PagableResponseList;
import twitter4j.User;
import twitter4j.UserList;

/**
 * Created by akihit on 2017/08/17.
 */

public class WritableListsSortedCache implements WritableSortedCache<UserList> {
  private final TypedCache<User> pool;
  private final NamingBaseCacheRealm sortedCache;

  public WritableListsSortedCache(TypedCache<User> pool) {
    this.pool = pool;
    sortedCache = new NamingBaseCacheRealm();
  }

  @Override
  public void open(String name) {
    pool.open();
    sortedCache.open(name);
  }

  @Override
  public void close() {
    sortedCache.close();
    pool.close();
  }

  @Override
  public long getLastPageCursor() {
    final PageCursor cursor = sortedCache.where(PageCursor.class)
        .equalTo("type", PageCursor.TYPE_NEXT)
        .findFirst();
    return cursor != null ? cursor.cursor : -1;
  }

  @Override
  public boolean hasNextPage() {
    return getLastPageCursor() > 0;
  }

  @Override
  public Completable observeUpsert(Collection<UserList> entities) {
    if (entities == null || entities.isEmpty()) {
      return Completable.complete();
    }
    final RealmResults<UserListRealm> lists = sortedCache.where(UserListRealm.class)
        .findAllSorted("order");
    int order = lists.size() > 0 ? lists.last().getOrder() + 1 : 0;
    final ArrayList<UserListRealm> inserts = new ArrayList<>(entities.size());
    for (UserList list : entities) {
      final UserListRealm registered = sortedCache.where(UserListRealm.class)
          .equalTo("id", list.getId())
          .findFirst();
      if (registered == null) {
        inserts.add(new UserListRealm(list, order));
        order++;
      }
    }
    final Collection<User> users = splitUsers(entities);
    return Completable.concatArray(pool.observeUpsert(users),
        Completable.create(e -> {
          sortedCache.executeTransaction(r -> r.insertOrUpdate(inserts));
          updateLastCursor(entities);
          e.onComplete();
        }));
  }

  @Override
  public void upsert(UserList entity) {
    if (entity == null) {
      return;
    }
    upsert(Collections.singleton(entity));
  }

  @Override
  public void upsert(Collection<UserList> entities) {
    observeUpsert(entities).subscribe();
  }

  private Collection<User> splitUsers(Collection<UserList> entities) {
    final LinkedHashMap<Long, User> res = new LinkedHashMap<>();
    for (UserList entity : entities) {
      res.put(entity.getUser().getId(), entity.getUser());
    }
    return res.values();
  }

  private void updateLastCursor(Collection<UserList> entities) {
    if (!(entities instanceof PagableResponseList)) {
      return;
    }
    final PagableResponseList pagableList = (PagableResponseList) entities;
    sortedCache.executeTransaction(r -> {
      final PageCursor pageCursor = new PageCursor(PageCursor.TYPE_NEXT, pagableList.getNextCursor());
      r.insertOrUpdate(pageCursor);
    });
  }

  @Override
  public void insert(UserList entity) {}

  @Override
  public void delete(long id) {}

  @Override
  public void clear() {
    sortedCache.clear();
  }

  @Override
  public void drop() {
    sortedCache.drop();
  }
}
