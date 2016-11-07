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

import android.content.SharedPreferences;

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.TypedCache;

import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import twitter4j.TwitterAPIConfiguration;
import twitter4j.User;

import static com.freshdigitable.udonroad.subscriber.ConfigRequestWorker.TWITTER_API_CONFIG_DATE;

/**
 * Created by akihit on 2016/11/07.
 */

public class AppSettingStoreRealm implements AppSettingStore {
  private Realm realm;
  private final TypedCache<User> cache;
  private final RealmConfiguration config;
  private final SharedPreferences prefs;

  public AppSettingStoreRealm(TypedCache<User> userCacheRealm, SharedPreferences prefs) {
    this.cache = userCacheRealm;
    config = new RealmConfiguration.Builder()
        .name("appSettings")
        .deleteRealmIfMigrationNeeded()
        .build();
    this.prefs = prefs;
  }

  @Override
  public void open() {
    cache.open();
    realm = Realm.getInstance(config);
  }

  @Override
  public void close() {
    cache.close();
    realm.close();
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
  public void addAuthenticatedUser(final User authenticatedUser) {
    realm.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        final UserRealm userRealm = new UserRealm(authenticatedUser);
        realm.insertOrUpdate(userRealm);
      }
    });
    cache.upsert(authenticatedUser);
  }

  @Override
  public User getAuthenticatedUser(long userId) {
    final UserRealm user = realm.where(UserRealm.class)
        .equalTo("id", userId)
        .findFirst();
    if (user == null) {
      return null;
    }
    final User cacheUser = cache.find(user.getId());
    return cacheUser != null
        ? cacheUser
        : user;
  }

  @Override
  public void setTwitterAPIConfig(final TwitterAPIConfiguration twitterAPIConfig) {
    realm.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        final TwitterAPIConfigurationRealm twitterAPIConfiguration
            = new TwitterAPIConfigurationRealm(twitterAPIConfig);
        realm.insertOrUpdate(twitterAPIConfiguration);
      }
    });
    setFetchTwitterAPIConfigTime(System.currentTimeMillis());
  }

  @Override
  public TwitterAPIConfiguration getTwitterAPIConfig() {
    return realm.where(TwitterAPIConfigurationRealm.class)
        .findFirst();
  }

  private void setFetchTwitterAPIConfigTime(long timestamp) {
    prefs.edit()
        .putLong(TWITTER_API_CONFIG_DATE, timestamp)
        .apply();
  }

  private long getFetchTwitterAPIConfigTime() {
    return prefs.getLong(TWITTER_API_CONFIG_DATE, -1);
  }

  @Override
  public boolean isTwitterAPIConfigFetchable() {
    final TwitterAPIConfiguration twitterAPIConfig = getTwitterAPIConfig();
    if (twitterAPIConfig == null) {
      return true;
    }
    final long lastTime = getFetchTwitterAPIConfigTime();
    if (lastTime == -1) {
      return true;
    }
    final long now = System.currentTimeMillis();
    return now - lastTime > TimeUnit.DAYS.toMillis(1);
  }
}
