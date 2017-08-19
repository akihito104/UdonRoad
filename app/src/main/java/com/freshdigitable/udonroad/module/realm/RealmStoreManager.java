/*
 * Copyright (c) 2017. Matsuda, Akihit (akihito104)
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

import android.content.Context;
import android.util.Log;

import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.datastore.StoreManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by akihit on 2017/07/04.
 */

public class RealmStoreManager implements StoreManager {

  private static final List<StoreType> deletableCaches
      = Arrays.asList(StoreType.HOME, StoreType.CONVERSATION, StoreType.POOL,
      StoreType.USER_FAV, StoreType.USER_FOLLOWER, StoreType.USER_FRIEND, StoreType.USER_HOME,
      StoreType.SEARCH, StoreType.OWNED_LIST, StoreType.USER_LIST);
  private static final List<StoreType> timelineStore
      = Arrays.asList(StoreType.HOME, StoreType.CONVERSATION,
      StoreType.USER_FAV, StoreType.USER_FOLLOWER, StoreType.USER_FRIEND, StoreType.USER_HOME,
      StoreType.SEARCH, StoreType.OWNED_LIST, StoreType.USER_LIST);

  @Override
  public void init(Context context) {
    Realm.init(context);
    dropCaches();
  }

  @Override
  public void cleanUp() {
    maybeDropPool();
  }

  static void maybeDropPool() {
    for (String name : listStorage()) {
      for (StoreType t : timelineStore) {
        if (name.startsWith(t.storeName)) {
          return;
        }
      }
    }
    Log.d("RealmStoreManager", "maybeDropPool: cache");
    dropCache(StoreType.POOL.storeName);
  }

  private static void dropCaches() {
    for (String name : listDeletableCache()) {
      dropCache(name);
    }
  }

  private static void dropCache(String name) {
    final RealmConfiguration config = new RealmConfiguration.Builder()
        .name(name)
        .deleteRealmIfMigrationNeeded()
        .build();
    final int globalInstanceCount = Realm.getGlobalInstanceCount(config);
    if (globalInstanceCount > 0) {
      final Realm realm = Realm.getInstance(config);
      realm.executeTransaction(r -> r.deleteAll());
      realm.close();
    } else {
      Realm.deleteRealm(config);
    }
  }

  private static String[] listDeletableCache() {
    List<String> res = new ArrayList<>();
    for (String s : listStorage()) {
      for (StoreType t : deletableCaches) {
        if (s.startsWith(t.storeName)) {
          res.add(s);
        }
      }
    }
    return res.toArray(new String[res.size()]);
  }

  private static String[] listStorage() {
    // default file location
    final String[] list = new RealmConfiguration.Builder().build().getRealmDirectory()
        .list((file, s) -> s.matches("^.*\\.management$"));
    for (int i = 0; i < list.length; i++) {
      list[i] = list[i].replace(".management", "");
    }
    return list;
  }
}
