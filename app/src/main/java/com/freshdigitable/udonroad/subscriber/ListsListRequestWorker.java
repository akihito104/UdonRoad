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

import android.support.annotation.NonNull;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.datastore.WritableSortedCache;
import com.freshdigitable.udonroad.timeline.fetcher.FetchQuery;
import com.freshdigitable.udonroad.timeline.fetcher.ListFetcher;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import io.reactivex.Single;
import io.reactivex.processors.PublishProcessor;
import twitter4j.UserList;

/**
 * Created by akihit on 2017/08/17.
 */

public class ListsListRequestWorker implements ListRequestWorker<UserList> {
  private final Map<StoreType, Provider<ListFetcher<UserList>>> listFetchers;
  private final WritableSortedCache<UserList> cache;
  private final PublishProcessor<UserFeedbackEvent> feedback;

  @Inject
  public ListsListRequestWorker(@NonNull Map<StoreType, Provider<ListFetcher<UserList>>> listFetchers,
                                @NonNull WritableSortedCache<UserList> cache,
                                @NonNull PublishProcessor<UserFeedbackEvent> feedback) {
    this.listFetchers = listFetchers;
    this.cache = cache;
    this.feedback = feedback;
  }

  @Override
  public ListFetchStrategy getFetchStrategy(StoreType type, long idForQuery, String query) {
    final Provider<ListFetcher<UserList>> userListProvider = listFetchers.get(type);
    if (userListProvider == null) {
      throw new IllegalStateException("unsupported StoreType: " + type);
    }
    final FetchQuery initQuery = new FetchQuery.Builder()
        .id(idForQuery)
        .build();
    final String storeName = type.nameWithSuffix(idForQuery, "");
    return new ListFetchStrategy() {
      @Override
      public void fetch() {
        fetchToStore(userListProvider.get().fetchInit(initQuery), storeName);
      }

      @Override
      public void fetchNext() {
        final FetchQuery nextQuery = new FetchQuery.Builder()
            .id(idForQuery)
            .lastPageCursor(getLastPageCursor(storeName))
            .build();
        fetchToStore(userListProvider.get().fetchNext(nextQuery), storeName);
      }
    };
  }

  private long getLastPageCursor(String storeName) {
    cache.open(storeName);
    final long lastPageCursor = cache.getLastPageCursor();
    cache.close();
    return lastPageCursor;
  }

  private void fetchToStore(Single<? extends Collection<UserList>> fetchTask, String storeName) {
    Util.fetchToStore(fetchTask,
        cache, storeName,
        (throwable) -> feedback.onNext(new UserFeedbackEvent(R.string.msg_fetch_user_list_failed)));
  }

  @Override
  public OnIffabItemSelectedListener getOnIffabItemSelectedListener(long selectedId) {
    return (item) -> {};
  }
}
