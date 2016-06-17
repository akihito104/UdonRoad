/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.app.Application;

/**
 * Created by akihit on 2016/06/16.
 */
public class MainApplication extends Application {

  private TwitterApiComponent twitterApiComponent;

  @Override
  public void onCreate() {
    super.onCreate();
    twitterApiComponent = createTwitterApiComponent();
  }

  protected TwitterApiComponent createTwitterApiComponent() {
    return DaggerTwitterApiComponent.builder()
        .twitterApiModule(new TwitterApiModule(getApplicationContext()))
        .build();
  }

  public TwitterApiComponent getTwitterApiComponent() {
    return twitterApiComponent;
  }
}
