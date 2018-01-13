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

package com.freshdigitable.udonroad.timeline.repository;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.datastore.SortedCache;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.datastore.WritableSortedCache;
import com.freshdigitable.udonroad.listitem.ListsListItem;
import com.freshdigitable.udonroad.listitem.StatusListItem;
import com.freshdigitable.udonroad.listitem.UserListItem;
import com.freshdigitable.udonroad.subscriber.UserFeedbackEvent;
import com.freshdigitable.udonroad.timeline.fetcher.FetchQuery;
import com.freshdigitable.udonroad.timeline.fetcher.FetchQueryProvider;
import com.freshdigitable.udonroad.timeline.fetcher.ListFetcher;

import java.util.Map;

import javax.inject.Provider;
import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import io.reactivex.processors.PublishProcessor;
import twitter4j.Status;
import twitter4j.User;
import twitter4j.UserList;

/**
 * Created by akihit on 2018/01/13.
 */
@Module
public abstract class ListItemRepositoryModule {
  @Provides
  static SortedListItemCache<Status> providesSortedListItemCacheStatus(
      SortedCache<Status> cache, WritableSortedCache<Status> writableSortedCache) {
    return new SortedListItemCache<>(cache, writableSortedCache, StatusListItem::new);
  }

  @Provides
  static SortedListItemCache<User> providesSortedListItemCacheUser(
      SortedCache<User> cache, WritableSortedCache<User> writableSortedCache) {
    return new SortedListItemCache<>(cache, writableSortedCache, UserListItem::new);
  }

  @Provides
  static SortedListItemCache<UserList> providesSortedListItemCacheUserList(
      SortedCache<UserList> cache, WritableSortedCache<UserList> writableSortedCache) {
    return new SortedListItemCache<>(cache, writableSortedCache, ListsListItem::new);
  }

  @IntoMap
  @Provides
  @ListItemRepositoryModuleKey(StoreType.HOME)
  static ListItemRepository provideHomeListItemRepository(
      SortedListItemCache<Status> cache,
      Map<StoreType, Provider<ListFetcher<Status>>> listFetchers,
      PublishProcessor<UserFeedbackEvent> userFeedback) {
    return ListItemRepositoryCreator.create(cache, listFetchers, userFeedback,
        StoreType.HOME, R.string.msg_tweet_not_download);
  }

  @IntoMap
  @Provides
  @ListItemRepositoryModuleKey(StoreType.USER_HOME)
  static ListItemRepository provideUserHomeListItemRepository(
      SortedListItemCache<Status> cache,
      Map<StoreType, Provider<ListFetcher<Status>>> listFetchers,
      PublishProcessor<UserFeedbackEvent> userFeedback) {
    return ListItemRepositoryCreator.create(cache, listFetchers, userFeedback,
        StoreType.USER_HOME, R.string.msg_tweet_not_download);
  }

  @IntoMap
  @Provides
  @ListItemRepositoryModuleKey(StoreType.USER_FAV)
  static ListItemRepository provideUserFavListItemRepository(
      SortedListItemCache<Status> cache,
      Map<StoreType, Provider<ListFetcher<Status>>> listFetchers,
      PublishProcessor<UserFeedbackEvent> userFeedback) {
    return ListItemRepositoryCreator.create(cache, listFetchers, userFeedback,
        StoreType.USER_FAV, R.string.msg_tweet_not_download);
  }

  @IntoMap
  @Provides
  @ListItemRepositoryModuleKey(StoreType.SEARCH)
  static ListItemRepository provideSearchListItemRepository(
      SortedListItemCache<Status> cache,
      Map<StoreType, Provider<ListFetcher<Status>>> listFetchers,
      PublishProcessor<UserFeedbackEvent> userFeedback) {
    return ListItemRepositoryCreator.create(cache, listFetchers, userFeedback,
        StoreType.SEARCH, R.string.msg_tweet_not_download);
  }

  @IntoMap
  @Provides
  @ListItemRepositoryModuleKey(StoreType.LIST_TL)
  static ListItemRepository provideListTlListItemRepository(
      SortedListItemCache<Status> cache,
      Map<StoreType, Provider<ListFetcher<Status>>> listFetchers,
      PublishProcessor<UserFeedbackEvent> userFeedback) {
    return ListItemRepositoryCreator.create(cache, listFetchers, userFeedback,
        StoreType.LIST_TL, R.string.msg_tweet_not_download);
  }

  @IntoMap
  @Provides
  @ListItemRepositoryModuleKey(StoreType.USER_MEDIA)
  static ListItemRepository provideUserMediaListItemRepository(
      SortedListItemCache<Status> cache, TypedCache<User> userTypedCache,
      Map<StoreType, Provider<ListFetcher<Status>>> listFetchers,
      PublishProcessor<UserFeedbackEvent> userFeedback) {
    return ListItemRepositoryCreator.create(cache, listFetchers, userFeedback,
        StoreType.USER_MEDIA, R.string.msg_tweet_not_download, new FetchQueryProvider() {
          @Override
          public FetchQuery getInitQuery(long id, String q) {
            userTypedCache.open();
            final String screenName = userTypedCache.find(id).getScreenName();
            userTypedCache.close();
            return super.getInitQuery(id, screenName);
          }
        });
  }

  @IntoMap
  @Provides
  @ListItemRepositoryModuleKey(StoreType.USER_FOLLOWER)
  static ListItemRepository provideUserFollowerListItemRepository(
      SortedListItemCache<User> cache,
      Map<StoreType, Provider<ListFetcher<User>>> listFetchers,
      PublishProcessor<UserFeedbackEvent> userFeedback) {
    return ListItemRepositoryCreator.create(cache, listFetchers, userFeedback,
        StoreType.USER_FOLLOWER, R.string.msg_follower_list_failed);
  }

  @IntoMap
  @Provides
  @ListItemRepositoryModuleKey(StoreType.USER_FRIEND)
  static ListItemRepository provideUserFriendListItemRepository(
      SortedListItemCache<User> cache,
      Map<StoreType, Provider<ListFetcher<User>>> listFetchers,
      PublishProcessor<UserFeedbackEvent> userFeedback) {
    return ListItemRepositoryCreator.create(cache, listFetchers, userFeedback,
        StoreType.USER_FRIEND, R.string.msg_friends_list_failed);
  }

  @IntoMap
  @Provides
  @ListItemRepositoryModuleKey(StoreType.OWNED_LIST)
  static ListItemRepository provideOwnedListListItemRepository(
      SortedListItemCache<UserList> cache,
      Map<StoreType, Provider<ListFetcher<UserList>>> listFetchers,
      PublishProcessor<UserFeedbackEvent> userFeedback) {
    return ListItemRepositoryCreator.create(cache, listFetchers, userFeedback,
        StoreType.OWNED_LIST, R.string.msg_fetch_user_list_failed);
  }

  @IntoMap
  @Provides
  @ListItemRepositoryModuleKey(StoreType.USER_LIST)
  static ListItemRepository provideUserListListItemRepository(
      SortedListItemCache<UserList> cache,
      Map<StoreType, Provider<ListFetcher<UserList>>> listFetchers,
      PublishProcessor<UserFeedbackEvent> userFeedback) {
    return ListItemRepositoryCreator.create(cache, listFetchers, userFeedback,
        StoreType.USER_LIST, R.string.msg_fetch_user_list_failed);
  }

  @Binds
  @IntoMap
  @ListItemRepositoryModuleKey(StoreType.CONVERSATION)
  abstract ListItemRepository bindsConversationListItemRepository(ConversationListItemRepository repository);

  @Binds
  @Singleton
  abstract ListItemRepositoryProvider bindsListItemRepositoryProvider(ListItemRepositoryProvider repositories);
}
