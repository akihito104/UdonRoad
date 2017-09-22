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

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.NamingBaseCache;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmModel;
import io.realm.RealmQuery;

/**
 * BaseStoredCacheRealm is a base class implementing SortedCache.<br>
 * It provides basic operation for its members.
 *
 * Created by akihit on 2016/09/21.
 */
class NamingBaseCacheRealm implements NamingBaseCache {
  private static final String TAG = NamingBaseCacheRealm.class.getSimpleName();
  private Realm realm;
  private RealmConfiguration config;
  private AppSettingStore appSettingStore;

  NamingBaseCacheRealm(AppSettingStore appSettingStore) {
    this.appSettingStore = appSettingStore;
  }

  @Override
  @CallSuper
  public void open(String storeName) {
    appSettingStore.open();
    config = getRealmConfiguration(storeName);
    realm = Realm.getInstance(config);
    Log.d(TAG, "open: " + config.getRealmFileName());
    appSettingStore.close();
  }

  private RealmConfiguration getRealmConfiguration(String storeName) {
    return new RealmConfiguration.Builder()
        .directory(appSettingStore.getCurrentUserDir())
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
    Log.d(TAG, "close: " + config.getRealmDirectory() + "/" + config.getRealmFileName());
    realm.close();
  }

  @Override
  public void drop() {
    if (config == null) {
      return;
    }
    if (Realm.getGlobalInstanceCount(config) <= 0) {
      final boolean dropped = Realm.deleteRealm(config);
      if (dropped) {
        Log.d(TAG, "drop: " + config.getRealmDirectory() + "/" + config.getRealmFileName());
        RealmStoreManager.maybeDropPool(config.getRealmDirectory());
      }
    }
  }

  @Override
  public void clear() {
    realm.executeTransaction(_realm -> _realm.deleteAll());
  }

  boolean isOpened() {
    return realm != null && !realm.isClosed();
  }

  void executeTransaction(Realm.Transaction transaction) {
    realm.executeTransaction(transaction);
  }

  <T extends RealmModel> RealmQuery<T> where(Class<T> clz) {
    return realm.where(clz);
  }
}
