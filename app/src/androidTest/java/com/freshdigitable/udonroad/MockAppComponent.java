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

import com.freshdigitable.udonroad.module.ActivityBuilders;
import com.freshdigitable.udonroad.module.AppComponent;
import com.freshdigitable.udonroad.module.DataStoreModule;
import com.freshdigitable.udonroad.module.ViewModelModule;
import com.freshdigitable.udonroad.oauth.DemoListFetcherModule;
import com.freshdigitable.udonroad.repository.RepositoryModule;
import com.freshdigitable.udonroad.timeline.fetcher.ListsListFetcherModule;
import com.freshdigitable.udonroad.timeline.fetcher.StatusListFetcherModule;
import com.freshdigitable.udonroad.timeline.fetcher.UserListFetcherModule;
import com.freshdigitable.udonroad.timeline.repository.ListItemRepositoryModule;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.support.AndroidSupportInjectionModule;

/**
 * MockAppComponent is application component for test.
 * <p>
 * Created by akihit on 2016/06/16.
 */
@Singleton
@Component(modules = {
    MockTwitterApiModule.class,
    DataStoreModule.class,
    MockSharedPreferenceModule.class,
    RepositoryModule.class,
    ViewModelModule.class,
    StatusListFetcherModule.class,
    UserListFetcherModule.class,
    ListsListFetcherModule.class,
    ListItemRepositoryModule.class,
    DemoListFetcherModule.class,
    AndroidSupportInjectionModule.class,
    ActivityBuilders.class
})
public interface MockAppComponent extends AppComponent {

  @Component.Builder
  interface Builder {
    @BindsInstance
    Builder application(Context context);

    Builder twitterApi(MockTwitterApiModule module);

    Builder sharedPreferences(MockSharedPreferenceModule sharedPreferenceModule);

    MockAppComponent build();
  }

  void inject(MockMainApplication application);

  void inject(UserInfoActivityInstTest.Base userInfoActivityInstTest);
}
