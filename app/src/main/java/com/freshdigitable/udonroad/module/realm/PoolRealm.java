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

import com.freshdigitable.udonroad.datastore.BaseCache;

import io.reactivex.Completable;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmModel;

/**
 * PoolRealm provides basic operation for `cache` database.
 *
 * Created by akihit on 2016/09/14.
 */
final class PoolRealm implements BaseCache {
  private static final String TAG = PoolRealm.class.getSimpleName();
  private Realm cache;
  private final RealmConfiguration config;

  PoolRealm() {
    config = new RealmConfiguration.Builder()
        .name("cache")
        .deleteRealmIfMigrationNeeded()
        .build();
  }

  @Override
  @CallSuper
  public void open() {
    Log.d(TAG, "open: " + config.getRealmFileName());
    cache = Realm.getInstance(config);
  }

  @Override
  public void clear() {
    cache.executeTransaction(realm -> realm.deleteAll());
  }

  @Override
  @CallSuper
  public void close() {
    if (cache == null || cache.isClosed()) {
      return;
    }
    Log.d(TAG, "close: " + config.getRealmFileName());
    cache.close();
  }

  @Override
  public void drop() {
    if (Realm.getGlobalInstanceCount(config) <= 0) {
      Log.d(TAG, "drop: " + config.getRealmFileName());
      Realm.deleteRealm(config);
    }
  }

  void executeTransaction(Realm.Transaction transaction) {
    cache.executeTransaction(transaction);
  }

  void executeTransactionAsync(Realm.Transaction transaction) {
    cache.executeTransactionAsync(transaction);
  }

  <T extends RealmModel> T findById(long id, Class<T> clz) {
    return CacheUtil.findById(cache, id, clz);
  }

  Completable observeUpsertImpl(Realm.Transaction transaction) {
    return CacheUtil.observeUpsertImpl(cache, transaction);
  }
}
