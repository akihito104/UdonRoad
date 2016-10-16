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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
  private final TypedCache<User> cache;
  private final RealmConfiguration config;

  public ConfigStoreRealm(TypedCache<User> userCacheRealm) {
    this.cache = userCacheRealm;
    config = new RealmConfiguration.Builder()
        .name("config")
        .deleteRealmIfMigrationNeeded()
        .build();
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
        realm.insert(twitterAPIConfiguration);
      }
    });
  }

  @Override
  public TwitterAPIConfiguration getTwitterAPIConfig() {
    return realm.where(TwitterAPIConfigurationRealm.class)
        .findFirst();
  }

  @Override
  public void replaceIgnoringUsers(final Collection<Long> iDs) {
    realm.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        realm.delete(IgnoringUser.class);
        List<IgnoringUser> ignoringUsers = new ArrayList<>(iDs.size());
        for (Long id : iDs) {
          final IgnoringUser iUser = new IgnoringUser(id);
          ignoringUsers.add(iUser);
        }
        realm.insertOrUpdate(ignoringUsers);
      }
    });
  }

  @Override
  public boolean isIgnoredUser(long userId) {
    return realm.where(IgnoringUser.class)
        .equalTo("id", userId)
        .findFirst() != null;
  }

  @Override
  public void addIgnoringUser(User user) {
    final long userId = user.getId();
    realm.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        realm.insertOrUpdate(new IgnoringUser(userId));
      }
    });
  }

  @Override
  public void removeIgnoringUser(User user) {
    final long userId = user.getId();
    realm.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        realm.where(IgnoringUser.class)
            .equalTo("id", userId)
            .findAll()
            .deleteAllFromRealm();
      }
    });
  }
}
