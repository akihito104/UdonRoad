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

package com.freshdigitable.udonroad.module;

import android.content.Context;
import android.content.SharedPreferences;

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.BaseCache;
import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.datastore.MediaCache;
import com.freshdigitable.udonroad.datastore.SortedCache;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.datastore.UpdateSubjectFactory;
import com.freshdigitable.udonroad.module.realm.AppSettingStoreRealm;
import com.freshdigitable.udonroad.module.realm.BaseCacheRealm;
import com.freshdigitable.udonroad.module.realm.ConfigStoreRealm;
import com.freshdigitable.udonroad.module.realm.StatusCacheRealm;
import com.freshdigitable.udonroad.module.realm.TimelineStoreRealm;
import com.freshdigitable.udonroad.module.realm.UserCacheRealm;
import com.freshdigitable.udonroad.module.realm.UserSortedCacheRealm;
import com.freshdigitable.udonroad.subscriber.ListRequestWorker;
import com.freshdigitable.udonroad.subscriber.StatusListRequestWorker;
import com.freshdigitable.udonroad.subscriber.StatusRequestWorker;
import com.freshdigitable.udonroad.subscriber.UserListRequestWorker;
import com.freshdigitable.udonroad.subscriber.UserRequestWorker;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import twitter4j.Status;
import twitter4j.User;

/**
 * DataStoreModule defines injected modules for data store.
 *
 * Created by akihit on 2016/07/25.
 */
@Module
public class DataStoreModule {
  protected final Context context;

  public DataStoreModule(Context context) {
    this.context = context;
  }

  @Singleton
  @Provides
  BaseCache provideBaseCache() {
    return new BaseCacheRealm();
  }

  @Singleton
  @Provides
  TypedCache<Status> provideTypedCacheStatus(ConfigStore configStore) {
    return new StatusCacheRealm(configStore);
  }

  @Singleton
  @Provides
  MediaCache provideMediaCache(TypedCache<Status> configStore) {
    return ((StatusCacheRealm) configStore);
  }

  @Singleton
  @Provides
  TypedCache<User> provideTypedCacheUser() {
    return new UserCacheRealm();
  }

  @Provides
  @Singleton
  UpdateSubjectFactory provideUpdateSubjectFactory() {
    return new UpdateSubjectFactory();
  }

  @Provides
  SortedCache<Status> provideSortedCacheStatus(
      UpdateSubjectFactory factory, TypedCache<Status> statusCacheRealm, ConfigStore configStore) {
    return new TimelineStoreRealm(factory, statusCacheRealm, configStore);
  }

  @Provides
  SortedCache<User> provideSortedCacheUser(
      UpdateSubjectFactory factory, TypedCache<User> userCacheRealm) {
    return new UserSortedCacheRealm(factory, userCacheRealm);
  }

  @Singleton
  @Provides
  ConfigStore provideConfigStore() {
    return new ConfigStoreRealm();
  }

  @Singleton
  @Provides
  public SharedPreferences provideSharedPreferences() {
    return context.getSharedPreferences("udonroad_prefs", Context.MODE_PRIVATE);
  }

  @Singleton
  @Provides
  AppSettingStore provideAppSettingStore(TypedCache<User> userTypedCache,
                                                SharedPreferences sharedPreferences) {
    return new AppSettingStoreRealm(userTypedCache, sharedPreferences);
  }

  @Provides
  ListRequestWorker<Status> provideListRequestWorkerStatus(
      StatusRequestWorker<SortedCache<Status>> worker) {
    return new StatusListRequestWorker(worker);
  }

  @Provides
  ListRequestWorker<User> provideListRequestWorkerUser(
      UserRequestWorker<SortedCache<User>> worker) {
    return new UserListRequestWorker(worker);
  }
}
