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

import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.datastore.StatusCache;
import com.freshdigitable.udonroad.datastore.TimelineStore;
import com.freshdigitable.udonroad.realmdata.ConfigStoreRealm;
import com.freshdigitable.udonroad.realmdata.StatusCacheRealm;
import com.freshdigitable.udonroad.realmdata.TimelineStoreRealm;

import dagger.Module;
import dagger.Provides;

/**
 * Created by akihit on 2016/07/25.
 */
@Module
public class DataStoreModule {
  @Provides
  public StatusCache provideStatusCache() {
    return new StatusCacheRealm();
  }

  @Provides
  public TimelineStore provideTimelineStore() {
    return new TimelineStoreRealm();
  }

  @Provides
  public ConfigStore provideConfigStore() {
    return new ConfigStoreRealm();
  }
}
