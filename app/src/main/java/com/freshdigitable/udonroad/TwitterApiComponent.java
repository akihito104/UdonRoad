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
@Component(modules = TwitterApiModule.class)
public interface TwitterApiComponent {
  void inject(OAuthActivity oAuthActivity);

  void inject(MainActivity activity);

  void inject(MediaViewActivity mediaViewActivity);

  void inject(UserStreamUtil userStreamUtil);

  void inject(TimelineFragment timelineFragment);
}
