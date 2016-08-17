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

  void inject(UserInfoActivity userInfoActivity);

  void inject(UserStreamUtil userStreamUtil);

  void inject(UserInfoFragment userInfoFragment);

  void inject(StatusDetailFragment statusDetailFragment);

  void inject(TweetInputFragment tweetInputFragment);

  void inject(UserInfoPagerFragment userInfoPagerFragment);
}
