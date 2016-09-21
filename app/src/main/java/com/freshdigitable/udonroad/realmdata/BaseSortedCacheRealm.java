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
import android.support.annotation.CallSuper;
import android.util.Log;

import com.freshdigitable.udonroad.datastore.SortedCache;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import rx.Observable;
import rx.subjects.PublishSubject;

/**
 * BaseStoredCacheRealm is a base class implementing SortedCache.<br>
 * It provides basic operation for its members.
 *
 * Created by akihit on 2016/09/21.
 */
abstract class BaseSortedCacheRealm<T> implements SortedCache<T> {
  private static final String TAG = BaseSortedCacheRealm.class.getSimpleName();
  protected Realm realm;
  protected PublishSubject<Integer> insertEvent;
  protected PublishSubject<Integer> updateEvent;
  protected PublishSubject<Integer> deleteEvent;

  @Override
  @CallSuper
  public void open(Context context, String storeName) {
    insertEvent = PublishSubject.create();
    updateEvent = PublishSubject.create();
    deleteEvent = PublishSubject.create();
    final RealmConfiguration config = new RealmConfiguration.Builder(context)
        .name(storeName)
        .deleteRealmIfMigrationNeeded()
        .build();
    realm = Realm.getInstance(config);
    Log.d(TAG, "openRealm: " + config.getRealmFileName());
  }

  @Override
  @CallSuper
  public void close() {
    if (realm == null || realm.isClosed()) {
      return;
    }
    Log.d(TAG, "closeRealm: " + realm.getConfiguration().getRealmFileName());
    insertEvent.onCompleted();
    updateEvent.onCompleted();
    deleteEvent.onCompleted();
    realm.close();
  }

  @Override
  public Observable<Integer> observeInsertEvent() {
    return insertEvent.onBackpressureBuffer();
  }

  @Override
  public Observable<Integer> observeUpdateEvent() {
    return updateEvent.onBackpressureBuffer();
  }

  @Override
  public Observable<Integer> observeDeleteEvent() {
    return deleteEvent.onBackpressureBuffer();
  }
}
