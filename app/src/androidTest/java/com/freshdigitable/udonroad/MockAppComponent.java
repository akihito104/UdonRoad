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

import com.freshdigitable.udonroad.UserInfoActivityInstTest.UserInfoActivityInstTestBase;
import com.freshdigitable.udonroad.module.AppComponent;
import com.freshdigitable.udonroad.module.DataStoreModule;
import com.freshdigitable.udonroad.module.TwitterApiModule;

import javax.inject.Singleton;

import dagger.Component;

/**
 * MockAppComponent is application component for test.
 *
 * Created by akihit on 2016/06/16.
 */
@Singleton
@Component(modules = {TwitterApiModule.class, DataStoreModule.class,
    MockTwitterApiModule.MockTwitterStreamApiModule.class})
public interface MockAppComponent extends AppComponent {
  void inject(TimelineInstTestBase mainActivityInstTest);

  void inject(UserInfoActivityInstTestBase userInfoActivityInstTest);
}
