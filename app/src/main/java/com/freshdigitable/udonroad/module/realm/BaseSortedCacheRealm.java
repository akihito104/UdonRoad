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

import android.support.annotation.CallSuper;
import android.util.Log;

import com.freshdigitable.udonroad.datastore.SortedCache;
import com.freshdigitable.udonroad.datastore.UpdateEvent;
import com.freshdigitable.udonroad.datastore.UpdateSubject;
import com.freshdigitable.udonroad.datastore.UpdateSubjectFactory;

import io.reactivex.Flowable;
import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * BaseStoredCacheRealm is a base class implementing SortedCache.<br>
 * It provides basic operation for its members.
 *
 * Created by akihit on 2016/09/21.
 */
abstract class BaseSortedCacheRealm<T> implements SortedCache<T> {
  private static final String TAG = BaseSortedCacheRealm.class.getSimpleName();
  private final UpdateSubjectFactory factory;
  Realm realm;
  UpdateSubject updateSubject;

  BaseSortedCacheRealm(UpdateSubjectFactory factory) {
    this.factory = factory;
  }

  @Override
  @CallSuper
  public void open(String storeName) {
    updateSubject = factory.getInstance(storeName);
    final RealmConfiguration config = getRealmConfiguration(storeName);
    realm = Realm.getInstance(config);
    Log.d(TAG, "openRealm: " + config.getRealmFileName());
  }

  private RealmConfiguration getRealmConfiguration(String storeName) {
    return new RealmConfiguration.Builder()
          .name(storeName)
          .deleteRealmIfMigrationNeeded()
          .build();
  }

  @Override
  @CallSuper
  public void close() {
    if (realm == null || realm.isClosed()) {
      return;
    }
    Log.d(TAG, "closeRealm: " + realm.getConfiguration().getRealmFileName());
    realm.close();
    if (realm.isClosed()) {
      updateSubject.onComplete();
    }
  }

  @Override
  public void drop(String storeName) {
    final RealmConfiguration conf = getRealmConfiguration(storeName);
    if (Realm.getGlobalInstanceCount(conf) <= 0) {
      Log.d(TAG, "drop: " + storeName);
      Realm.deleteRealm(conf);
    }
  }

  @Override
  public Flowable<UpdateEvent> observeUpdateEvent() {
    return updateSubject.observeUpdateEvent();
  }

  @Override
  public void open() {
    throw new RuntimeException();
  }

  @Override
  public void drop() {
    throw new RuntimeException();
  }
}
