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
@Component(modules = {TwitterApiModule.class, DataStoreModule.class})
public interface AppComponent {
  void inject(OAuthActivity oAuthActivity);

  void inject(MainActivity activity);

  void inject(MediaViewActivity mediaViewActivity);

  void inject(ReplyActivity replyActivity);

  void inject(UserStreamUtil userStreamUtil);

  void inject(TimelineFragment timelineFragment);

  void inject(UserInfoAppbarFragment userInfoAppbarFragment);

  void inject(StatusDetailFragment statusDetailFragment);
}
