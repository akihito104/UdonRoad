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
import android.support.annotation.StringRes;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.datastore.WritableSortedCache;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.processors.PublishProcessor;
import twitter4j.User;

/**
 * Created by akihit on 2017/03/31.
 */

public class UserListRequestWorker implements ListRequestWorker<User> {
  private final TwitterApi twitterApi;
  private final WritableSortedCache<User> sortedCache;
  private final PublishProcessor<UserFeedbackEvent> userFeedback;
  private StoreType storeType;
  private String storeName;

  @Inject
  public UserListRequestWorker(@NonNull TwitterApi twitterApi,
                               @NonNull WritableSortedCache<User> statusStore,
                               @NonNull PublishProcessor<UserFeedbackEvent> userFeedback) {
    this.twitterApi = twitterApi;
    this.sortedCache = statusStore;
    this.userFeedback = userFeedback;
  }

  @Override
  public void setStoreName(StoreType type, String suffix) {
    if (!type.isForUser()) {
      throw new IllegalArgumentException();
    }
    this.storeType = type;
    this.storeName = type.nameWithSuffix(suffix);
  }

  @Override
  public ListFetchStrategy getFetchStrategy(final long userId) {
    if (storeType == StoreType.USER_FOLLOWER) {
      return new ListFetchStrategy() {
        @Override
        public void fetch() {
          fetchToStore(twitterApi.getFollowersList(userId, -1L), R.string.msg_follower_list_failed);
        }

        @Override
        public void fetchNext() {
          if (!sortedCache.hasNextPage()) {
            userFeedback.onNext(new UserFeedbackEvent(R.string.msg_no_next_page));
            return;
          }
          final long lastPageCursor = sortedCache.getLastPageCursor();
          fetchToStore(twitterApi.getFollowersList(userId, lastPageCursor), R.string.msg_follower_list_failed);
        }
      };
    } else if (storeType == StoreType.USER_FRIEND) {
      return new ListFetchStrategy() {
        @Override
        public void fetch() {
          fetchToStore(twitterApi.getFriendsList(userId, -1L), R.string.msg_friends_list_failed);
        }

        @Override
        public void fetchNext() {
          if (!sortedCache.hasNextPage()) {
            userFeedback.onNext(new UserFeedbackEvent(R.string.msg_no_next_page));
            return;
          }
          final long lastPageCursor = sortedCache.getLastPageCursor();
          fetchToStore(twitterApi.getFriendsList(userId, lastPageCursor), R.string.msg_friends_list_failed);
        }
      };
    }
    throw new IllegalStateException();
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
