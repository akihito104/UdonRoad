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

package com.freshdigitable.udonroad;

import android.util.Log;

import com.android.annotations.NonNull;
import com.freshdigitable.udonroad.datastore.SortedCache;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.UserStreamAdapter;
import twitter4j.UserStreamListener;

/**
 * UserStreamUtil transforms twitter stream to observable subscription.
 *
 * Created by akihit on 2016/06/07.
 */
public class UserStreamUtil {
  private static final String TAG = UserStreamUtil.class.getSimpleName();
  private final TwitterStreamApi streamApi;

  @Inject
  public UserStreamUtil(@NonNull TwitterStreamApi streamApi) {
    this.streamApi = streamApi;
  }

  private boolean isConnectedUserStream = false;
  private PublishSubject<Status> statusPublishSubject;
  private Subscription onStatusSubscription;
  private PublishSubject<Long> deletionPublishSubject;
  private Subscription onDeletionSubscription;

  public void connect(final SortedCache<Status> timelineStore) {
    if (onStatusSubscription == null || onStatusSubscription.isUnsubscribed()) {
      statusPublishSubject = PublishSubject.create();
      onStatusSubscription = statusPublishSubject
          .buffer(500, TimeUnit.MILLISECONDS)
          .onBackpressureBuffer()
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(new Action1<List<Status>>() {
            @Override
            public void call(List<Status> statuses) {
              timelineStore.upsert(statuses);
            }
          }, onErrorAction);
    }
    if (onDeletionSubscription == null || onDeletionSubscription.isUnsubscribed()) {
      deletionPublishSubject = PublishSubject.create();
      onDeletionSubscription = deletionPublishSubject
          .buffer(500, TimeUnit.MILLISECONDS)
          .onBackpressureBuffer()
          .observeOn(AndroidSchedulers.mainThread())
          .flatMap(new Func1<List<Long>, Observable<Long>>() {
            @Override
            public Observable<Long> call(List<Long> deletionIds) {
              return Observable.from(deletionIds);
            }
          })
          .subscribe(new Action1<Long>() {
            @Override
            public void call(Long deletionId) {
              timelineStore.delete(deletionId);
            }
          }, onErrorAction);
    }
    if (!isConnectedUserStream) {
      streamApi.loadAccessToken();
      streamApi.connectUserStream(statusListener);
      isConnectedUserStream = true;
    }
  }

  public void disconnect() {
    streamApi.disconnectStreamListener();
    isConnectedUserStream = false;

    statusPublishSubject.onCompleted();
    if (onStatusSubscription != null && !onStatusSubscription.isUnsubscribed()) {
      onStatusSubscription.unsubscribe();
    }
    if (onDeletionSubscription != null && !onDeletionSubscription.isUnsubscribed()) {
      onDeletionSubscription.unsubscribe();
    }
  }

  private final UserStreamListener statusListener = new UserStreamAdapter() {

    @Override
    public void onStatus(final Status status) {
      statusPublishSubject.onNext(status);
    }

    @Override
    public void onDeletionNotice(final StatusDeletionNotice statusDeletionNotice) {
      Log.d(TAG, statusDeletionNotice.toString());
      deletionPublishSubject.onNext(statusDeletionNotice.getStatusId());
    }

    @Override
    public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
      Log.d(TAG, "onTrackLimitationNotice: " + numberOfLimitedStatuses);
    }

    @Override
    public void onScrubGeo(long userId, long upToStatusId) {
      Log.d(TAG, "onScrubGeo: " + userId + ", " + upToStatusId);
    }

    @Override
    public void onStallWarning(StallWarning warning) {
      Log.d(TAG, "onStallWarning: " + warning.toString());
    }

    @Override
    public void onException(Exception ex) {
      Log.d(TAG, "onException: " + ex.toString());
    }
  };

  private final Action1<Throwable> onErrorAction = new Action1<Throwable>() {
    @Override
    public void call(Throwable throwable) {
      Log.d(TAG, "error: " + throwable);
    }
  };
}
