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
import android.util.Log;

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;

import javax.inject.Inject;

import io.reactivex.functions.Consumer;
import twitter4j.User;
import twitter4j.auth.AccessToken;

/**
 * Created by akihit on 2017/07/17.
 */

public class AppSettingRequestWorker implements RequestWorker {
  private static final String TAG = AppSettingRequestWorker.class.getSimpleName();
  private final TwitterApi twitterApi;
  private final AppSettingStore appSettings;
  private final TypedCache<User> userCache;

  @Inject
  public AppSettingRequestWorker(@NonNull TwitterApi twitterApi,
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
    final AccessToken accessToken = appSettings.getCurrentUserAccessToken();
    twitterApi.setOAuthAccessToken(accessToken);
    if (!appSettings.getCurrentUserDir().exists()) {
      appSettings.getCurrentUserDir().mkdir();
    }
    final boolean twitterAPIConfigFetchable = appSettings.isTwitterAPIConfigFetchable();
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

  private void fetchTwitterAPIConfig() {
    Util.fetchToStore(twitterApi.getTwitterAPIConfiguration(),
        appSettings, AppSettingStore::setTwitterAPIConfig,
        t -> {}, onErrorAction);
  }

  private final Consumer<Throwable> onErrorAction = throwable -> Log.e(TAG, "call: ", throwable);

  @Override
  public OnIffabItemSelectedListener getOnIffabItemSelectedListener(long selectedId) {
    return item -> {};
  }
}
