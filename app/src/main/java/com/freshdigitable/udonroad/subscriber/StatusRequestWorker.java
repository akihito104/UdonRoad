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
import com.freshdigitable.udonroad.Utils;
import com.freshdigitable.udonroad.datastore.BaseOperation;
import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.datastore.StatusReaction;
import com.freshdigitable.udonroad.datastore.StatusReactionImpl;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.PublishProcessor;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.TwitterException;

/**
 * StatusRequestWorker creates twitter request for status resources and subscribes its response
 * with user feedback.
 *
 * Created by akihit on 2016/08/01.
 */
public class StatusRequestWorker<T extends BaseOperation<Status>>
    extends RequestWorkerBase<T> {
  private static final String TAG = StatusRequestWorker.class.getSimpleName();
  private final ConfigStore configStore;

  @Inject
  public StatusRequestWorker(@NonNull TwitterApi twitterApi,
                             @NonNull T statusStore,
                             @NonNull ConfigStore configStore,
                             @NonNull PublishProcessor<UserFeedbackEvent> userFeedback) {
    super(twitterApi, statusStore, userFeedback);
    this.configStore = configStore;
  }

  private boolean opened = false;

  @Override
  public void open() {
    super.open();
    configStore.open();
    opened = true;
  }

  @Override
  public void open(@NonNull String name) {
    super.open(name);
    configStore.open();
    opened = true;
  }

  @Override
  public void close() {
    if (opened) {
      super.close();
      configStore.close();
      opened = false;
    }
  }

  public boolean isOpened() {
    return opened;
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

  public Single<Status> observeCreateFavorite(final long statusId) {
    return twitterApi.createFavorite(statusId)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(createUpsertAction(R.string.msg_fav_create_success))
        .doOnError(throwable -> {
          final int msg = findMessageByTwitterExeption(throwable);
          if (msg == R.string.msg_already_fav) {
            updateStatusWithReaction(statusId,
                reaction -> reaction.setFavorited(true));
          }
          final UserFeedbackEvent event = new UserFeedbackEvent(msg > 0
              ? msg : R.string.msg_fav_create_failed);
          userFeedback.onNext(event);
        });
  }

  public void createFavorite(long statusId) {
    observeCreateFavorite(statusId).subscribe((s, e) -> {});
  }

  public Single<Status> observeRetweetStatus(final long statusId) {
    return twitterApi.retweetStatus(statusId)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(createUpsertAction(R.string.msg_rt_create_success))
        .doOnError(throwable -> {
          final int msg = findMessageByTwitterExeption(throwable);
          if (msg == R.string.msg_already_rt) {
            updateStatusWithReaction(statusId,
                reaction -> reaction.setRetweeted(true));
          }
          final UserFeedbackEvent event = new UserFeedbackEvent(msg > 0 ?
              msg : R.string.msg_rt_create_failed);
          userFeedback.onNext(event);
        });
  }

  public void retweetStatus(long statusId) {
    observeRetweetStatus(statusId).subscribe((s, e) -> {});
  }

  public void destroyFavorite(long statusId) {
    twitterApi.destroyFavorite(statusId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(status -> {
              cache.insert(status);
              userFeedback.onNext(new UserFeedbackEvent(R.string.msg_fav_delete_success));
            },
            onErrorFeedback(R.string.msg_fav_delete_failed));
  }

  public void destroyRetweet(long statusId) {
    twitterApi.destroyStatus(statusId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(status -> {
              cache.insert(status);
              userFeedback.onNext(new UserFeedbackEvent(R.string.msg_rt_delete_success));
            },
            onErrorFeedback(R.string.msg_rt_delete_failed));
  }

  public Single<Status> observeUpdateStatus(String text) {
    return twitterApi.updateStatus(text)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(createUpsertAction(R.string.msg_updateStatus_success))
        .doOnError(onErrorFeedback(R.string.msg_updateStatus_failed));
  }

  public Single<Status> observeUpdateStatus(StatusUpdate statusUpdate) {
    return twitterApi.updateStatus(statusUpdate)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(createUpsertAction(R.string.msg_updateStatus_success))
        .doOnError(onErrorFeedback(R.string.msg_updateStatus_failed));
  }

  @NonNull
  private Consumer<List<Status>> createListUpsertAction() {
    return statuses -> cache.upsert(statuses);
  }

  @NonNull
  private Consumer<Status> createUpsertAction() {
    return statuses -> cache.upsert(statuses);
  }

  @NonNull
  private Consumer<Status> createUpsertAction(@StringRes int msg) {
    return s -> {
      cache.upsert(s);
      userFeedback.onNext(new UserFeedbackEvent(msg));
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

  private void updateStatusWithReaction(long statusId, Consumer<StatusReaction> action) {
    final Status status = cache.find(statusId);
    if (status == null) {
      return;
    }
    final StatusReactionImpl reaction = new StatusReactionImpl(Utils.getBindingStatus(status));
    try {
      action.accept(reaction); // XXX
    } catch (Exception e) {
    }
    configStore.insert(reaction);
  }

  public void fetchConversations(long statusId) {
    twitterApi.fetchConversations(statusId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(createUpsertAction(),
            onErrorFeedback(R.string.msg_tweet_not_download));
  }
}
