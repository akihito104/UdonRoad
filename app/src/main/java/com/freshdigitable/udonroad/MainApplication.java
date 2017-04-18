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

package com.freshdigitable.udonroad;

import android.app.Application;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.module.AppComponent;
import com.freshdigitable.udonroad.module.DaggerAppComponent;
import com.freshdigitable.udonroad.module.DataStoreModule;
import com.freshdigitable.udonroad.module.TwitterApiModule;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;
import com.squareup.leakcanary.LeakCanary;

import javax.inject.Inject;

import io.realm.Realm;
import twitter4j.auth.AccessToken;

/**
 * MainApplication is custom Application class.
 *
 * Created by akihit on 2016/06/16.
 */
public class MainApplication extends Application {

  private AppComponent appComponent;
  @Inject
  TwitterApi twitterApi;
  @Inject
  AppSettingStore appSettings;

  @Override
  public void onCreate() {
    super.onCreate();
    appComponent = createAppComponent();
    LeakCanary.install(this);
    Realm.init(getApplicationContext());
    appComponent.inject(this);

    appSettings.open();
    final AccessToken accessToken = appSettings.getCurrentUserAccessToken();
    if (accessToken == null) {
      startActivity(new Intent(this, OAuthActivity.class));
      return;
    }
    twitterApi.setOAuthAccessToken(accessToken);
    appSettings.close();
  }

  @VisibleForTesting
  protected AppComponent createAppComponent() {
    return DaggerAppComponent.builder()
        .twitterApiModule(new TwitterApiModule(getApplicationContext()))
        .dataStoreModule(new DataStoreModule(getApplicationContext()))
        .build();
  }

  public AppComponent getAppComponent() {
    return appComponent;
  }
}
