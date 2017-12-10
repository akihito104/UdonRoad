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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.repository.ImageQuery;
import com.freshdigitable.udonroad.repository.ImageRepository;
import com.freshdigitable.udonroad.subscriber.StatusRequestWorker;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.TwitterAPIConfiguration;
import twitter4j.User;

/**
 * Created by akihit on 2017/12/02.
 */

class TweetInputViewModel implements LifecycleObserver {
  private final StatusRequestWorker statusRequestWorker;
  private final AppSettingStore appSettings;
  private final TypedCache<Status> statusCache;
  private final ImageRepository imageRepository;

  private TweetInputModel model = new TweetInputModel();
  private PublishSubject<List<Uri>> mediaEmitter;

  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  public void onStart() {
    appSettings.open();
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  public void onStop() {
    appSettings.close();
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  public void onDestroy() {
    if (mediaEmitter != null && mediaEmitter.hasObservers()) {
      mediaEmitter.onComplete();
    }
  }

  @Inject
  TweetInputViewModel(AppSettingStore appSettings, TypedCache<Status> statusCache,
                      ImageRepository imageRepository, StatusRequestWorker statusRequestWorker) {
    this.appSettings = appSettings;
    this.statusCache = statusCache;
    this.imageRepository = imageRepository;
    this.statusRequestWorker = statusRequestWorker;
    mediaEmitter = PublishSubject.create();
  }

  void setReplyToStatusId(long inReplyToStatusId) {
    statusCache.open();
    final Status inReplyTo = statusCache.find(inReplyToStatusId);
    if (inReplyTo != null) {
      model.setReplyEntity(ReplyEntity.create(inReplyTo, appSettings.getCurrentUserId()));
    }
    statusCache.close();
  }

  int getUrlLength() {
    final TwitterAPIConfiguration twitterAPIConfig = appSettings.getTwitterAPIConfig();
    return twitterAPIConfig != null ? twitterAPIConfig.getShortURLLengthHttps() : 0;
  }

  void addQuoteId(long quoteId) {
    model.addQuoteId(quoteId);
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

  Single<Status> createSendObservable(Context context, String sendingText) {
    if (!model.isStatusUpdateNeeded()) {
      return statusRequestWorker.observeUpdateStatus(sendingText);
    }
    final StatusUpdate statusUpdate = createStatusUpdate(sendingText);
    return model.hasMedia() ? statusRequestWorker.observeUpdateStatus(context, statusUpdate, model.getMedia())
        : statusRequestWorker.observeUpdateStatus(statusUpdate);
  }

  private StatusUpdate createStatusUpdate(@NonNull String sendingText) {
    final StringBuilder s = new StringBuilder(sendingText);
    statusCache.open();
    for (long q : model.getQuoteIds()) {
      final Status status = statusCache.find(q);
      if (status != null) {
        s.append(" https://twitter.com/")
            .append(status.getUser().getScreenName()).append("/status/").append(q);
      }
    }
    statusCache.close();
    final StatusUpdate statusUpdate = new StatusUpdate(s.toString());
    if (model.hasReplyEntity()) {
      statusUpdate.setInReplyToStatusId(model.getReplyEntity().getInReplyToStatusId());
    }
    return statusUpdate;
  }

  Observable<List<Uri>> observeMedia() {
    return mediaEmitter.publish();
  }

  void addAllMedia(Collection<Uri> uris) {
    model.addMediaAll(uris);
    mediaEmitter.onNext(model.getMedia());
  }

  void removeMedia(Uri uri) {
    model.removeMedia(uri);
    mediaEmitter.onNext(model.getMedia());
  }

  void clear() {
    model.clear();
    mediaEmitter.onNext(model.getMedia());
  }

  boolean isCleared() {
    return model.isCleared();
  }

  boolean hasQuoteStatus() {
    return model.hasQuoteId();
  }

  boolean hasReplyEntity() {
    return model.hasReplyEntity();
  }

  String createReplyString() {
    return model.hasReplyEntity() ? model.getReplyEntity().createReplyString() : "";
  }

  void onSaveInstanceState(Bundle outState) {
    outState.putParcelable("ss_model", model);
  }

  void onViewStateRestored(@Nullable Bundle savedInstanceState) {
    if (savedInstanceState == null) {
      return;
    }
    model = savedInstanceState.getParcelable("ss_model");
  }
}
