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
import android.support.annotation.Nullable;
import android.util.Log;

import com.freshdigitable.udonroad.datastore.BaseCache;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmModel;

/**
 * BaseCacheRealm provides basic operation for `cache` database.
 *
 * Created by akihit on 2016/09/14.
 */
abstract class BaseCacheRealm implements BaseCache {
  private static final String TAG = BaseCacheRealm.class.getSimpleName();
  Realm cache;
  private final RealmConfiguration config;
  private BaseCacheRealm baseCacheRealm;

  BaseCacheRealm() {
    this(null);
  }

  BaseCacheRealm(@Nullable BaseCacheRealm baseCacheRealm) {
    if (baseCacheRealm != null) {
      this.baseCacheRealm = baseCacheRealm;
      cache = baseCacheRealm.cache;
      config = null;
    } else {
      config = new RealmConfiguration.Builder()
          .name("cache")
          .deleteRealmIfMigrationNeeded()
          .build();
    }
  }

  @Override
  @CallSuper
  public void open() {
    if (baseCacheRealm != null) {
      return;
    }
    Log.d(TAG, "open");
    cache = Realm.getInstance(config);
  }

  @Override
  public void clear() {
    cache.executeTransaction(realm -> realm.deleteAll());
  }

  @Override
  @CallSuper
  public void close() {
    if (baseCacheRealm != null) {
      return;
    }
    if (cache == null || cache.isClosed()) {
      return;
    }
    Log.d(TAG, "close: " + cache.getConfiguration().getRealmFileName());
    cache.close();
  }

  @Override
  public void drop() {
    if (config != null) {
      Realm.deleteRealm(config);
    }
  }

  static <T extends RealmModel> T findById(Realm realm, long id, Class<T> clz) {
    return realm.where(clz)
        .equalTo("id", id)
        .findFirst();
  }
}
