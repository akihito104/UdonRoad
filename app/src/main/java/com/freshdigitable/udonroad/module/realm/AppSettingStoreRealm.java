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

import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.datastore.AppSettingStore;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import twitter4j.TwitterAPIConfiguration;
import twitter4j.User;
import twitter4j.auth.AccessToken;

/**
 * AppSettingStoreRealm is implementation of AppSettingStore with Realm.
 *
 * Created by akihit on 2016/11/07.
 */

public class AppSettingStoreRealm implements AppSettingStore {
  private Realm realm;
  private RealmConfiguration config;
  private final SharedPreferences prefs;
  private final File filesDir;

  public AppSettingStoreRealm(SharedPreferences prefs, File filesDir) {
    this.prefs = prefs;
    this.filesDir = filesDir;
  }

  @Override
  public void open() {
    config = new RealmConfiguration.Builder()
        .name(StoreType.APP_SETTINGS.storeName)
        .deleteRealmIfMigrationNeeded()
        .build();
    realm = Realm.getInstance(config);
  }

  @Override
  public void close() {
    realm.close();
  }

  @Override
  public void clear() {
    realm.executeTransaction(_realm -> _realm.deleteAll());
    final Set<String> users = prefs.getStringSet(AUTHENTICATED_USERS, Collections.emptySet());
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(TWITTER_API_CONFIG_DATE);
    editor.remove(CURRENT_USER_ID);
    for (String u : users) {
      editor.remove(ACCESS_TOKEN_PREFIX + u)
          .remove(TOKEN_SECRET_PREFIX + u);
    }
    editor.remove(AUTHENTICATED_USERS);
    editor.apply();
  }

  @Override
  public void drop() {
    Realm.deleteRealm(config);
  }

  @Override
  public void addAuthenticatedUser(final User authenticatedUser) {
    if (!isAuthenticatedUser(authenticatedUser.getId())) {
      throw new IllegalArgumentException("unregistered userId: " + authenticatedUser.getId());
    }
    final UserRealm user = realm.where(UserRealm.class)
        .equalTo("id", authenticatedUser.getId())
        .findFirst();
    realm.executeTransaction(r -> {
      if (user == null) {
        final UserRealm userRealm = new UserRealm(authenticatedUser);
        r.insertOrUpdate(userRealm);
      } else {
        user.merge(authenticatedUser, r);
      }
    });
  }

  @Override
  public List<? extends User> getAllAuthenticatedUsers() {
    return realm.where(UserRealm.class)
        .findAll();
  }

  @Override
  public Observable<User> observeCurrentUser() {
    return CacheUtil.observeById(realm, getCurrentUserId(), UserRealm.class)
        .cast(User.class);
  }

  @Override
  public void setTwitterAPIConfig(final TwitterAPIConfiguration twitterAPIConfig) {
    if (twitterAPIConfig == null) {
      return;
    }
    realm.executeTransaction(r -> {
      r.delete(TwitterAPIConfigurationRealm.class);
      final TwitterAPIConfigurationRealm twitterAPIConfiguration
          = new TwitterAPIConfigurationRealm(twitterAPIConfig);
      r.insertOrUpdate(twitterAPIConfiguration);
    });
    setFetchTwitterAPIConfigTime(System.currentTimeMillis());
  }

  @Override
  public TwitterAPIConfiguration getTwitterAPIConfig() {
    return realm.where(TwitterAPIConfigurationRealm.class)
        .findFirst();
  }

  private static final String TWITTER_API_CONFIG_DATE = "twitterAPIConfigDate";

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

  private static final String AUTHENTICATED_USERS = "authenticatedUsers";
  private static final String CURRENT_USER_ID = "currentUserId";
  private static final String ACCESS_TOKEN_PREFIX = "accessToken_";
  private static final String TOKEN_SECRET_PREFIX = "tokenSecret_";
  static final String USER_DIR_PREFIX = "user_";

  @Override
  public void storeAccessToken(AccessToken token) {
    final long userId = token.getUserId();
    final Set<String> authenticatedUsers
        = prefs.getStringSet(AUTHENTICATED_USERS, new HashSet<>());
    authenticatedUsers.add(Long.toString(userId));

    prefs.edit()
        .putStringSet(AUTHENTICATED_USERS, authenticatedUsers)
        .putString(ACCESS_TOKEN_PREFIX + userId, token.getToken())
        .putString(TOKEN_SECRET_PREFIX + userId, token.getTokenSecret())
        .apply();
  }

  @Override
  public AccessToken getCurrentUserAccessToken() {
    final long currentUserId = getCurrentUserId();
    if (currentUserId < 0) {
      return null;
    }
    final String token = prefs.getString(ACCESS_TOKEN_PREFIX + currentUserId, null);
    final String secret = prefs.getString(TOKEN_SECRET_PREFIX + currentUserId, null);
    if (token == null || secret == null) {
      return null;
    }
    return new AccessToken(token, secret);
  }

  @Override
  public long getCurrentUserId() {
    return prefs.getLong(CURRENT_USER_ID, -1);
  }

  @Override
  public File getCurrentUserDir() {
    return new File(filesDir, USER_DIR_PREFIX + getCurrentUserId());
  }

  @Override
  public void setCurrentUserId(long userId) {
    if (!isAuthenticatedUser(userId)) {
      throw new IllegalArgumentException("unregistered userId: " + userId);
    }
    prefs.edit()
        .putLong(CURRENT_USER_ID, userId)
        .apply();
  }

  private boolean isAuthenticatedUser(long userId) {
    final Set<String> userIds = prefs.getStringSet(AUTHENTICATED_USERS, new HashSet<>());
    for (String id : userIds) {
      if (id.equals(Long.toString(userId))) {
        return true;
      }
    }
    return false;
  }
}
