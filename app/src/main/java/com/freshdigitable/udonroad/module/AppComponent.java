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

import com.freshdigitable.udonroad.MainApplication;
import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.repository.RepositoryModule;
import com.freshdigitable.udonroad.timeline.fetcher.ListFetcher;
import com.freshdigitable.udonroad.timeline.fetcher.ListsListFetcherModule;
import com.freshdigitable.udonroad.timeline.fetcher.StatusListFetcherModule;
import com.freshdigitable.udonroad.timeline.fetcher.UserListFetcherModule;
import com.freshdigitable.udonroad.timeline.repository.ListItemRepositoryModule;

import java.util.Map;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.support.AndroidSupportInjectionModule;
import twitter4j.Status;
import twitter4j.User;
import twitter4j.UserList;

/**
 * AppComponent provides for dependency injection
 *
 * Created by akihit on 2016/06/16.
 */
@Singleton
@Component(modules = {
    TwitterApiModule.class,
    DataStoreModule.class,
    SharedPreferenceModule.class,
    RepositoryModule.class,
    ViewModelModule.class,
    StatusListFetcherModule.class,
    UserListFetcherModule.class,
    ListsListFetcherModule.class,
    ListItemRepositoryModule.class,
    AndroidSupportInjectionModule.class,
    ActivityBuilders.class
})
public interface AppComponent {

  @Component.Builder
  interface Builder {
    @BindsInstance
    Builder application(Context context);

    AppComponent build();
  }

  void inject(MainApplication mainApplication);

  Map<StoreType, ListFetcher<Status>> storeTypeListFetcherStatusMap();

  Map<StoreType, ListFetcher<User>> storeTypeListFetcherUserMap();

  Map<StoreType, ListFetcher<UserList>> storeTypeListFetcherUserListMap();
}
