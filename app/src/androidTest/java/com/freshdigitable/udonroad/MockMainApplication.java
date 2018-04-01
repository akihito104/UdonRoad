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

import android.support.test.InstrumentationRegistry;

import com.freshdigitable.udonroad.MockTwitterApiModule.UserStreamListenerHolder;
import com.freshdigitable.udonroad.module.AppComponent;

import javax.inject.Inject;

import twitter4j.UserStreamListener;

/**
 * MockMainApplication is custom Application class for test.
 *
 * Created by akihit on 2016/06/16.
 */
public class MockMainApplication extends MainApplication {

  @Inject
  UserStreamListenerHolder streamApiModule;

  @Override
  protected AppComponent createAppComponent() {
    final MockAppComponent component = DaggerMockAppComponent.builder()
        .application(InstrumentationRegistry.getTargetContext())
        .build();
    component.inject(this);
    return component;
  }

  public UserStreamListener getUserStreamListener() {
    return streamApiModule.userStreamListener;
  }
}
