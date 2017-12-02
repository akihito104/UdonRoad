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

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.repository.ImageQuery;
import com.freshdigitable.udonroad.repository.ImageRepository;
import com.freshdigitable.udonroad.subscriber.StatusRequestWorker;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Single;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.TwitterAPIConfiguration;
import twitter4j.User;

/**
 * Created by akihit on 2017/12/02.
 */

class TweetInputUseCase implements LifecycleObserver {
  private final StatusRequestWorker statusRequestWorker;
  private final AppSettingStore appSettings;
  private final TypedCache<Status> statusCache;
  private final ImageRepository imageRepository;

  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  public void onStart() {
    appSettings.open();
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  public void onStop() {
    appSettings.close();
  }

  @Inject
  TweetInputUseCase(AppSettingStore appSettings, TypedCache<Status> statusCache,
                    ImageRepository imageRepository, StatusRequestWorker statusRequestWorker) {
    this.appSettings = appSettings;
    this.statusCache = statusCache;
    this.imageRepository = imageRepository;
    this.statusRequestWorker = statusRequestWorker;
  }

  TweetInputFragment.ReplyEntity getReplyEntity(long inReplyToStatusId) {
    statusCache.open();
    final Status inReplyTo = statusCache.find(inReplyToStatusId);
    TweetInputFragment.ReplyEntity replyEntity=null;
    if (inReplyTo != null) {
      replyEntity = TweetInputFragment.ReplyEntity.create(inReplyTo, appSettings.getCurrentUserId());
    }
    statusCache.close();
    return replyEntity;
  }

  int getUrlLength() {
    final TwitterAPIConfiguration twitterAPIConfig = appSettings.getTwitterAPIConfig();
    return twitterAPIConfig != null ? twitterAPIConfig.getShortURLLengthHttps() : 0;
  }

  StatusUpdate createStatusUpdate(@NonNull String sendingText, @NonNull List<Long> quoteStatusIds,
                                  @Nullable TweetInputFragment.ReplyEntity replyEntity) {
    final StringBuilder s = new StringBuilder(sendingText);
    statusCache.open();
    for (long q : quoteStatusIds) {
      final Status status = statusCache.find(q);
      if (status == null) {
        continue;
      }
      s.append(" https://twitter.com/")
          .append(status.getUser().getScreenName()).append("/status/").append(q);
    }
    statusCache.close();
    final StatusUpdate statusUpdate = new StatusUpdate(s.toString());
    if (replyEntity != null) {
      statusUpdate.setInReplyToStatusId(replyEntity.inReplyToStatusId);
    }
    return statusUpdate;
  }

  Single<Status> createSendTask(String sendingText) {
    return statusRequestWorker.observeUpdateStatus(sendingText);
  }

  Single<Status> createSendTask(StatusUpdate update) {
    return statusRequestWorker.observeUpdateStatus(update);
  }

  Single<Status> createSendTask(Context context, StatusUpdate statusUpdate, List<Uri> media) {
    return statusRequestWorker.observeUpdateStatus(context, statusUpdate, media);
  }

  Observable<User> observeCurrentUser() {
    return appSettings.observeCurrentUser();
  }

  Observable<Drawable> queryImage(ImageQuery query) {
    return imageRepository.queryImage(query);
  }

  Observable<Drawable> queryImage(Single<ImageQuery> query) {
    return imageRepository.queryImage(query);
  }
}
