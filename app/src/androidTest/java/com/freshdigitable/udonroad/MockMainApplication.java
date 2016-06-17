/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import twitter4j.UserStreamListener;

/**
 * Created by akihit on 2016/06/16.
 */
public class MockMainApplication extends MainApplication {

  private MockTwitterApiModule mockTwitterApiModule;

  @Override
  protected TwitterApiComponent createTwitterApiComponent() {
    mockTwitterApiModule = new MockTwitterApiModule(getApplicationContext());
    return DaggerMockTwitterApiComponent.builder()
        .mockTwitterApiModule(mockTwitterApiModule)
        .build();
  }

  public UserStreamListener getUserStreamListener() {
    return mockTwitterApiModule.userStreamListener;
  }
}
