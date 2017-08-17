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

import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.datastore.WritableSortedCache;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;

import javax.inject.Inject;

import io.reactivex.processors.PublishProcessor;
import twitter4j.UserList;

/**
 * Created by akihit on 2017/08/17.
 */

public class ListsListRequestWorker implements ListRequestWorker<UserList> {
  private final TwitterApi twitterApi;
  private final WritableSortedCache<UserList> cache;
  private final PublishProcessor<UserFeedbackEvent> feedback;

  @Inject
  public ListsListRequestWorker(@NonNull TwitterApi twitterApi,
                                @NonNull WritableSortedCache<UserList> cache,
                                @NonNull PublishProcessor<UserFeedbackEvent> feedback) {
    this.twitterApi = twitterApi;
    this.cache = cache;
    this.feedback = feedback;
  }

  @Override
  public ListFetchStrategy getFetchStrategy(StoreType type, long idForQuery, String query) {
    if (type == StoreType.LISTS) {
      String storeName = type.nameWithSuffix(idForQuery, "");
      return new ListFetchStrategy() {
        @Override
        public void fetch() {
          Util.fetchToStore(
              twitterApi.fetchUserListsOwnerships(idForQuery, 20, -1),
              cache, storeName,
              (throwable) -> feedback.onNext(new UserFeedbackEvent(0)));
        }

        @Override
        public void fetchNext() {
          cache.open(storeName);
          final long lastPageCursor = cache.getLastPageCursor();
          cache.close();
          Util.fetchToStore(
              twitterApi.fetchUserListsOwnerships(idForQuery, 20, lastPageCursor),
              cache, storeName,
              (throwable) -> feedback.onNext(new UserFeedbackEvent(0)));
        }
      };
    }
    throw new IllegalArgumentException();
  }

  @Override
  public OnIffabItemSelectedListener getOnIffabItemSelectedListener(long selectedId) {
    return (item) -> {};
  }
}
