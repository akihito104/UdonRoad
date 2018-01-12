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

import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.datastore.MediaCache;
import com.freshdigitable.udonroad.datastore.SortedCache;
import com.freshdigitable.udonroad.datastore.StoreManager;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.datastore.UpdateSubjectFactory;
import com.freshdigitable.udonroad.datastore.WritableSortedCache;
import com.freshdigitable.udonroad.fetcher.ListFetcher;
import com.freshdigitable.udonroad.module.realm.AppSettingStoreRealm;
import com.freshdigitable.udonroad.module.realm.ConfigStoreRealm;
import com.freshdigitable.udonroad.module.realm.ListsSortedCacheRealm;
import com.freshdigitable.udonroad.module.realm.RealmStoreManager;
import com.freshdigitable.udonroad.module.realm.StatusCacheRealm;
import com.freshdigitable.udonroad.module.realm.TimelineStoreRealm;
import com.freshdigitable.udonroad.module.realm.UserCacheRealm;
import com.freshdigitable.udonroad.module.realm.UserSortedCacheRealm;
import com.freshdigitable.udonroad.module.realm.WritableListsSortedCache;
import com.freshdigitable.udonroad.module.realm.WritableTimelineRealm;
import com.freshdigitable.udonroad.module.realm.WritableUserSortedCacheRealm;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;
import com.freshdigitable.udonroad.subscriber.ListRequestWorker;
import com.freshdigitable.udonroad.subscriber.ListsListRequestWorker;
import com.freshdigitable.udonroad.subscriber.StatusListRequestWorker;
import com.freshdigitable.udonroad.subscriber.StatusRequestWorker;
import com.freshdigitable.udonroad.subscriber.UserFeedbackEvent;
import com.freshdigitable.udonroad.subscriber.UserListRequestWorker;

import java.util.Map;

import javax.inject.Provider;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.processors.PublishProcessor;
import twitter4j.Status;
import twitter4j.User;
import twitter4j.UserList;

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
  public StoreManager provideStoreManager() {
    return new RealmStoreManager();
  }

  @Singleton
  @Provides
  public SharedPreferences provideSharedPreferences() {
    return context.getSharedPreferences("udonroad_prefs", Context.MODE_PRIVATE);
  }

  @Provides
  AppSettingStore provideAppSettingStore(SharedPreferences sharedPreferences) {
    return new AppSettingStoreRealm(sharedPreferences, context.getFilesDir());
  }

  @Provides
  ConfigStore provideConfigStore(AppSettingStore appSetting) {
    return new ConfigStoreRealm(appSetting);
  }

  @Provides
  SortedCache<UserList> provideSortedCacheUserList(UpdateSubjectFactory factory,
                                                   TypedCache<User> userCache,
                                                   AppSettingStore appSetting) {
    return new ListsSortedCacheRealm(factory, userCache, appSetting);
  }

  @Provides
  TypedCache<Status> provideTypedCacheStatus(ConfigStore configStore, AppSettingStore appSetting) {
    return new StatusCacheRealm(configStore, appSetting);
  }

  @Provides
  MediaCache provideMediaCache(TypedCache<Status> configStore) {
    return ((StatusCacheRealm) configStore);
  }

  @Provides
  TypedCache<User> provideTypedCacheUser(AppSettingStore appSetting) {
    return new UserCacheRealm(appSetting);
  }

  @Provides
  SortedCache<Status> provideSortedCacheStatus(
      UpdateSubjectFactory factory, TypedCache<Status> statusCacheRealm, AppSettingStore appSetting) {
    return new TimelineStoreRealm(factory, statusCacheRealm, appSetting);
  }

  @Provides
  WritableSortedCache<Status> provideWritableSortedCacheStatus(TypedCache<Status> statusCache,
                                                               ConfigStore configStore,
                                                               AppSettingStore appSetting) {
    return new WritableTimelineRealm(statusCache, configStore, appSetting);
  }

  @Provides
  SortedCache<User> provideSortedCacheUser(
      UpdateSubjectFactory factory, TypedCache<User> userCacheRealm, AppSettingStore appSetting) {
    return new UserSortedCacheRealm(factory, userCacheRealm, appSetting);
  }

  @Provides
  WritableSortedCache<User> provideWritableSortedCacheUser(
      TypedCache<User> userCache, AppSettingStore appSetting) {
    return new WritableUserSortedCacheRealm(userCache, appSetting);
  }

  @Provides
  WritableSortedCache<UserList> provideWritableSortedCacheUserList(
      TypedCache<User> userCache, AppSettingStore appSetting) {
    return new WritableListsSortedCache(userCache, appSetting);
  }

  @Provides
  @Singleton
  UpdateSubjectFactory provideUpdateSubjectFactory() {
    return new UpdateSubjectFactory();
  }

  @Provides
  ListRequestWorker<Status> provideListRequestWorkerStatus(TwitterApi twitterApi,
                                                           TypedCache<User> userTypedCache,
                                                           WritableSortedCache<Status> sortedCache,
                                                           PublishProcessor<UserFeedbackEvent> userFeedback,
                                                           StatusRequestWorker requestWorker,
                                                           Map<StoreType, Provider<ListFetcher<Status>>> listFetchers) {
    return new StatusListRequestWorker(twitterApi, userTypedCache, sortedCache, userFeedback, requestWorker, listFetchers);
  }

  @Provides
  ListRequestWorker<User> provideListRequestWorkerUser(Map<StoreType, Provider<ListFetcher<User>>> listFetchers,
                                                       WritableSortedCache<User> sortedCache,
                                                       PublishProcessor<UserFeedbackEvent> userFeedback) {
    return new UserListRequestWorker(listFetchers, sortedCache, userFeedback);
  }

  @Provides
  ListRequestWorker<UserList> provideListRequestWorkerUserList(
      TwitterApi twitterApi,
      WritableSortedCache<UserList> cache,
      PublishProcessor<UserFeedbackEvent> userFeedback) {
    return new ListsListRequestWorker(twitterApi, cache, userFeedback);
  }
}
