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
import android.util.Log;

import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.datastore.TypedCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;
import rx.Observable;
import twitter4j.Status;

import static com.freshdigitable.udonroad.module.realm.StatusRealm.KEY_ID;
import static com.freshdigitable.udonroad.module.realm.StatusRealm.KEY_QUOTAD_STATUS_ID;
import static com.freshdigitable.udonroad.module.realm.StatusRealm.KEY_RETWEETED_STATUS_ID;

/**
 * TimelineStoreRealm implements TimelineStore for Realm.
 *
 * Created by akihit on 2016/07/23.
 */
public class TimelineStoreRealm extends BaseSortedCacheRealm<Status> {
  private static final String TAG = TimelineStoreRealm.class.getSimpleName();
  private RealmResults<StatusIDs> timeline;
  private final TypedCache<Status> statusCache;
  private final ConfigStore configStore;

  public TimelineStoreRealm(TypedCache<Status> statusCacheRealm, ConfigStore configStore) {
    this.statusCache = statusCacheRealm;
    this.configStore = configStore;
  }

  @Override
  public void open(String storeName) {
    super.open(storeName);
    statusCache.open();
    configStore.open();
    defaultTimeline();
  }

  private void defaultTimeline() {
    timeline = realm
        .where(StatusIDs.class)
        .findAllSorted(KEY_ID, Sort.DESCENDING);
    setItemCount(0);
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
  public void upsert(final List<Status> statuses) {
    if (statuses == null) {
      return;
    }
    for (int i = statuses.size() - 1; i >= 0; i--) {
      final Status status = statuses.get(i);
      if (isIgnorable(status)) {
        statuses.remove(status);
      }
    }
    if (statuses.isEmpty()) {
      return;
    }

    final List<StatusIDs> inserts = createInsertList(statuses);
    final List<StatusIDs> updates = createUpdateList(statuses);
    statusCache.observeUpsert(statuses).subscribe(aVoid -> {
      if (!inserts.isEmpty()) {
        insertStatus(inserts);
      }
      if (!updates.isEmpty()) {
        notifyChanged(updates);
      }
    }, throwable -> Log.e(TAG, "upsert: ", throwable));
  }

  private List<StatusIDs> createInsertList(List<Status> statuses) {
    final List<StatusIDs> inserts = new ArrayList<>();
    for (Status s : statuses) {
      final StatusIDs update = timeline.where()
          .beginGroup()
          .equalTo(KEY_ID, s.getId()).or()
          .equalTo(KEY_RETWEETED_STATUS_ID, s.getId()).or()
          .equalTo(KEY_QUOTAD_STATUS_ID, s.getId())
          .endGroup()
          .findFirst();
      if (update == null) {
        inserts.add(new StatusIDs(s));
      }
    }
    return inserts;
  }

  private void insertStatus(List<StatusIDs> inserts) {
    realm.beginTransaction();
    final List<StatusIDs> inserted = realm.copyToRealmOrUpdate(inserts);
    realm.commitTransaction();
    if (inserted.isEmpty() || !insertEvent.hasObservers()) {
      return;
    }
    timeline.addChangeListener(new RealmChangeListener<RealmResults<StatusIDs>>() {
      @Override
      public void onChange(RealmResults<StatusIDs> element) {
        setItemCount(element.size());
        notifyInserted(inserted, element);
        element.removeChangeListener(this);
      }
    });
  }

  private void notifyInserted(List<StatusIDs> inserted, RealmResults<StatusIDs> results) {
    if (inserted.isEmpty()) {
      return;
    }
    final List<Integer> index = searchTimeline(inserted, results);
    if (index.isEmpty()) {
      return;
    }
    Collections.sort(index);
    for (int i : index) {
      insertEvent.onNext(i);
    }
  }

  private List<StatusIDs> createUpdateList(List<Status> statuses) {
    if (!updateEvent.hasObservers()) {
      return Collections.emptyList();
    }

    final List<StatusIDs> updates = new ArrayList<>();
    for (Status s : statuses) {
      final StatusIDs update = findTimeline(s);
      if (update != null) {
        updates.add(update);
      }

      final RealmResults<StatusIDs> u = findReferringStatus(s.getId());
      if (!u.isEmpty()) {
        updates.addAll(u);
      }

      final long quotedStatusId = s.getQuotedStatusId();
      if (quotedStatusId > 0) {
        final Status quotedStatus = s.getQuotedStatus();

        final StatusIDs q = findTimeline(quotedStatus);
        if (q != null) {
          updates.add(q);
        }

        final RealmResults<StatusIDs> updatedQuotedStatus = findReferringStatus(quotedStatusId);
        if (!updatedQuotedStatus.isEmpty()) {
          updates.addAll(updatedQuotedStatus);
        }
      }

      if (!s.isRetweet()) {
        continue;
      }
      final Status retweetedStatus = s.getRetweetedStatus();

      final StatusIDs rs = findTimeline(retweetedStatus);
      if (rs != null) {
        updates.add(rs);
      }
      final RealmResults<StatusIDs> rtedUpdate = findReferringStatus(retweetedStatus.getId());
      if (!rtedUpdate.isEmpty()) {
        updates.addAll(rtedUpdate);
      }

      final long rtQuotedStatusId = retweetedStatus.getQuotedStatusId();
      if (rtQuotedStatusId > 0) {
        final RealmResults<StatusIDs> rtUpdatedQuotedStatus = findReferringStatus(rtQuotedStatusId);
        if (!rtUpdatedQuotedStatus.isEmpty()) {
          updates.addAll(rtUpdatedQuotedStatus);
        }
      }
    }
    return updates;
  }

  private void notifyChanged(List<StatusIDs> changed) {
    if (changed.isEmpty()) {
      return;
    }
    final List<Integer> index = searchTimeline(changed, timeline);
    if (index.isEmpty()) {
      return;
    }
    for (int i : index) {
      updateEvent.onNext(i);
    }
  }

  @Override
  public void insert(Status status) {
    statusCache.insert(status);
    final List<StatusIDs> updates = createUpdateList(Collections.singletonList(status));
    if (!updates.isEmpty()) {
      notifyChanged(updates);
    }
  }

  @NonNull
  private RealmResults<StatusIDs> findReferringStatus(long id) {
    return timeline.where()
        .beginGroup()
        .equalTo(KEY_QUOTAD_STATUS_ID, id)
        .or()
        .equalTo(KEY_RETWEETED_STATUS_ID, id)
        .endGroup()
        .findAll();
  }

  @Nullable
  private StatusIDs findTimeline(Status newer) {
    if (newer == null) {
      return null;
    }
    return timeline.where()
        .equalTo(KEY_ID, newer.getId())
        .findFirst();
  }

  @Override
  public void delete(long statusId) {
    final RealmResults<StatusIDs> res = realm.where(StatusIDs.class)
        .beginGroup()
        .equalTo(KEY_ID, statusId)
        .or()
        .equalTo(KEY_RETWEETED_STATUS_ID, statusId)
        .endGroup()
        .findAll();
    if (res.isEmpty()) {
      return;
    }

    final List<Integer> deleted = searchTimeline(res);

    realm.beginTransaction();
    res.deleteAllFromRealm();
    realm.commitTransaction();

    if (deleted.isEmpty() || !deleteEvent.hasObservers()) {
      return;
    }
    timeline.addChangeListener(new RealmChangeListener<RealmResults<StatusIDs>>() {
      @Override
      public void onChange(RealmResults<StatusIDs> element) {
        Log.d(TAG, "call: deletedStatus");
        setItemCount(element.size());
        for (int d : deleted) {
          deleteEvent.onNext(d);
        }
        for (StatusIDs ids : res) {
          statusCache.delete(ids.getId());
        }
        element.removeChangeListener(this);
      }
    });
  }

  @NonNull
  private List<Integer> searchTimeline(List<StatusIDs> managedItems) {
    return searchTimeline(managedItems, timeline);
  }

  @NonNull
  private List<Integer> searchTimeline(List<StatusIDs> managedItems,
                                       RealmResults<StatusIDs> timeline) {
    final List<Integer> res = new ArrayList<>(managedItems.size());
    for (StatusIDs sr : managedItems) {
      final int index = timeline.indexOf(sr);
      if (index >= 0) {
        res.add(index);
      }
    }
    return res;
  }

  @Override
  public void clear() {
    realm.executeTransaction(_realm -> _realm.deleteAll());
  }

  @Override
  public void clearPool() {
    clear();
    statusCache.clear();
  }

  @Override
  public void close() {
    timeline.removeChangeListeners();
    super.close();
    statusCache.close();
    configStore.close();
  }

  @Override
  public Status get(int position) {
    final StatusIDs ids = timeline.get(position);
    return statusCache.find(ids.getId());
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

  @Override
  public Status find(long statusId) {
    return statusCache.find(statusId);
  }

  @NonNull
  @Override
  public Observable<Status> observeById(long statusId) {
    return statusCache.observeById(statusId);
  }
}
