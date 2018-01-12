/*
 * Copyright (c) 2017. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad.subscriber;

import android.support.annotation.StringRes;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.datastore.WritableSortedCache;
import com.freshdigitable.udonroad.fetcher.FetchQuery;
import com.freshdigitable.udonroad.fetcher.ListFetcher;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;

import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import io.reactivex.Single;
import io.reactivex.processors.PublishProcessor;
import twitter4j.User;

/**
 * Created by akihit on 2017/03/31.
 */

public class UserListRequestWorker implements ListRequestWorker<User> {
  private final WritableSortedCache<User> sortedCache;
  private final PublishProcessor<UserFeedbackEvent> userFeedback;
  private final Map<StoreType, Provider<ListFetcher<User>>> listFetchers;
  private String storeName;

  public UserListRequestWorker(Map<StoreType, Provider<ListFetcher<User>>> listFetchers,
                               WritableSortedCache<User> sortedCache,
                               PublishProcessor<UserFeedbackEvent> userFeedback) {
    this.sortedCache = sortedCache;
    this.userFeedback = userFeedback;
    this.listFetchers = listFetchers;
  }

  @Override
  public ListFetchStrategy getFetchStrategy(StoreType storeType, long userId, String query) {
    this.storeName = storeType.nameWithSuffix(userId, query);
    final Provider<ListFetcher<User>> listFetcherProvider = listFetchers.get(storeType);
    final FetchQuery initQuery = getInitQuery(storeType, userId, query);
    final @StringRes int messageRes = getMessageRes(storeType);
    return new ListFetchStrategy() {
      @Override
      public void fetch() {
        fetchToStore(listFetcherProvider.get().fetchInit(initQuery), messageRes);
      }

      @Override
      public void fetchNext() {
        final FetchQuery nextQuery = getNextQuery(storeType, userId, query);
        if (nextQuery == null) {
          userFeedback.onNext(new UserFeedbackEvent(R.string.msg_no_next_page));
          return;
        }
        fetchToStore(listFetcherProvider.get().fetchNext(nextQuery), messageRes);
      }
    };
  }

  private int getMessageRes(StoreType storeType) {
    if (storeType == StoreType.USER_FOLLOWER) {
      return R.string.msg_follower_list_failed;
    } else if (storeType == StoreType.USER_FRIEND) {
      return R.string.msg_friends_list_failed;
    }
    throw new IllegalStateException("unsupported StoreType: " + storeType);
  }

  private FetchQuery getInitQuery(StoreType storeType, long id, String query) {
    return new FetchQuery.Builder()
        .id(id)
        .build();
  }

  private FetchQuery getNextQuery(StoreType storeType, long id, String query) {
    return sortedCache.hasNextPage() ?
        new FetchQuery.Builder()
            .id(id)
            .lastPageCursor(sortedCache.getLastPageCursor())
            .build()
        : null;
  }

  private void fetchToStore(Single<? extends List<User>> fetchTask, @StringRes int failureRes) {
    Util.fetchToStore(fetchTask, sortedCache, storeName,
        throwable -> userFeedback.onNext(new UserFeedbackEvent(failureRes)));
  }

  @Override
  public OnIffabItemSelectedListener getOnIffabItemSelectedListener(long selectedId) {
    return item -> { /* nop */ };
  }
}
