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
import android.util.Log;

import com.freshdigitable.udonroad.datastore.BaseCache;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * BaseCacheRealm provides basic operation for `cache` database.
 *
 * Created by akihit on 2016/09/14.
 */
abstract class BaseCacheRealm implements BaseCache {
  private static final String TAG = BaseCacheRealm.class.getSimpleName();
  protected Realm cache;

  @Override
  public void open(Context context) {
    Log.d(TAG, "StatusCacheRealm: open");
    final RealmConfiguration config = new RealmConfiguration.Builder(context)
        .name("cache")
        .deleteRealmIfMigrationNeeded()
        .build();
    cache = Realm.getInstance(config);
  }

  @Override
  public void clear() {
    cache.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        cache.deleteAll();
      }
    });
  }

  @Override
  public void close() {
    if (cache == null || cache.isClosed()) {
      return;
    }
    Log.d(TAG, "close: " + cache.getConfiguration().getRealmFileName());
    cache.close();
  }
}