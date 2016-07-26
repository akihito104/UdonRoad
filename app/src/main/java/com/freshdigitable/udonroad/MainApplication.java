/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.app.Application;
import android.support.annotation.VisibleForTesting;

import com.squareup.leakcanary.LeakCanary;

/**
 * Created by akihit on 2016/06/16.
 */
public class MainApplication extends Application {

  private AppComponent appComponent;

  @Override
  public void onCreate() {
    super.onCreate();
    appComponent = createAppComponent();
    LeakCanary.install(this);
  }

  @VisibleForTesting
  protected AppComponent createAppComponent() {
    return DaggerAppComponent.builder()
        .twitterApiModule(new TwitterApiModule(getApplicationContext()))
        .dataStoreModule(new DataStoreModule(getApplicationContext()))
        .build();
  }
  
  public AppComponent getAppComponent() {
    return appComponent;
  }
}
