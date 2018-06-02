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

import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.datastore.StoreManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import timber.log.Timber;

import static com.freshdigitable.udonroad.module.realm.StatusRealm.KEY_ID;
import static com.freshdigitable.udonroad.module.realm.StatusRealm.KEY_RETWEETED_STATUS_ID;

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
  public static final String TAG = "RealmStoreManager";

  @Override
  public void init(Context context) {
    Realm.init(context);
  }

  @Override
  public void cleanUp() {
    for (File d : listDir()) {
      dropCaches(d);
    }
    for (File dir : listDir()) {
      maybeDropPool(dir);
    }
  }

  private static void dropCaches(File dir) {
    for (String name : listDeletableCache(dir)) {
      dropCache(dir, name);
    }
  }

  static void maybeDropPool(File dir) {
    for (String name : listStorage(dir)) {
      for (StoreType t : timelineStore) {
        if (name.startsWith(t.storeName)) {
          Timber.tag(TAG).d("maybeDropPool: %s", name);
          return;
        }
      }
    }
    dropCache(dir, StoreType.POOL.storeName);
  }

  private static void dropCache(File dir, String name) {
    final RealmConfiguration config = new RealmConfiguration.Builder()
        .directory(dir)
        .name(name)
        .deleteRealmIfMigrationNeeded()
        .build();
    final int globalInstanceCount = Realm.getGlobalInstanceCount(config);
    if (globalInstanceCount > 0) {
      final Realm realm = Realm.getInstance(config);
      realm.executeTransaction(r -> r.deleteAll());
      realm.close();
      Timber.tag(TAG).d("deleted: cache> %s/%s", dir.getName(), name);
    } else {
      if (Realm.deleteRealm(config)) {
        Timber.tag(TAG).d("dropped: cache> %s/%s", dir.getName(), name);
      }
    }
  }

  private static String[] listDeletableCache(File dir) {
    List<String> res = new ArrayList<>();
    for (String s : listStorage(dir)) {
      for (StoreType t : deletableCaches) {
        if (s.startsWith(t.storeName)) {
          res.add(s);
        }
      }
    }
    return res.toArray(new String[res.size()]);
  }

  private static String[] listStorage(File dir) {
    // default file location
    final String[] list = dir.list((file, s) -> s.matches("^.*\\.management$"));
    for (int i = 0; i < list.length; i++) {
      list[i] = list[i].replace(".management", "");
    }
    return list;
  }

  private static File[] listDir() {
    final File realmDirectory = new RealmConfiguration.Builder().build().getRealmDirectory();
    return realmDirectory.listFiles(file ->
        file.isDirectory() && file.getName().startsWith(AppSettingStoreRealm.USER_DIR_PREFIX));
  }

  public void delete(long statusId) {
    final File[] files = listDir();
    for (File f : files) {
      final String[] storage = listStorage(f);
      for (String s : storage) {
        final RealmConfiguration config = new RealmConfiguration.Builder()
            .directory(f)
            .name(s)
            .deleteRealmIfMigrationNeeded()
            .build();
        final Realm realm = Realm.getInstance(config);
        realm.executeTransaction(r -> r.where(StatusIDs.class)
            .beginGroup()
            .equalTo(KEY_ID, statusId)
            .or()
            .equalTo(KEY_RETWEETED_STATUS_ID, statusId)
            .endGroup()
            .findAll()
            .deleteAllFromRealm());
        realm.close();
      }
    }
  }
}
