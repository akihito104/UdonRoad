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

import android.content.Context;
import android.content.SharedPreferences;

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.module.CurrentTimeProvider;
import com.freshdigitable.udonroad.module.realm.AppSettingStoreRealm;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by akihit on 2016/11/07.
 */
@Module
public class MockSharedPreferenceModule {

  public final SharedPreferences sharedPreferences;
  public final AppSettingStoreRealm appSettingStore;

  public MockSharedPreferenceModule(Context context) {
    sharedPreferences = getTestSharedPreferences(context);
    appSettingStore = new AppSettingStoreRealm(sharedPreferences, context.getFilesDir());
  }

  @Singleton
  @Provides
  SharedPreferences provideSharedPreferences() {
    return sharedPreferences;
  }

  public static SharedPreferences getTestSharedPreferences(Context context) {
    return context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE);
  }

  @Provides
  AppSettingStore provideAppSettingStore() {
    return appSettingStore;
  }

  @Provides
  @Singleton
  CurrentTimeProvider provideCurrentTimeProvider() {
    return () -> BuildConfig.STREAM_RETIREMENT_DATE - 1;
  }
}
