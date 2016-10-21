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

import com.freshdigitable.udonroad.FeedbackSubscriber;
import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.datastore.BaseOperation;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;

import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
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
  private final FeedbackSubscriber userFeedback;

  @SuppressWarnings("unused")
  public TimelineSubscriber(@NonNull TwitterApi twitterApi,
                            @NonNull T statusStore) {
    this(twitterApi, statusStore, new FeedbackSubscriber.LogFeedback());
  }

  public TimelineSubscriber(@NonNull TwitterApi twitterApi,
                            @NonNull T statusStore,
                            @NonNull FeedbackSubscriber userFeedback) {
    this.twitterApi = twitterApi;
    this.statusStore = statusStore;
    this.userFeedback = userFeedback;
  }

  public void fetchHomeTimeline() {
    twitterApi.getHomeTimeline()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createListUpsertAction(),
            userFeedback.onErrorDefault(R.string.msg_tweet_not_download));
  }

  public void fetchHomeTimeline(Paging paging) {
    twitterApi.getHomeTimeline(paging)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createListUpsertAction(),
            userFeedback.onErrorDefault(R.string.msg_tweet_not_download));
  }

  public void fetchHomeTimeline(long userId) {
    twitterApi.getUserTimeline(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createListUpsertAction(),
            userFeedback.onErrorDefault(R.string.msg_tweet_not_download));
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
            userFeedback.onErrorDefault(R.string.msg_tweet_not_download));
  }

  public void fetchFavorites(long userId) {
    twitterApi.getFavorites(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createListUpsertAction(),
            userFeedback.onErrorDefault(R.string.msg_tweet_not_download));
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
            userFeedback.onErrorDefault(R.string.msg_tweet_not_download));
  }

  public void createFavorite(long statusId) {
    twitterApi.createFavorite(statusId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createUpsertAction(),
            new Action1<Throwable>() {
              @Override
              public void call(Throwable throwable) {
                if (throwable instanceof TwitterException) {
                  final TwitterException te = (TwitterException) throwable;
                  final int statusCode = te.getStatusCode();
                  final int errorCode = te.getErrorCode();
                  if (statusCode == 403 && errorCode == 139) {
                    userFeedback.onErrorDefault(R.string.msg_already_fav).call(throwable);
                  }
                } else {
                  userFeedback.onErrorDefault(R.string.msg_fav_create_failed).call(throwable);
                }
              }
            },
            userFeedback.onCompleteDefault(R.string.msg_fav_create_success));
  }

  public void retweetStatus(long statusId) {
    twitterApi.retweetStatus(statusId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            createUpsertAction(),
            userFeedback.onErrorDefault(R.string.msg_rt_create_failed),
            userFeedback.onCompleteDefault(R.string.msg_rt_create_success));
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
            userFeedback.onErrorDefault(R.string.msg_fav_delete_failed),
            userFeedback.onCompleteDefault(R.string.msg_fav_delete_success));
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
            userFeedback.onErrorDefault(R.string.msg_rt_delete_failed),
            userFeedback.onCompleteDefault(R.string.msg_rt_delete_success));
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
}
