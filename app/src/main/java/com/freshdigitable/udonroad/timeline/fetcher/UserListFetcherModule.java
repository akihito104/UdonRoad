/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad.timeline.fetcher;

import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;

import java.util.List;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import io.reactivex.Single;
import twitter4j.User;

/**
 * Created by akihit on 2018/01/12.
 */
@Module
public class UserListFetcherModule {
  @Provides
  @IntoMap
  @ListFetcherModuleKey(StoreType.USER_FOLLOWER)
  ListFetcher<User> provideUserFollowerListFetcher(TwitterApi twitterApi) {
    return new ListFetcher<User>() {
      @Override
      public Single<? extends List<User>> fetchInit(FetchQuery query) {
        return twitterApi.getFollowersList(query.id, -1L);
      }

      @Override
      public Single<? extends List<User>> fetchNext(FetchQuery query) {
        return twitterApi.getFollowersList(query.id, query.lastPageCursor);
      }
    };
  }

  @Provides
  @IntoMap
  @ListFetcherModuleKey(StoreType.USER_FRIEND)
  ListFetcher<User> provideUserFriendListFetcher(TwitterApi twitterApi) {
    return new ListFetcher<User>() {
      @Override
      public Single<? extends List<User>> fetchInit(FetchQuery query) {
        return twitterApi.getFriendsList(query.id, -1L);
      }

      @Override
      public Single<? extends List<User>> fetchNext(FetchQuery query) {
        return twitterApi.getFriendsList(query.id, query.lastPageCursor);
      }
    };
  }
}