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
import com.freshdigitable.udonroad.datastore.UpdateEvent.EventType;
import com.freshdigitable.udonroad.datastore.UpdateSubjectFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;
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
  private final TypedCache<Status> pool;
  private final ConfigStore configStore;
  private final OrderedRealmCollectionChangeListener<RealmResults<StatusIDs>> addChangeListener
      = new OrderedRealmCollectionChangeListener<RealmResults<StatusIDs>>() {
    @Override
    public void onChange(RealmResults<StatusIDs> elem, OrderedCollectionChangeSet changeSet) {
      setItemCount(elem.size());
      if (updateSubject.hasSubscribers()) {
        for (OrderedCollectionChangeSet.Range range : changeSet.getInsertionRanges()) {
          updateSubject.onNext(EventType.INSERT, range.startIndex, range.length);
        }
      }
      elem.removeChangeListener(this);
    }
  };

  public TimelineStoreRealm(UpdateSubjectFactory factory,
                            TypedCache<Status> statusCacheRealm, ConfigStore configStore) {
    super(factory);
    this.pool = statusCacheRealm;
    this.configStore = configStore;
  }

  @Override
  public void open(String storeName) {
    super.open(storeName);
    pool.open();
    configStore.open();
    defaultTimeline();
  }

  private void defaultTimeline() {
    if (timeline != null) {
      return;
    }
    timeline = realm
        .where(StatusIDs.class)
        .findAllSorted(KEY_ID, Sort.DESCENDING);
    setItemCount(timeline.size());
    timeline.addChangeListener(elem -> setItemCount(elem.size()));
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
    pool.observeUpsert(statuses).subscribe(() -> {
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

  private void insertStatus(final List<StatusIDs> inserts) {
    timeline.addChangeListener(addChangeListener);
    realm.executeTransaction(r -> r.insertOrUpdate(inserts));
  }

  private List<StatusIDs> createUpdateList(List<Status> statuses) {
    if (!updateSubject.hasSubscribers()) {
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
      updateSubject.onNext(EventType.CHANGE, i);
    }
  }

  @Override
  public void insert(Status status) {
    pool.insert(status);
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
    timeline.addChangeListener(new OrderedRealmCollectionChangeListener<RealmResults<StatusIDs>>() {
      @Override
      public void onChange(RealmResults<StatusIDs> collection, OrderedCollectionChangeSet changeSet) {
        setItemCount(collection.size());
        if (updateSubject.hasSubscribers()) {
          final OrderedCollectionChangeSet.Range[] deletions = changeSet.getDeletionRanges();
          for (int i = deletions.length - 1; i >= 0; i--) {
            updateSubject.onNext(EventType.DELETE, deletions[i].startIndex, deletions[i].length);
          }
        }
        for (StatusIDs ids : res) {
          pool.delete(ids.getId());
        }
        collection.removeChangeListener(this);
      }
    });
    realm.executeTransaction(r -> res.deleteAllFromRealm());
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
  public int getPositionById(long id) {
    final StatusIDs status = timeline.where().equalTo("id", id).findFirst();
    return timeline.indexOf(status);
  }

  @Override
  public void close() {
    timeline.removeAllChangeListeners();
    timeline = null;
    super.close();
    pool.close();
    configStore.close();
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

  @Override
  public long getLastPageCursor() {
    if (timeline.size() < 1) {
      return -1;
    }
    final StatusIDs lastStatus = timeline.last();
    return lastStatus.getId() - 1;
  }

  @NonNull
  @Override
  public Observable<Status> observeById(long statusId) {
    return pool.observeById(statusId);
  }
}
