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

import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.datastore.TypedCache;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import twitter4j.TwitterAPIConfiguration;
import twitter4j.User;

/**
 * ConfigStoreRealm is data store for TwitterAPIConfiguration or other config.
 *
 * Created by akihit on 2016/07/30.
 */
public class ConfigStoreRealm implements ConfigStore {

  private Realm realm;
  private TypedCache<User> cache;

  @Override
  public void open() {
    if (cache == null) {
      cache = new UserCacheRealm();
    }
    cache.open();

    final RealmConfiguration config = new RealmConfiguration.Builder()
        .name("config")
        .deleteRealmIfMigrationNeeded()
        .build();
    realm = Realm.getInstance(config);
  }

  @Override
  public void close() {
    cache.close();
    realm.close();
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
        realm.insert(twitterAPIConfiguration);
      }
    });
  }

  @Override
  public TwitterAPIConfiguration getTwitterAPIConfig() {
    return realm.where(TwitterAPIConfigurationRealm.class)
        .findFirst();
  }
}
