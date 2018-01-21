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
import twitter4j.Paging;
import twitter4j.Status;

/**
 * Created by akihit on 2018/01/09.
 */
@Module
public class StatusListFetcherModule {

  @Provides
  @IntoMap
  @ListFetcherModuleKey(StoreType.HOME)
  ListFetcher<Status> provideHomeListFetcher(TwitterApi twitterApi) {
    return new ListFetcher<Status>() {
      @Override
      public Single<List<Status>> fetchInit(FetchQuery query) {
        return twitterApi.getHomeTimeline();
      }

      @Override
      public Single<List<Status>> fetchNext(FetchQuery query) {
        return twitterApi.getHomeTimeline(query.getPaging());
      }
    };
  }

  @Provides
  @IntoMap
  @ListFetcherModuleKey(StoreType.USER_HOME)
  ListFetcher<Status> provideUserHomeListFetcher(TwitterApi twitterApi) {
    return new ListFetcher<Status>() {
      @Override
      public Single<List<Status>> fetchInit(FetchQuery query) {
        return twitterApi.getUserTimeline(query.id);
      }

      @Override
      public Single<List<Status>> fetchNext(FetchQuery query) {
        return twitterApi.getUserTimeline(query.id, query.getPaging());
      }
    };
  }

  @Provides
  @IntoMap
  @ListFetcherModuleKey(StoreType.USER_FAV)
  ListFetcher<Status> provideUserFavListFetcher(TwitterApi twitterApi) {
    return new ListFetcher<Status>() {
      @Override
      public Single<List<Status>> fetchInit(FetchQuery query) {
        return twitterApi.getFavorites(query.id);
      }

      @Override
      public Single<List<Status>> fetchNext(FetchQuery query) {
        return twitterApi.getFavorites(query.id, query.getPaging());
      }
    };
  }

  @Provides
  @IntoMap
  @ListFetcherModuleKey(StoreType.LIST_TL)
  ListFetcher<Status> provideListTlFavListFetcher(TwitterApi twitterApi) {
    return new ListFetcher<Status>() {
      @Override
      public Single<List<Status>> fetchInit(FetchQuery query) {
        return twitterApi.fetchUserListsStatuses(query.id, new Paging(1, 20));
      }

      @Override
      public Single<List<Status>> fetchNext(FetchQuery query) {
        return twitterApi.getFavorites(query.id, query.getPaging());
      }
    };
  }

  @Provides
  @IntoMap
  @ListFetcherModuleKey(StoreType.SEARCH)
  ListFetcher<Status> provideSearchListFetcher(TwitterApi twitterApi) {
    return new ListFetcher<Status>() {
      @Override
      public Single<List<Status>> fetchInit(FetchQuery query) {
        return twitterApi.fetchSearch(
            new twitter4j.Query(query.searchQuery + " exclude:retweets")
                .count(20)
                .resultType(twitter4j.Query.RECENT));
      }

      @Override
      public Single<List<Status>> fetchNext(FetchQuery query) {
        return twitterApi.fetchSearch(
            new twitter4j.Query(query.searchQuery + " exclude:retweets")
                .count(20)
                .maxId(query.lastPageCursor)
                .resultType(twitter4j.Query.RECENT));
      }
    };
  }

  @Provides
  @IntoMap
  @ListFetcherModuleKey(StoreType.USER_MEDIA)
  ListFetcher<Status> provideUserMediaListFetcher(TwitterApi twitterApi) {
    return new ListFetcher<Status>() {
      @Override
      public Single<List<Status>> fetchInit(FetchQuery query) {
        return twitterApi.fetchSearch(
            new twitter4j.Query("from:" + query.searchQuery + " filter:media exclude:retweets")
                .count(20)
                .resultType(twitter4j.Query.RECENT));
      }

      @Override
      public Single<List<Status>> fetchNext(FetchQuery query) {
        return twitterApi.fetchSearch(
            new twitter4j.Query("from:" + query.searchQuery + " filter:media exclude:retweets")
                .count(20)
                .maxId(query.lastPageCursor)
                .resultType(twitter4j.Query.RECENT));
      }
    };
  }
}
