/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by akihit on 2016/06/16.
 */
@Singleton
@Component(modules = {MockTwitterApiModule.class, DataStoreModule.class})
public interface MockAppComponent extends AppComponent {
  void inject(MainActivityInstTestBase mainActivityInstTest);
}
