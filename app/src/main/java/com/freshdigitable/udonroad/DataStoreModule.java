/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import com.freshdigitable.udonroad.datastore.StatusCache;
import com.freshdigitable.udonroad.datastore.TimelineStore;
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
}
