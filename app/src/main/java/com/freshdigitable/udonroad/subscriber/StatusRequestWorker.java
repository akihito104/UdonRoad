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
import android.util.Log;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.datastore.BaseOperation;
import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.datastore.StatusReactionImpl;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.TwitterException;

/**
 * StatusRequestWorker creates twitter request for status resources and subscribes its response
 * with user feedback.
 *
 * Created by akihit on 2016/08/01.
 */
public class StatusRequestWorker<T extends BaseOperation<Status>>
    extends RequestWorkerBase<T> {
  public static final String TAG = StatusRequestWorker.class.getSimpleName();
  private final ConfigStore configStore;

  @Inject
  public StatusRequestWorker(@NonNull TwitterApi twitterApi,
                             @NonNull T statusStore,
                             @NonNull ConfigStore configStore,
                             @NonNull UserFeedbackSubscriber userFeedback) {
    super(twitterApi, statusStore, userFeedback);
    this.configStore = configStore;
  }

  @Override
  public void open() {
    super.open();
    configStore.open();
  }

  @Override
  public void open(@NonNull String name) {
    super.open(name);
    configStore.open();
  }

  @Override
  public void close() {
    super.close();
    configStore.close();
  }

  public void fetchHomeTimeline() {
    twitterApi.getHomeTimeline()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createListUpsertAction(),
            onErrorFeedback(R.string.msg_tweet_not_download));
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

  public Observable<Status> observeCreateFavorite(final long statusId) {
    return twitterApi.createFavorite(statusId)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(createUpsertAction())
        .doOnError(new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            final int msg = findMessageByTwitterExeption(throwable);
            if (msg == R.string.msg_already_fav) {
              updateStatus();
            }
            userFeedback.offerEvent(msg > 0 ? msg : R.string.msg_fav_create_failed);
          }

          private void updateStatus() {
            final Status status = cache.find(statusId);
            if (status == null) {
              return;
            }
            final Status bindingStatus = status.isRetweet() ? status.getRetweetedStatus() : status;
            final StatusReactionImpl reaction = new StatusReactionImpl(bindingStatus);
            reaction.setFavorited(true);
            configStore.forceUpsert(reaction);
          }
        })
        .doOnCompleted(onCompleteFeedback(R.string.msg_fav_create_success));
  }

  public void createFavorite(long statusId) {
    observeCreateFavorite(statusId)
        .subscribe(StatusRequestWorker.<Status>nopSubscriber());
  }

  public Observable<Status> observeRetweetStatus(final long statusId) {
    return twitterApi.retweetStatus(statusId)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(createUpsertAction())
        .doOnError(new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            final int msg = findMessageByTwitterExeption(throwable);
            if (msg == R.string.msg_already_rt) {
              updateStatus();
            }
            userFeedback.offerEvent(msg > 0 ? msg : R.string.msg_rt_create_failed);
          }

          private void updateStatus() {
            final Status status = cache.find(statusId);
            if (status == null) {
              return;
            }
            final Status bindingStatus = status.isRetweet() ? status.getRetweetedStatus() : status;
            final StatusReactionImpl reaction = new StatusReactionImpl(bindingStatus);
            reaction.setRetweeted(true);
            configStore.forceUpsert(reaction);
          }
        })
        .doOnCompleted(onCompleteFeedback(R.string.msg_rt_create_success));
  }

  public void retweetStatus(long statusId) {
    observeRetweetStatus(statusId)
        .subscribe(StatusRequestWorker.<Status>nopSubscriber());
  }

  public void destroyFavorite(long statusId) {
    twitterApi.destroyFavorite(statusId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            new Action1<Status>() {
              @Override
              public void call(Status status) {
                cache.forceUpsert(status);
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
                cache.forceUpsert(status);
              }
            },
            onErrorFeedback(R.string.msg_rt_delete_failed),
            onCompleteFeedback(R.string.msg_rt_delete_success));
  }

  @NonNull
  private Action1<List<Status>> createListUpsertAction() {
    return new Action1<List<Status>>() {
      @Override
      public void call(List<Status> statuses) {
        cache.upsert(statuses);
      }
    };
  }

  @NonNull
  private Action1<Status> createUpsertAction() {
    return new Action1<Status>() {
      @Override
      public void call(Status statuses) {
        cache.upsert(statuses);
      }
    };
  }

  public static <T> Subscriber<T> nopSubscriber() {
    return new Subscriber<T>() {
      @Override
      public void onCompleted() {
      }

      @Override
      public void onError(Throwable e) {
      }

      @Override
      public void onNext(T o) {
      }
    };
  }

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
    Log.d(TAG, "not registered exception: ", throwable);
    return 0;
  }
}
