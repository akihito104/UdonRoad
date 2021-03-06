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

package com.freshdigitable.udonroad.subscriber;

import android.support.annotation.NonNull;

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;

import java.util.Set;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import timber.log.Timber;
import twitter4j.User;

/**
 * Created by akihit on 2017/07/17.
 */

public class AppSettingRequestWorker implements RequestWorker {
  private static final String TAG = AppSettingRequestWorker.class.getSimpleName();
  private final TwitterApi twitterApi;
  private final AppSettingStore appSettings;
  private final TypedCache<User> userCache;

  @Inject
  AppSettingRequestWorker(@NonNull TwitterApi twitterApi,
                                 @NonNull AppSettingStore appSettings,
                                 @NonNull TypedCache<User> userCache) {
    this.twitterApi = twitterApi;
    this.appSettings = appSettings;
    this.userCache = userCache;
  }

  public boolean setup() {
    if (!isAuthenticated()) {
      return false;
    }
    appSettings.open();
    if (!appSettings.getCurrentUserDir().exists()) {
      appSettings.getCurrentUserDir().mkdir();
    }
    final boolean twitterAPIConfigFetchable = appSettings.isTwitterAPIConfigFetchable();
    setupAuthenticatedUsers();
    appSettings.close();
    if (twitterAPIConfigFetchable) {
      fetchTwitterAPIConfig();
    }
    return true;
  }

  private boolean isAuthenticated() {
    appSettings.open();
    final boolean authenticated = appSettings.getCurrentUserAccessToken() != null;
    appSettings.close();
    return authenticated;
  }

  public void verifyCredentials() {
    if (!isAuthenticated()) {
      return;
    }
    Util.fetchToStore(twitterApi.verifyCredentials(),
        appSettings, (appSettingStore, authenticatedUser) -> {
          appSettingStore.addAuthenticatedUser(authenticatedUser);
          userCache.open();
          userCache.upsert(authenticatedUser);
          userCache.close();
        },
        t -> {}, onErrorAction);
  }

  private void setupAuthenticatedUsers() {
    final Set<String> allAuthenticatedUserIds = appSettings.getAllAuthenticatedUserIds();
    Observable.fromIterable(allAuthenticatedUserIds)
        .map(Long::parseLong)
        .filter(id -> id > 0)
        .flatMapSingle(twitterApi::showUser)
        .blockingForEach(appSettings::addAuthenticatedUser);
  }

  private void fetchTwitterAPIConfig() {
    Util.fetchToStore(twitterApi.getTwitterAPIConfiguration(),
        appSettings, AppSettingStore::setTwitterAPIConfig,
        t -> {}, onErrorAction);
  }

  private final Consumer<Throwable> onErrorAction = throwable -> Timber.tag(TAG).e(throwable, "call: ");

  @Override
  public OnIffabItemSelectedListener getOnIffabItemSelectedListener(long selectedId) {
    return item -> {};
  }
}
