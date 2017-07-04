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

  @Override
  @CallSuper
  public void open(String storeName) {
    config = getRealmConfiguration(storeName);
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
  }

  @Override
  public void drop() {
    if (config == null) {
      return;
    }
    if (Realm.getGlobalInstanceCount(config) <= 0) {
      Log.d(TAG, "drop: " + config.getRealmFileName());
      Realm.deleteRealm(config);
      RealmStoreManager.maybeDropPool();
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
