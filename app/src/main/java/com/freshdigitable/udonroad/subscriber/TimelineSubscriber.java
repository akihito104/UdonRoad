/*
 * Copyright (c) 2016. Matsuda, Akihit (akihito104)
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
import com.freshdigitable.udonroad.datastore.BaseOperation;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.TwitterException;

/**
 * TimelineSubscriber creates twitter request for status resources and subscribes its response
 * with user feedback.

 * Created by akihit on 2016/08/01.
 */
public class TimelineSubscriber<T extends BaseOperation<Status>> {
  public static final String TAG = TimelineSubscriber.class.getSimpleName();
  private final TwitterApi twitterApi;
  private final T statusStore;
//  private final FeedbackAction userFeedback;
  private final ConcurrentLinkedQueue<Integer> queue = new ConcurrentLinkedQueue<>();
  private Subscription feedbackSubscription;

  @SuppressWarnings("unused")
  public TimelineSubscriber(@NonNull TwitterApi twitterApi,
                            @NonNull T statusStore) {
    this(twitterApi, statusStore, new FeedbackAction.LogFeedback());
  }

  public TimelineSubscriber(@NonNull TwitterApi twitterApi,
                            @NonNull T statusStore,
                            @NonNull final FeedbackAction userFeedback) {
    this.twitterApi = twitterApi;
    this.statusStore = statusStore;
//    this.userFeedback = userFeedback;
    feedbackSubscription = Observable.interval(1, TimeUnit.SECONDS)
        .onBackpressureBuffer()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<Long>() {
          @Override
          public void call(Long time) {
            final Integer msg = queue.poll();
            if (msg == null) {
              return;
            }
            userFeedback.onCompleteDefault(msg).call();
          }
        });
  }

  public void close() {
    queue.clear();
    feedbackSubscription.unsubscribe();
  }

  public void fetchHomeTimeline() {
    twitterApi.getHomeTimeline()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createListUpsertAction(),
            onErrorFeedback(R.string.msg_tweet_not_download));
  }

  private Action1<Throwable> onErrorFeedback(@StringRes final int msg) {
    return new Action1<Throwable>() {
      @Override
      public void call(Throwable throwable) {
        queue.offer(msg);
      }
    };
  }

  public void fetchHomeTimeline(Paging paging) {
    twitterApi.getHomeTimeline(paging)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createListUpsertAction(),
            onErrorFeedback(R.string.msg_tweet_not_download));
  }

  public void fetchHomeTimeline(long userId) {
    twitterApi.getUserTimeline(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createListUpsertAction(),
            onErrorFeedback(R.string.msg_tweet_not_download));
  }

  public void fetchHomeTimeline(long userId, @Nullable Paging paging) {
    if (paging == null) {
      fetchHomeTimeline(userId);
      return;
    }
    twitterApi.getUserTimeline(userId, paging)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createListUpsertAction(),
            onErrorFeedback(R.string.msg_tweet_not_download));
  }

  public void fetchFavorites(long userId) {
    twitterApi.getFavorites(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createListUpsertAction(),
            onErrorFeedback(R.string.msg_tweet_not_download));
  }

  public void fetchFavorites(long userId, @Nullable Paging paging) {
    if (paging == null) {
      fetchFavorites(userId);
      return;
    }
    twitterApi.getFavorites(userId, paging)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createListUpsertAction(),
            onErrorFeedback(R.string.msg_tweet_not_download));
  }

  public Observable<Status> observeCreateFavorite(long statusId) {
    return twitterApi.createFavorite(statusId)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(createUpsertAction())
        .doOnError(new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            final int msg = findMessageByTwitterExeption(throwable);
            queue.offer(msg > 0 ? msg : R.string.msg_fav_create_failed);
          }
        })
        .doOnCompleted(onCompleteFeedback(R.string.msg_fav_create_success));
  }

  private Action0 onCompleteFeedback(@StringRes final int msg) {
    return new Action0() {
      @Override
      public void call() {
        queue.offer(msg);
      }
    };
  }

  public void createFavorite(long statusId) {
    subscribeWithEmpty(observeCreateFavorite(statusId));
  }

  public Observable<Status> observeRetweetStatus(long statusId) {
    return twitterApi.retweetStatus(statusId)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(createUpsertAction())
        .doOnError(new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            final int msg = findMessageByTwitterExeption(throwable);
            queue.offer(msg > 0 ? msg : R.string.msg_rt_create_failed);
          }
        })
        .doOnCompleted(onCompleteFeedback(R.string.msg_rt_create_success));
  }

  public void retweetStatus(long statusId) {
    subscribeWithEmpty(observeRetweetStatus(statusId));
  }

  public void destroyFavorite(long statusId) {
    twitterApi.destroyFavorite(statusId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            new Action1<Status>() {
              @Override
              public void call(Status status) {
                statusStore.forceUpsert(status);
              }
            },
            onErrorFeedback(R.string.msg_fav_delete_failed),
            onCompleteFeedback(R.string.msg_fav_delete_success));
  }

  public void destroyRetweet(long statusId) {
    twitterApi.destroyStatus(statusId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            new Action1<Status>() {
              @Override
              public void call(Status status) {
                statusStore.forceUpsert(status);
              }
            },
            onErrorFeedback(R.string.msg_rt_delete_failed),
            onCompleteFeedback(R.string.msg_rt_delete_success));
  }

  public T getStatusStore() {
    return statusStore;
  }

  @NonNull
  private Action1<List<Status>> createListUpsertAction() {
    return new Action1<List<Status>>() {
      @Override
      public void call(List<Status> statuses) {
        statusStore.upsert(statuses);
      }
    };
  }

  @NonNull
  private Action1<Status> createUpsertAction() {
    return new Action1<Status>() {
      @Override
      public void call(Status statuses) {
        statusStore.upsert(statuses);
      }
    };
  }

  public static <T> void subscribeWithEmpty(Observable<T> observable) {
    observable.subscribe(new Action1<T>() {
      @Override
      public void call(T t) {
      }
    }, new Action1<Throwable>() {
      @Override
      public void call(Throwable throwable) {
      }
    });
  }

//  private static void feedbackErrorMessageWithDefault(@NonNull FeedbackAction userFeedback,
//                                                      @NonNull Throwable throwable,
//                                                      @StringRes int defaultMes) {
//    final int msg = findMessageByTwitterExeption(throwable);
//    userFeedback.onErrorDefault(msg > 0 ? msg : defaultMes).call(throwable);
//  }

  @StringRes
  private static int findMessageByTwitterExeption(@NonNull Throwable throwable) {
    if (!(throwable instanceof TwitterException)) {
      return 0;
    }
    final TwitterException te = (TwitterException) throwable;
    final int statusCode = te.getStatusCode();
    final int errorCode = te.getErrorCode();
    if (statusCode == 403 && errorCode == 139) {
      return R.string.msg_already_fav;
    } else if (statusCode == 403 && errorCode == 327) {
      return R.string.msg_already_rt;
    }
    return 0;
  }
}
