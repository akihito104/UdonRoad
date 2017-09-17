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
import android.util.Log;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.datastore.WritableSortedCache;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.CompletableObserver;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.PublishProcessor;
import twitter4j.Paging;
import twitter4j.Query;
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
  private final TypedCache<User> userCache;
  private String storeName;

  @Inject
  public StatusListRequestWorker(@NonNull TwitterApi twitterApi,
                                 @NonNull TypedCache<User> userCache,
                                 @NonNull WritableSortedCache<Status> statusStore,
                                 @NonNull PublishProcessor<UserFeedbackEvent> userFeedback,
                                 @NonNull StatusRequestWorker requestWorker) {
    this.twitterApi = twitterApi;
    this.userCache = userCache;
    this.sortedCache = statusStore;
    this.userFeedback = userFeedback;
    this.requestWorker = requestWorker;
  }

  @Override
  public ListFetchStrategy getFetchStrategy(StoreType storeType, long id, String query) {
    this.storeName = storeType.nameWithSuffix(id, query);

    if (storeType == StoreType.HOME) {
      return new ListFetchStrategy() {
        @Override
        public void fetch() {
          fetchToStore(twitterApi.getHomeTimeline());
        }

        @Override
        public void fetchNext() {
          fetchToStore(twitterApi.getHomeTimeline(getNextPage()));
        }
      };
    } else if (storeType == StoreType.USER_HOME) {
      return new ListFetchStrategy() {
        @Override
        public void fetch() {
          fetchToStore(twitterApi.getUserTimeline(id));
        }

        @Override
        public void fetchNext() {
          fetchToStore(twitterApi.getUserTimeline(id, getNextPage()));
        }
      };
    } else if (storeType == StoreType.USER_FAV) {
      return new ListFetchStrategy() {
        @Override
        public void fetch() {
          fetchToStore(twitterApi.getFavorites(id));
        }

        @Override
        public void fetchNext() {
          fetchToStore(twitterApi.getFavorites(id, getNextPage()));
        }
      };
    } else if (storeType == StoreType.CONVERSATION) {
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
                    Log.e("StatusListRequestWorker", "onError: ", e1);
                  }
                  sortedCache.close();
                }
              });
        }

        @Override
        public void fetchNext() {}
      };
    } else if (storeType == StoreType.USER_MEDIA) {
      userCache.open();
      final String user = userCache.find(id).getScreenName();
      userCache.close();
      return new ListFetchStrategy() {
        @Override
        public void fetch() {
          fetchToStore(twitterApi.fetchSearch(getQuery()));
        }

        @Override
        public void fetchNext() {
          sortedCache.open(storeName);
          if (!sortedCache.hasNextPage()) {
            userFeedback.onNext(new UserFeedbackEvent(R.string.msg_no_next_page));
            sortedCache.close();
            return;
          }
          final Query query = getQuery().maxId(sortedCache.getLastPageCursor());
          sortedCache.close();
          fetchToStore(twitterApi.fetchSearch(query));
        }

        private Query getQuery() {
          return new Query("from:" + user + " filter:media exclude:retweets")
              .count(20)
              .resultType(Query.RECENT);
        }
      };
    } else if (storeType == StoreType.SEARCH) {
      return new ListFetchStrategy() {
        @Override
        public void fetch() {
          fetchToStore(twitterApi.fetchSearch(getQuery()));
        }

        @Override
        public void fetchNext() {
          sortedCache.open(storeName);
          if (!sortedCache.hasNextPage()) {
            userFeedback.onNext(new UserFeedbackEvent(R.string.msg_no_next_page));
            sortedCache.close();
            return;
          }
          final Query q = getQuery().maxId(sortedCache.getLastPageCursor());
          sortedCache.close();
          fetchToStore(twitterApi.fetchSearch(q));
        }

        private Query getQuery() {
          return new Query(query + " exclude:retweets")
              .count(20)
              .resultType(Query.RECENT);
        }
      };
    } else if (storeType == StoreType.LIST_TL) {
      return new ListFetchStrategy() {
        @Override
        public void fetch() {
          fetchToStore(twitterApi.fetchUserListsStatuses(id, new Paging(1, 20)));
        }

        @Override
        public void fetchNext() {
          fetchToStore(twitterApi.fetchUserListsStatuses(id, getNextPage()));
        }
      };
    }
    throw new IllegalStateException();
  }

  private void fetchToStore(Single<List<Status>> fetchingTask) {
    Util.fetchToStore(fetchingTask, sortedCache, storeName,
        onErrorFeedback(R.string.msg_tweet_not_download));
  }

  private Paging getNextPage() {
    sortedCache.open(storeName);
    final long lastPageCursor = sortedCache.getLastPageCursor();
    sortedCache.close();
    return new Paging(1, 20, 1, lastPageCursor);
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
