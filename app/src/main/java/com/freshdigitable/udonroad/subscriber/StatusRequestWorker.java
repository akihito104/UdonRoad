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
import android.support.annotation.StringRes;
import android.util.Log;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.Utils;
import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.datastore.StatusReaction;
import com.freshdigitable.udonroad.datastore.StatusReactionImpl;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;

import java.util.Arrays;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.PublishProcessor;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.TwitterException;

/**
 * StatusRequestWorker creates twitter request for status resources and subscribes its response
 * with user feedback.
 * <p>
 * Created by akihit on 2016/08/01.
 */
public class StatusRequestWorker implements RequestWorker {
  private static final String TAG = StatusRequestWorker.class.getSimpleName();
  private final TwitterApi twitterApi;
  private final TypedCache<Status> cache;
  private final PublishProcessor<UserFeedbackEvent> userFeedback;
  private final ConfigStore configStore;

  @Inject
  public StatusRequestWorker(@NonNull TwitterApi twitterApi,
                             @NonNull TypedCache<Status> statusStore,
                             @NonNull ConfigStore configStore,
                             @NonNull PublishProcessor<UserFeedbackEvent> userFeedback) {
    this.twitterApi = twitterApi;
    this.cache = statusStore;
    this.userFeedback = userFeedback;
    this.configStore = configStore;
  }

  @Override
  public OnIffabItemSelectedListener getOnIffabItemSelectedListener(long selectedId) {
    return item -> {
      final int itemId = item.getItemId();
      if (itemId == R.id.iffabMenu_main_fav) {
        if (!item.isChecked()) {
          createFavorite(selectedId);
        } else {
          destroyFavorite(selectedId);
        }
      } else if (itemId == R.id.iffabMenu_main_rt) {
        if (!item.isChecked()) {
          retweetStatus(selectedId);
        } else {
          destroyRetweet(selectedId);
        }
      } else if (itemId == R.id.iffabMenu_main_favRt) {
        Observable.concatDelayError(Arrays.asList(
            observeCreateFavorite(selectedId).toObservable(),
            observeRetweetStatus(selectedId).toObservable())
        ).subscribe(s -> {}, e -> {});
      }
    };
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
        .subscribe(createInsertAction(R.string.msg_fav_delete_success),
            onErrorFeedback(R.string.msg_fav_delete_failed));
  }

  public void destroyRetweet(long statusId) {
    twitterApi.destroyStatus(statusId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(createInsertAction(R.string.msg_rt_delete_success),
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
  private Consumer<Throwable> onErrorFeedback(@StringRes final int msg) {
    return throwable -> userFeedback.onNext(new UserFeedbackEvent(msg));
  }

  @NonNull
  private Consumer<Status> createUpsertAction(@StringRes int msg) {
    return s -> {
      cache.open();
      cache.upsert(s);
      cache.close();
      userFeedback.onNext(new UserFeedbackEvent(msg));
    };
  }

  @NonNull
  private Consumer<Status> createInsertAction(@StringRes int msg) {
    return s -> {
      cache.open();
      cache.insert(s);
      cache.close();
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
