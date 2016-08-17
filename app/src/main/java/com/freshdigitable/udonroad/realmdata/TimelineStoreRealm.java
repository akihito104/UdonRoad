/*
 * Copyright (c) 2016. Akihito Matsuda (akihito104)
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.freshdigitable.udonroad.datastore.TimelineStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.Sort;
import rx.Observable;
import rx.subjects.PublishSubject;
import twitter4j.Status;

import static com.freshdigitable.udonroad.realmdata.StatusRealm.KEY_ID;
import static com.freshdigitable.udonroad.realmdata.StatusRealm.KEY_QUOTAD_STATUS_ID;
import static com.freshdigitable.udonroad.realmdata.StatusRealm.KEY_RETWEETED_STATUS_ID;

/**
 * Created by akihit on 2016/07/23.
 */
public class TimelineStoreRealm implements TimelineStore {
  public static final String TAG = TimelineStoreRealm.class.getSimpleName();
  private Realm realm;
  private RealmResults<StatusIDs> timeline;
  private StatusCacheRealm statusCache;
  private PublishSubject<Integer> insertEvent;
  private PublishSubject<Integer> updateEvent;
  private PublishSubject<Integer> deleteEvent;

  public void open(Context context) {
    final RealmConfiguration config = new RealmConfiguration.Builder(context).build();
    open(context, config);
  }

  @Override
  public void open(Context context, String storeName) {
    final RealmConfiguration config = new RealmConfiguration.Builder(context)
        .name(storeName)
        .deleteRealmIfMigrationNeeded()
        .build();
    open(context, config);
  }

  private void open(Context context, RealmConfiguration config) {
    Log.d(TAG, "openRealm: " + config.getRealmFileName());
    insertEvent = PublishSubject.create();
    updateEvent = PublishSubject.create();
    deleteEvent = PublishSubject.create();
    statusCache = new StatusCacheRealm();
    statusCache.open(context);
    realm = Realm.getInstance(config);
    defaultTimeline();
  }

  private void defaultTimeline() {
    timeline = realm
        .where(StatusIDs.class)
        .findAllSorted(KEY_ID, Sort.DESCENDING);
  }

  @Override
  public Observable<Integer> subscribeInsertEvent() {
    return insertEvent.onBackpressureBuffer();
  }

  @Override
  public Observable<Integer> subscribeUpdateEvent() {
    return updateEvent.onBackpressureBuffer();
  }

  @Override
  public Observable<Integer> subscribeDeleteEvent() {
    return deleteEvent.onBackpressureBuffer();
  }

  @Override
  public void upsert(Status status) {
    if (status == null) {
      return;
    }
    upsert(Collections.singletonList(status));
  }

  @Override
  public void upsert(List<Status> statuses) {
    if (statuses == null || statuses.isEmpty()) {
      return;
    }
    statusCache.upsert(statuses);
    final List<StatusIDs> inserts = new ArrayList<>();
    final List<StatusIDs> updates = new ArrayList<>();
    for (Status s : statuses) {
      final StatusIDs update = findTimeline(s);
      if (update == null) {
        inserts.add(new StatusIDs(s));
      } else {
        updates.add(new StatusIDs(s));
      }

      final RealmResults<StatusIDs> u = findReferringStatus(s.getId());
      if (u.size() > 0) {
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
        if (updatedQuotedStatus.size() > 0) {
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
      if (rtedUpdate.size() > 0) {
        updates.addAll(rtedUpdate);
      }

      final long rtQuotedStatusId = retweetedStatus.getQuotedStatusId();
      if (rtQuotedStatusId > 0) {
        final RealmResults<StatusIDs> rtUpdatedQuotedStatus = findReferringStatus(rtQuotedStatusId);
        if (rtUpdatedQuotedStatus.size() > 0) {
          updates.addAll(rtUpdatedQuotedStatus);
        }
      }
    }

    if (inserts.size() > 0) {
      insertStatus(inserts);
    }
    if (updates.size() > 0) {
      updateStatus(updates);
    }
  }

  @Override
  public void deleteStatus(long statusId) {
    delete(statusId);
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

  private void insertStatus(List<StatusIDs> inserts) {
    realm.beginTransaction();
    final List<StatusIDs> inserted = realm.copyToRealmOrUpdate(inserts);
    realm.commitTransaction();
    timeline.addChangeListener(new RealmChangeListener<RealmResults<StatusIDs>>() {
      @Override
      public void onChange(RealmResults<StatusIDs> element) {
        notifyInserted(inserted, element);
        element.removeChangeListener(this);
      }
    });
  }

  private void notifyInserted(List<StatusIDs> inserted, RealmResults<StatusIDs> results) {
    if (inserted.size() < 1) {
      return;
    }
    final List<Integer> index = searchTimeline(inserted, results);
    if (index.size() < 1) {
      return;
    }
    Collections.sort(index);
    for (int i : index) {
      insertEvent.onNext(i);
    }
  }

  private void updateStatus(List<StatusIDs> updates) {
    realm.beginTransaction();
    final List<StatusIDs> updated = realm.copyToRealmOrUpdate(updates);
    realm.commitTransaction();
    timeline.addChangeListener(new RealmChangeListener<RealmResults<StatusIDs>>() {
      @Override
      public void onChange(RealmResults<StatusIDs> element) {
        notifyChanged(updated, element);
        element.removeChangeListener(this);
      }
    });
  }

  private void notifyChanged(List<StatusIDs> changed, RealmResults<StatusIDs> results) {
    if (changed.size() < 1) {
      return;
    }
    final List<Integer> index = searchTimeline(changed, results);
    if (index.size() < 1) {
      return;
    }
    for (int i : index) {
      updateEvent.onNext(i);
    }
  }

  private void delete(long statusId) {
    final RealmResults<StatusIDs> res = realm.where(StatusIDs.class)
        .beginGroup()
        .equalTo(KEY_ID, statusId)
        .or()
        .equalTo(KEY_RETWEETED_STATUS_ID, statusId)
        .endGroup()
        .findAll();
    if (res.size() < 1) {
      return;
    }

    final List<Integer> deleted = searchTimeline(res);

    realm.beginTransaction();
    res.deleteAllFromRealm();
    realm.commitTransaction();

    if (deleted.size() <= 0) {
      return;
    }
    timeline.addChangeListener(new RealmChangeListener<RealmResults<StatusIDs>>() {
      @Override
      public void onChange(RealmResults<StatusIDs> element) {
        Log.d(TAG, "call: deletedStatus");
        for (int d : deleted) {
          deleteEvent.onNext(d);
          statusCache.deleteStatus(d);
        }
        element.removeChangeListener(this);
      }
    });
  }

  @NonNull
  private List<Integer> searchTimeline(List<StatusIDs> items) {
    return searchTimeline(items, timeline);
  }

  @NonNull
  private List<Integer> searchTimeline(List<StatusIDs> items, RealmResults<StatusIDs> timeline) {
    final List<Integer> res = new ArrayList<>(items.size());
    for (StatusIDs sr : items) {
      final int index = timeline.indexOf(sr);
      if (index >= 0) {
        res.add(index);
      }
    }
    return res;
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
  public void close() {
    if (realm == null || realm.isClosed()) {
      return;
    }
    Log.d(TAG, "closeRealm: " + realm.getConfiguration().getRealmFileName());
    insertEvent.onCompleted();
    updateEvent.onCompleted();
    deleteEvent.onCompleted();
    timeline.removeChangeListeners();
    realm.close();
    statusCache.close();
  }

  @Override
  public Status get(int position) {
    final StatusIDs ids = timeline.get(position);
    return statusCache.getStatus(ids.getId());
  }

  @Override
  public int getItemCount() {
    return timeline.size();
  }

  @Override
  public Status findStatus(long statusId) {
    return statusCache.getStatus(statusId);
  }
}
