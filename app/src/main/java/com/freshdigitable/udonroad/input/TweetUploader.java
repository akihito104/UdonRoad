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

package com.freshdigitable.udonroad.input;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;
import com.freshdigitable.udonroad.subscriber.RequestWorker;
import com.freshdigitable.udonroad.subscriber.UserFeedbackEvent;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.processors.PublishProcessor;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.auth.AccessToken;

/**
 * Created by akihit on 2017/12/23.
 */

class TweetUploader {
  private final TwitterApi twitterApi;
  private final TypedCache<Status> cache;
  private final PublishProcessor<UserFeedbackEvent> userFeedback;

  @Inject
  TweetUploader(@NonNull TwitterApi twitterApi,
                @NonNull TypedCache<Status> statusStore,
                @NonNull PublishProcessor<UserFeedbackEvent> userFeedback) {
    this.twitterApi = twitterApi;
    this.cache = statusStore;
    this.userFeedback = userFeedback;
  }

  void setAccessToken(AccessToken token) {
    twitterApi.setOAuthAccessToken(token);
  }

  Single<Status> observeUpdateStatus(String text) {
    return observeUpdateStatus(twitterApi.updateStatus(text));
  }

  Single<Status> observeUpdateStatus(StatusUpdate statusUpdate) {
    return observeUpdateStatus(twitterApi.updateStatus(statusUpdate));
  }

  Single<Status> observeUpdateStatus(Context context, StatusUpdate statusUpdate, List<Uri> media) {
    return observeUpdateStatus(twitterApi.updateStatus(context, statusUpdate, media));
  }

  private Single<Status> observeUpdateStatus(Single<Status> updateStatus) {
    return Single.create(e ->
        RequestWorker.Util.fetchToStore(updateStatus, cache, TypedCache::upsert,
            s -> {
              userFeedback.onNext(new UserFeedbackEvent(R.string.msg_updateStatus_success));
              e.onSuccess(s);
            },
            throwable -> {
              userFeedback.onNext(new UserFeedbackEvent(R.string.msg_updateStatus_failed));
              e.onError(throwable);
            }));
  }
}
