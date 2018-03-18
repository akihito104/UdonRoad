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

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.datastore.WritableSortedCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.reactivex.Completable;
import io.realm.RealmResults;
import twitter4j.PagableResponseList;
import twitter4j.User;

/**
 * Created by akihit on 2017/07/02.
 */

public class WritableUserSortedCacheRealm implements WritableSortedCache<User> {
  private final NamingBaseCacheRealm sortedCache;
  private final TypedCache<User> pool;

  public WritableUserSortedCacheRealm(TypedCache<User> pool, AppSettingStore appSetting) {
    sortedCache = new NamingBaseCacheRealm(appSetting);
    this.pool = pool;
  }

  @Override
  public void open(String storeName) {
    pool.open();
    sortedCache.open(storeName);
  }

  @Override
  public void close() {
    sortedCache.close();
    pool.close();
  }

  @Override
  public void upsert(User entity) {
    if (entity == null) {
      return;
    }
    upsert(Collections.singletonList(entity));
  }

  @Override
  public void upsert(Collection<User> entities) {
    observeUpsert(entities).subscribe();
  }

  @Override
  public Completable observeUpsert(Collection<User> entities) {
    if (entities == null || entities.isEmpty()) {
      return Completable.complete();
    }
    final RealmResults<ListedUserIDs> users = sortedCache.where(ListedUserIDs.class)
        .sort("order")
        .findAll();
    final ListedUserIDs last = !users.isEmpty() ? users.last() : null;
    int order = last != null ? last.order + 1 : 0;
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
    return Completable.concatArray(
        pool.observeUpsert(entities),
        Completable.create(e -> {
          sortedCache.executeTransaction(r -> r.insertOrUpdate(inserts));
          updateCursorList(entities);
          e.onComplete();
        }));
  }

  private void updateCursorList(Collection<User> users) {
    if (!(users instanceof PagableResponseList)) {
      return;
    }
    final PagableResponseList page = (PagableResponseList) users;
    sortedCache.executeTransaction(r -> {
      final PageCursor cursor = new PageCursor(PageCursor.TYPE_NEXT, page.getNextCursor());
      r.insertOrUpdate(cursor);
    });
  }

  @Override
  public boolean hasNextPage() {
    final PageCursor nextPageCursor = getNextPageCursor();
    return nextPageCursor != null && nextPageCursor.cursor > 0;
  }

  @Override
  public long getLastPageCursor() {
    final PageCursor cursor = getNextPageCursor();
    return cursor != null ?
        cursor.cursor
        : -1;
  }

  private PageCursor getNextPageCursor() {
    return sortedCache.where(PageCursor.class)
          .equalTo("type", PageCursor.TYPE_NEXT)
          .findFirst();
  }

  @Override
  public void insert(User entity) {
    upsert(entity);
  }

  @Override
  public void delete(long id) {
    //todo
  }

  @Override
  public void clear() {
    sortedCache.clear();
  }

  @Override
  public void drop() {
    sortedCache.drop();
  }
}
