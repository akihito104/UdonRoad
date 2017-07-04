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
import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.datastore.MediaCache;
import com.freshdigitable.udonroad.datastore.SortedCache;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.datastore.UpdateSubjectFactory;
import com.freshdigitable.udonroad.datastore.WritableSortedCache;
import com.freshdigitable.udonroad.module.realm.AppSettingStoreRealm;
import com.freshdigitable.udonroad.module.realm.ConfigStoreRealm;
import com.freshdigitable.udonroad.module.realm.StatusCacheRealm;
import com.freshdigitable.udonroad.module.realm.TimelineStoreRealm;
import com.freshdigitable.udonroad.module.realm.UserCacheRealm;
import com.freshdigitable.udonroad.module.realm.UserSortedCacheRealm;
import com.freshdigitable.udonroad.module.realm.WritableTimelineRealm;
import com.freshdigitable.udonroad.module.realm.WritableUserSortedCacheRealm;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;
import com.freshdigitable.udonroad.subscriber.ListRequestWorker;
import com.freshdigitable.udonroad.subscriber.StatusListRequestWorker;
import com.freshdigitable.udonroad.subscriber.StatusRequestWorker;
import com.freshdigitable.udonroad.subscriber.UserFeedbackEvent;
import com.freshdigitable.udonroad.subscriber.UserListRequestWorker;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.processors.PublishProcessor;
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
      UpdateSubjectFactory factory, TypedCache<Status> statusCacheRealm) {
    return new TimelineStoreRealm(factory, statusCacheRealm);
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
  AppSettingStore provideAppSettingStore(SharedPreferences sharedPreferences) {
    return new AppSettingStoreRealm(sharedPreferences);
  }

  @Provides
  WritableSortedCache<Status> provideWritableSortedCacheStatus(TypedCache<Status> statusCache,
                                                               ConfigStore configStore) {
    return new WritableTimelineRealm(statusCache, configStore);
  }

  @Provides
  ListRequestWorker<Status> provideListRequestWorkerStatus(TwitterApi twitterApi,
                                                           WritableSortedCache<Status> sortedCache,
                                                           PublishProcessor<UserFeedbackEvent> userFeedback,
                                                           StatusRequestWorker requestWorker) {
    return new StatusListRequestWorker(twitterApi, sortedCache, userFeedback, requestWorker);
  }

  @Provides
  WritableSortedCache<User> provideWritableSortedCacheUser(TypedCache<User> userCache) {
    return new WritableUserSortedCacheRealm(userCache);
  }

  @Provides
  ListRequestWorker<User> provideListRequestWorkerUser(TwitterApi twitterApi,
                                                       WritableSortedCache<User> sortedCache,
                                                       PublishProcessor<UserFeedbackEvent> userFeedback) {
    return new UserListRequestWorker(twitterApi, sortedCache, userFeedback);
  }
}
