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

package com.freshdigitable.udonroad.module;

import com.freshdigitable.udonroad.MainActivity;
import com.freshdigitable.udonroad.MainApplication;
import com.freshdigitable.udonroad.OAuthActivity;
import com.freshdigitable.udonroad.StatusDetailFragment;
import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.TimelineFragment;
import com.freshdigitable.udonroad.UserInfoActivity;
import com.freshdigitable.udonroad.UserInfoFragment;
import com.freshdigitable.udonroad.UserInfoPagerFragment;
import com.freshdigitable.udonroad.UserSettingsActivity;
import com.freshdigitable.udonroad.UserStreamUtil;
import com.freshdigitable.udonroad.listitem.ListItem;
import com.freshdigitable.udonroad.timeline.fetcher.DemoListFetcherModule;
import com.freshdigitable.udonroad.timeline.fetcher.ListFetcher;
import com.freshdigitable.udonroad.timeline.fetcher.ListsListFetcherModule;
import com.freshdigitable.udonroad.timeline.fetcher.StatusListFetcherModule;
import com.freshdigitable.udonroad.timeline.fetcher.UserListFetcherModule;
import com.freshdigitable.udonroad.input.TweetInputActivity;
import com.freshdigitable.udonroad.input.TweetInputFragment;
import com.freshdigitable.udonroad.media.MediaViewActivity;
import com.freshdigitable.udonroad.media.PhotoMediaFragment;
import com.freshdigitable.udonroad.repository.RepositoryModule;
import com.freshdigitable.udonroad.timeline.repository.ListItemRepositoryModule;

import java.util.Map;

import javax.inject.Singleton;

import dagger.Component;
import twitter4j.Status;
import twitter4j.User;
import twitter4j.UserList;

/**
 * AppComponent provides for dependency injection
 *
 * Created by akihit on 2016/06/16.
 */
@Singleton
@Component(modules = {
    TwitterApiModule.class, DataStoreModule.class, RepositoryModule.class,
    ViewModelModule.class, StatusListFetcherModule.class, UserListFetcherModule.class,
    ListsListFetcherModule.class, DemoListFetcherModule.class, ListItemRepositoryModule.class
})
public interface AppComponent {
  void inject(OAuthActivity oAuthActivity);

  void inject(MainActivity activity);

  void inject(MediaViewActivity mediaViewActivity);

  void inject(UserInfoActivity userInfoActivity);

  void inject(UserStreamUtil userStreamUtil);

  void inject(UserInfoFragment userInfoFragment);

  void inject(StatusDetailFragment statusDetailFragment);

  void inject(TweetInputFragment tweetInputFragment);

  void inject(UserInfoPagerFragment userInfoPagerFragment);

  void inject(MainApplication mainApplication);

  void inject(UserSettingsActivity.SettingsFragment settingsFragment);

  void inject(PhotoMediaFragment photoMediaFragment);

  void inject(TweetInputActivity tweetInputActivity);

  Map<StoreType, ListFetcher<Status>> storeTypeListFetcherStatusMap();

  Map<StoreType, ListFetcher<User>> storeTypeListFetcherUserMap();

  Map<StoreType, ListFetcher<UserList>> storeTypeListFetcherUserListMap();

  Map<StoreType, ListFetcher<ListItem>> storeTypeListFetcherListItemMap();

  void inject(TimelineFragment timelineFragment);
}
