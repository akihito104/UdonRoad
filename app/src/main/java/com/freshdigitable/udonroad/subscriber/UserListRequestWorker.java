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

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.PublishProcessor;
import twitter4j.Paging;
import twitter4j.User;

/**
 * Created by akihit on 2017/03/31.
 */

public class UserListRequestWorker implements ListRequestWorker<User> {
  private final TwitterApi twitterApi;
  private final WritableSortedCache<User> sortedCache;
  private final PublishProcessor<UserFeedbackEvent> userFeedback;
  private StoreType storeType;

  @Inject
  public UserListRequestWorker(@NonNull TwitterApi twitterApi,
                               @NonNull WritableSortedCache<User> statusStore,
                               @NonNull PublishProcessor<UserFeedbackEvent> userFeedback) {
    this.twitterApi = twitterApi;
    this.sortedCache = statusStore;
    this.userFeedback = userFeedback;
  }

  @Override
  public void open(StoreType type, String suffix) {
    if (!type.isForUser()) {
      throw new IllegalArgumentException();
    }
    this.storeType = type;
    sortedCache.open(type.nameWithSuffix(suffix));
  }

  @Override
  public ListFetchStrategy getFetchStrategy(final long userId) {
    if (storeType == StoreType.USER_FOLLOWER) {
      return new ListFetchStrategy() {
        @Override
        public void fetch() {
          fetchFollowers(userId, -1);
        }

        @Override
        public void fetch(Paging paging) {
          fetchFollowers(userId, paging.getMaxId());
        }
      };
    } else if (storeType == StoreType.USER_FRIEND) {
      return new ListFetchStrategy() {
        @Override
        public void fetch() {
          fetchFriends(userId, -1);
        }

        @Override
        public void fetch(Paging paging) {
          fetchFriends(userId, paging.getMaxId());
        }
      };
    }
    throw new IllegalStateException();
  }

  private void fetchFollowers(final long userId, final long cursor) {
    twitterApi.getFollowersList(userId, cursor)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(createUpsertListAction(),
            onErrorFeedback(R.string.msg_follower_list_failed));
  }

  private void fetchFriends(long userId, long cursor) {
    twitterApi.getFriendsList(userId, cursor)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(createUpsertListAction(),
            onErrorFeedback(R.string.msg_friends_list_failed));
  }

  @NonNull
  private Consumer<Throwable> onErrorFeedback(@StringRes final int msg) {
    return throwable -> userFeedback.onNext(new UserFeedbackEvent(msg));
  }

  @NonNull
  private Consumer<List<User>> createUpsertListAction() {
    return sortedCache::upsert;
  }

  @Override
  public OnIffabItemSelectedListener getOnIffabItemSelectedListener(long selectedId) {
    return item -> { /* nop */ };
  }

  @Override
  public void close() {
    sortedCache.close();
  }

  @Override
  public void drop() {
    sortedCache.drop();
  }
}
