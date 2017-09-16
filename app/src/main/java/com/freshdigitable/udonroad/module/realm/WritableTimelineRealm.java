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

import android.util.Log;

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.datastore.WritableSortedCache;
import com.freshdigitable.udonroad.module.twitter.QueryResultList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import io.reactivex.Completable;
import io.realm.RealmResults;
import io.realm.Sort;
import twitter4j.Status;

import static com.freshdigitable.udonroad.module.realm.StatusRealm.KEY_ID;
import static com.freshdigitable.udonroad.module.realm.StatusRealm.KEY_RETWEETED_STATUS_ID;

/**
 * Created by akihit on 2017/07/02.
 */

public class WritableTimelineRealm implements WritableSortedCache<Status> {
  private static final String TAG = WritableTimelineRealm.class.getSimpleName();
  private final TypedCache<Status> pool;
  private final ConfigStore configStore;
  private final NamingBaseCacheRealm sortedCache;

  public WritableTimelineRealm(
      TypedCache<Status> statusCacheRealm, ConfigStore configStore, AppSettingStore appSetting) {
    this.pool = statusCacheRealm;
    this.configStore = configStore;
    sortedCache = new NamingBaseCacheRealm(appSetting);
  }

  @Override
  public void open(String name) {
    pool.open();
    configStore.open();
    sortedCache.open(name);
  }

  @Override
  public void close() {
    sortedCache.close();
    pool.close();
    configStore.close();
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
    observeUpsert(statuses).subscribe();
  }

  @Override
  public Completable observeUpsert(Collection<Status> statuses) {
    if (statuses == null) {
      return Completable.complete();
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
      return Completable.complete();
    }

    return Completable.concatArray(
        pool.observeUpsert(targets),
        Completable.create(e -> {
          sortedCache.executeTransaction(r -> r.insertOrUpdate(statusIDs));
          updatePageCursor(statuses);
          e.onComplete();
        }))
        .doOnError(throwable -> Log.e(TAG, "upsert: ", throwable));
  }

  private void updatePageCursor(Collection<Status> statuses) {
    if (!(statuses instanceof QueryResultList)) {
      return;
    }
    final QueryResultList queryResult = (QueryResultList) statuses;
    final long cursor = queryResult.hasNext() ? queryResult.nextQuery().getMaxId() : -1;
    sortedCache.executeTransaction(r -> {
      final PageCursor nextCursor = new PageCursor(PageCursor.TYPE_NEXT, cursor);
      r.insertOrUpdate(nextCursor);
    });
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
    long[] deleted = new long[res.size()];
    for (int i = 0; i < deleted.length; i++) {
      deleted[i] = res.get(i).getId();
    }

    Completable.create(e -> {
      sortedCache.executeTransaction(r -> res.deleteAllFromRealm());
      e.onComplete();
    }).subscribe(() -> {
      for (long id : deleted) {
        pool.delete(id);
      }
    }, e -> {});
  }

  @Override
  public boolean hasNextPage() {
    final PageCursor nextPageCursor = getNextPageCursor();
    return nextPageCursor != null && nextPageCursor.cursor > 0;
  }

  @Override
  public long getLastPageCursor() {
    final PageCursor nextPage = getNextPageCursor();
    if (nextPage != null) {
      return nextPage.cursor;
    }
    final RealmResults<StatusIDs> timeline = sortedCache.where(StatusIDs.class)
        .findAllSorted(KEY_ID, Sort.DESCENDING);
    return timeline.size() > 0 ?
        timeline.last().getId() - 1
        : -1;
  }

  private PageCursor getNextPageCursor() {
    return sortedCache.where(PageCursor.class)
          .equalTo("type", PageCursor.TYPE_NEXT)
          .findFirst();
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
