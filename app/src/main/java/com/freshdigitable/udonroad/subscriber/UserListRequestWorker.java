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

import android.text.TextUtils;

import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.datastore.SortedCache;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;

import javax.inject.Inject;

import twitter4j.Paging;
import twitter4j.User;

/**
 * Created by akihit on 2017/03/31.
 */

public class UserListRequestWorker implements ListRequestWorker<User> {
  private final UserRequestWorker<SortedCache<User>> requestWorker;
  private StoreType storeType;
  private String storeName;

  @Inject
  public UserListRequestWorker(UserRequestWorker<SortedCache<User>> requestWorker) {
    this.requestWorker = requestWorker;
  }

  @Override
  public void open(StoreType type, String suffix) {
    if (!type.isForUser()) {
      throw new IllegalArgumentException();
    }
    this.storeType = type;
    storeName = TextUtils.isEmpty(suffix)
        ? type.storeName : type.prefix() + suffix;
    requestWorker.open(storeName);
  }

  @Override
  public SortedCache<User> getCache() {
    return requestWorker.getCache();
  }

  @Override
  public ListFetchStrategy getFetchStrategy(final long userId) {
    if (storeType == StoreType.USER_FOLLOWER) {
      return new ListFetchStrategy() {
        @Override
        public void fetch() {
          requestWorker.fetchFollowers(userId, -1);
        }

        @Override
        public void fetch(Paging paging) {
          requestWorker.fetchFollowers(userId, paging.getMaxId());
        }
      };
    } else if (storeType == StoreType.USER_FRIEND) {
      return new ListFetchStrategy() {
        @Override
        public void fetch() {
          requestWorker.fetchFriends(userId, -1);
        }

        @Override
        public void fetch(Paging paging) {
          requestWorker.fetchFriends(userId, paging.getMaxId());
        }
      };
    }
    throw new IllegalStateException();
  }

  @Override
  public OnIffabItemSelectedListener getOnIffabItemSelectedListener(long selectedId) {
    return item -> { /* nop */ };
  }

  @Override
  public void close() {
    requestWorker.close();
  }

  @Override
  public void drop() {
    requestWorker.getCache().drop(storeName);
  }
}
