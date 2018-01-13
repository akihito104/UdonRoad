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
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.datastore.WritableSortedCache;
import com.freshdigitable.udonroad.timeline.fetcher.FetchQuery;
import com.freshdigitable.udonroad.timeline.fetcher.ListFetcher;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import io.reactivex.CompletableObserver;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.PublishProcessor;
import timber.log.Timber;
import twitter4j.Status;
import twitter4j.User;

/**
 * Created by akihit on 2017/03/31.
 */

public class StatusListRequestWorker implements ListRequestWorker<Status> {
  private final TwitterApi twitterApi;
  private final WritableSortedCache<Status> sortedCache;
  private final PublishProcessor<UserFeedbackEvent> userFeedback;
  private final StatusRequestWorker requestWorker;
  private Map<StoreType, Provider<ListFetcher<Status>>> listFetchers;
  private final TypedCache<User> userCache;
  private String storeName;

  @Inject
  public StatusListRequestWorker(@NonNull TwitterApi twitterApi,
                                 @NonNull TypedCache<User> userCache,
                                 @NonNull WritableSortedCache<Status> statusStore,
                                 @NonNull PublishProcessor<UserFeedbackEvent> userFeedback,
                                 @NonNull StatusRequestWorker requestWorker,
                                 Map<StoreType, Provider<ListFetcher<Status>>> listFetchers) {
    this.twitterApi = twitterApi;
    this.userCache = userCache;
    this.sortedCache = statusStore;
    this.userFeedback = userFeedback;
    this.requestWorker = requestWorker;
    this.listFetchers = listFetchers;
  }

  @Override
  public ListFetchStrategy getFetchStrategy(StoreType storeType, long id, String query) {
    this.storeName = storeType.nameWithSuffix(id, query);
    if (storeType == StoreType.CONVERSATION) {
      return new ListFetchStrategy() {
        @Override
        public void fetch() {
          twitterApi.fetchConversations(id)
              .observeOn(AndroidSchedulers.mainThread())
              .map(Collections::singleton)
              .flatMapCompletable(sortedCache::observeUpsert)
              .subscribe(new CompletableObserver() {
                @Override
                public void onSubscribe(Disposable d) {
                  sortedCache.open(storeName);
                }

                @Override
                public void onComplete() {
                  sortedCache.close();
                }

                @Override
                public void onError(Throwable e) {
                  try {
                    onErrorFeedback(R.string.msg_tweet_not_download).accept(e);
                  } catch (Exception e1) {
                    Timber.tag("StatusListRequestWorker").e(e1, "onError: ");
                  }
                  sortedCache.close();
                }
              });
        }

        @Override
        public void fetchNext() {}
      };
    }
    final Provider<ListFetcher<Status>> listFetcherProvider = listFetchers.get(storeType);
    final FetchQuery initQuery = getInitQuery(storeType, id, query);
    return new ListFetchStrategy() {
      @Override
      public void fetch() {
        fetchToStore(listFetcherProvider.get().fetchInit(initQuery));
      }

      @Override
      public void fetchNext() {
        final FetchQuery nextQuery = getNextQuery(storeType, id, query);
        if (nextQuery == null) {
          return;
        }
        fetchToStore(listFetcherProvider.get().fetchNext(nextQuery));
      }
    };
  }

  @Nullable
  private FetchQuery getInitQuery(StoreType storeType, long id, String query) {
    if (storeType == StoreType.HOME) {
      return null;
    } else if (storeType == StoreType.USER_FAV) {
      return new FetchQuery.Builder()
          .id(id)
          .build();
    } else if (storeType == StoreType.USER_HOME) {
      return new FetchQuery.Builder()
          .id(id)
          .build();
    } else if (storeType == StoreType.USER_MEDIA) {
      userCache.open();
      final String user = userCache.find(id).getScreenName();
      userCache.close();
      return new FetchQuery.Builder()
          .searchQuery(user)
          .build();
    } else if (storeType == StoreType.SEARCH) {
      return new FetchQuery.Builder()
          .searchQuery(query)
          .build();
    } else if (storeType == StoreType.LIST_TL) {
      return new FetchQuery.Builder()
          .id(id)
          .build();
    }
    throw new IllegalStateException("unsupported StoreType: " + storeType);
  }

  @Nullable
  private FetchQuery getNextQuery(StoreType storeType, long id, String query) {
    final String storeName = storeType.nameWithSuffix(id, query);
    if (storeType == StoreType.HOME) {
      return new FetchQuery.Builder()
          .lastPageCursor(getNextPageCursor(storeName))
          .build();
    } else if (storeType == StoreType.USER_FAV) {
      return new FetchQuery.Builder()
          .id(id)
          .lastPageCursor(getNextPageCursor(storeName))
          .build();
    } else if (storeType == StoreType.USER_HOME) {
      return new FetchQuery.Builder()
          .id(id)
          .lastPageCursor(getNextPageCursor(storeName))
          .build();
    } else if (storeType == StoreType.USER_MEDIA) {
      return checkHasNextCursor(storeName) ?
          new FetchQuery.Builder()
              .searchQuery(query)
              .lastPageCursor(getNextPageCursor(storeName))
              .build()
          : null;
    } else if (storeType == StoreType.SEARCH) {
      return checkHasNextCursor(storeName) ?
          new FetchQuery.Builder()
              .searchQuery(query)
              .lastPageCursor(getNextPageCursor(storeName))
              .build()
          : null;
    } else if (storeType == StoreType.LIST_TL) {
      return new FetchQuery.Builder()
          .id(id)
          .lastPageCursor(getNextPageCursor(storeName))
          .build();
    }
    throw new IllegalStateException("unsupported StoreType: " + storeType);
  }

  private boolean checkHasNextCursor(String storeName) {
    sortedCache.open(storeName);
    if (!sortedCache.hasNextPage()) {
      userFeedback.onNext(new UserFeedbackEvent(R.string.msg_no_next_page));
      sortedCache.close();
      return false;
    }
    return true;
  }

  private long getNextPageCursor(String storeName) {
    sortedCache.open(storeName);
    final long lastPageCursor = sortedCache.getLastPageCursor();
    sortedCache.close();
    return lastPageCursor;
  }

  private void fetchToStore(Single<? extends List<Status>> fetchingTask) {
    Util.fetchToStore(fetchingTask, sortedCache, storeName,
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
}
