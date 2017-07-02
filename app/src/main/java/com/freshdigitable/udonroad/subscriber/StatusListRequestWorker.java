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
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.datastore.WritableSortedCache;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.PublishProcessor;
import twitter4j.Paging;
import twitter4j.Status;

/**
 * Created by akihit on 2017/03/31.
 */

public class StatusListRequestWorker implements ListRequestWorker<Status> {
  private final TwitterApi twitterApi;
  private final WritableSortedCache<Status> sortedCache;
  private final PublishProcessor<UserFeedbackEvent> userFeedback;
  private final StatusRequestWorker requestWorker;
  private StoreType storeType;

  @Inject
  public StatusListRequestWorker(@NonNull TwitterApi twitterApi,
                                 @NonNull WritableSortedCache<Status> statusStore,
                                 @NonNull PublishProcessor<UserFeedbackEvent> userFeedback,
                                 @NonNull StatusRequestWorker requestWorker) {
    this.twitterApi = twitterApi;
    this.sortedCache = statusStore;
    this.userFeedback = userFeedback;
    this.requestWorker = requestWorker;
  }

  @Override
  public void open(@NonNull StoreType type, @Nullable String suffix) {
    if (!type.isForStatus()) {
      throw new IllegalArgumentException();
    }
    this.storeType = type;
    sortedCache.open(type.nameWithSuffix(suffix));
  }

  @Override
  public ListFetchStrategy getFetchStrategy(final long id) {
    if (storeType == StoreType.HOME) {
      return new ListFetchStrategy() {
        @Override
        public void fetch() {
          subscribeWithCommonTask(twitterApi.getHomeTimeline());
        }

        @Override
        public void fetch(Paging paging) {
          subscribeWithCommonTask(twitterApi.getHomeTimeline(paging));
        }
      };
    } else if (storeType == StoreType.USER_HOME) {
      return new ListFetchStrategy() {
        @Override
        public void fetch() {
          subscribeWithCommonTask(twitterApi.getUserTimeline(id));
        }

        @Override
        public void fetch(Paging paging) {
          if (paging == null) {
            fetch();
            return;
          }
          subscribeWithCommonTask(twitterApi.getUserTimeline(id, paging));
        }
      };
    } else if (storeType == StoreType.USER_FAV) {
      return new ListFetchStrategy() {
        @Override
        public void fetch() {
          subscribeWithCommonTask(twitterApi.getFavorites(id));
        }

        @Override
        public void fetch(Paging paging) {
          if (paging == null) {
            fetch();
          } else {
            subscribeWithCommonTask(twitterApi.getFavorites(id, paging));
          }
        }
      };
    } else if (storeType == StoreType.CONVERSATION) {
      return new ListFetchStrategy() {
        @Override
        public void fetch() {
          twitterApi.fetchConversations(id)
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(sortedCache::upsert,
                  onErrorFeedback(R.string.msg_tweet_not_download));
        }

        @Override
        public void fetch(Paging paging) {}
      };
    }
    throw new IllegalStateException();
  }

  private void subscribeWithCommonTask(Single<List<Status>> fetchingTask) {
    fetchingTask.observeOn(AndroidSchedulers.mainThread())
        .subscribe(sortedCache::upsert,
            onErrorFeedback(R.string.msg_tweet_not_download));
  }

  @NonNull
  private Consumer<Throwable> onErrorFeedback(@StringRes final int msg) {
    return throwable -> userFeedback.onNext(new UserFeedbackEvent(msg));
  }

  @Override
  public OnIffabItemSelectedListener getOnIffabItemSelectedListener(long selectedId) {
    return requestWorker.getOnIffabItemSelectedListener(selectedId);
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
