/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
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
import com.freshdigitable.udonroad.Utils;
import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.datastore.StatusReaction;
import com.freshdigitable.udonroad.datastore.StatusReactionImpl;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;
import com.freshdigitable.udonroad.subscriber.RequestWorker.Util;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.PublishProcessor;
import timber.log.Timber;
import twitter4j.Status;
import twitter4j.TwitterException;

/**
 * StatusRepository provides to access to status resources and receive response.
 *
 * Created by akihit on 2016/08/01.
 */
public class StatusRepository {
  private static final String TAG = StatusRepository.class.getSimpleName();
  private final TwitterApi twitterApi;
  private final TypedCache<Status> cache;
  private final PublishProcessor<UserFeedbackEvent> userFeedback;
  private final TypedCache<StatusReaction> configStore;

  @Inject
  public StatusRepository(@NonNull TwitterApi twitterApi,
                          @NonNull TypedCache<Status> statusStore,
                          @NonNull ConfigStore configStore,
                          @NonNull PublishProcessor<UserFeedbackEvent> userFeedback) {
    this.twitterApi = twitterApi;
    this.cache = statusStore;
    this.userFeedback = userFeedback;
    this.configStore = configStore;
  }

  Completable observeCreateFavorite(final long statusId) {
    return Completable.create(e ->
        Util.fetchToStore(twitterApi.createFavorite(statusId), cache, TypedCache::upsert,
            t -> {
              userFeedback.onNext(new UserFeedbackEvent(R.string.msg_fav_create_success));
              e.onComplete();
            },
            throwable -> {
              feedbackOnError(statusId, throwable, R.string.msg_fav_create_failed);
              e.onError(throwable);
            }));
  }

  public void createFavorite(long statusId) {
    observeCreateFavorite(statusId).subscribe(() -> {}, e -> {});
  }

  Completable observeRetweetStatus(final long statusId) {
    return Completable.create(e ->
        Util.fetchToStore(twitterApi.retweetStatus(statusId), cache, TypedCache::upsert,
            s -> {
              userFeedback.onNext(new UserFeedbackEvent(R.string.msg_rt_create_success));
              e.onComplete();
            },
            throwable -> {
              feedbackOnError(statusId, throwable, R.string.msg_rt_create_failed);
              e.onError(throwable);
            }));
  }

  public void retweetStatus(long statusId) {
    observeRetweetStatus(statusId).subscribe(() -> {}, e -> {});
  }

  public void destroyFavorite(long statusId) {
    fetchToStore(twitterApi.destroyFavorite(statusId), TypedCache::insert,
        R.string.msg_fav_delete_success, R.string.msg_fav_delete_failed);
  }

  public void destroyRetweet(long statusId) {
    fetchToStore(twitterApi.destroyStatus(statusId), TypedCache::insert,
        R.string.msg_rt_delete_success, R.string.msg_rt_delete_failed);
  }

  private void fetchToStore(Single<Status> fetchTask, BiConsumer<TypedCache<Status>, Status> storeTask,
                            @StringRes int successRes, @StringRes int failureRes) {
    Util.fetchToStore(fetchTask, cache, storeTask,
        s -> userFeedback.onNext(new UserFeedbackEvent(successRes)),
        throwable -> userFeedback.onNext(new UserFeedbackEvent(failureRes)));
  }

  private void feedbackOnError(long statusId, Throwable throwable, @StringRes int defaultId) {
    Timber.tag(TAG).e(throwable, "feedbackOnError: ");
    final int msg = findMessageByTwitterExeption(throwable, defaultId);
    if (msg == R.string.msg_already_fav) {
      updateStatusWithReaction(statusId, reaction -> reaction.setFavorited(true));
    } else if (msg == R.string.msg_already_rt) {
      updateStatusWithReaction(statusId, reaction -> reaction.setRetweeted(true));
    }
    final UserFeedbackEvent event = new UserFeedbackEvent(msg);
    userFeedback.onNext(event);
  }

  @StringRes
  private static int findMessageByTwitterExeption(@NonNull Throwable throwable, @StringRes int defaultId) {
    if (!(throwable instanceof TwitterException)) {
      return defaultId;
    }
    final TwitterException te = (TwitterException) throwable;
    final int statusCode = te.getStatusCode();
    final int errorCode = te.getErrorCode();
    if (statusCode == 403 && errorCode == 139) {
      return R.string.msg_already_fav;
    } else if (statusCode == 403 && errorCode == 327) {
      return R.string.msg_already_rt;
    }
    Timber.tag(TAG).d(throwable, "not registered exception: ");
    return defaultId;
  }

  private void updateStatusWithReaction(long statusId, Consumer<StatusReaction> action) {
    cache.open();
    final Status status = cache.find(statusId);
    if (status == null) {
      cache.close();
      return;
    }
    final StatusReactionImpl reaction = new StatusReactionImpl(Utils.getBindingStatus(status));
    try {
      action.accept(reaction); // XXX
    } catch (Exception e) {
    }
    configStore.open();
    configStore.insert(reaction);
    configStore.close();
    cache.close();
  }
}
