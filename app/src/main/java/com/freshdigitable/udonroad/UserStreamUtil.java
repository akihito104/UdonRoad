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

import android.support.annotation.NonNull;
import android.util.Log;

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.PerspectivalStatusImpl;
import com.freshdigitable.udonroad.datastore.SortedCache;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.module.twitter.TwitterStreamApi;
import com.freshdigitable.udonroad.subscriber.UserFeedbackEvent;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subjects.PublishSubject;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.User;
import twitter4j.UserStreamAdapter;
import twitter4j.UserStreamListener;

import static com.freshdigitable.udonroad.Utils.getBindingStatus;

/**
 * UserStreamUtil transforms twitter stream to observable subscription.
 *
 * Created by akihit on 2016/06/07.
 */
public class UserStreamUtil {
  private static final String TAG = UserStreamUtil.class.getSimpleName();
  private final TwitterStreamApi streamApi;
  private final PublishSubject<UserFeedbackEvent> feedback;
  private long userId;
  private final AppSettingStore appSettings;
  private final SortedCache<Status> sortedStatusCache;
  private final TypedCache<Status> pool;
  private Subscription onReactionSubscription;

  @Inject
  public UserStreamUtil(@NonNull TwitterStreamApi streamApi,
                        @NonNull SortedCache<Status> sortedStatusCache,
                        @NonNull TypedCache<Status> pool,
                        @NonNull AppSettingStore appSettings,
                        @NonNull PublishSubject<UserFeedbackEvent> feedback) {
    this.streamApi = streamApi;
    this.feedback = feedback;
    this.appSettings = appSettings;
    this.sortedStatusCache = sortedStatusCache;
    this.pool = pool;
  }

  private boolean isConnectedUserStream = false;
  private PublishSubject<Status> statusPublishSubject;
  private Subscription onStatusSubscription;
  private PublishSubject<Long> deletionPublishSubject;
  private Subscription onDeletionSubscription;
  private PublishSubject<Status> reactionPublishSubject;

  public void connect(String storeName) {
    if (onStatusSubscription == null || onStatusSubscription.isUnsubscribed()) {
      statusPublishSubject = PublishSubject.create();
      onStatusSubscription = statusPublishSubject
          .buffer(500, TimeUnit.MILLISECONDS)
          .onBackpressureBuffer()
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(sortedStatusCache::upsert, onErrorAction);
    }
    if (onDeletionSubscription == null || onDeletionSubscription.isUnsubscribed()) {
      deletionPublishSubject = PublishSubject.create();
      onDeletionSubscription = deletionPublishSubject
          .buffer(500, TimeUnit.MILLISECONDS)
          .onBackpressureBuffer()
          .observeOn(AndroidSchedulers.mainThread())
          .flatMap(Observable::from)
          .subscribe(sortedStatusCache::delete, onErrorAction);
    }
    if (reactionPublishSubject == null || onReactionSubscription.isUnsubscribed()) {
      reactionPublishSubject = PublishSubject.create();
      onReactionSubscription = reactionPublishSubject
          .buffer(500, TimeUnit.MILLISECONDS)
          .onBackpressureBuffer()
          .observeOn(AndroidSchedulers.mainThread())
          .flatMap(Observable::from)
          .subscribe(pool::upsert, onErrorAction);
    }
    if (!isConnectedUserStream) {
      pool.open();
      sortedStatusCache.open(storeName);
      appSettings.open();
      streamApi.setOAuthAccessToken(appSettings.getCurrentUserAccessToken());
      streamApi.connectUserStream(statusListener);
      userId = appSettings.getCurrentUserId();
      isConnectedUserStream = true;
    }
  }

  public void disconnect() {
    if (isConnectedUserStream) {
      pool.close();
      appSettings.close();
      sortedStatusCache.close();
      streamApi.disconnectStreamListener();
      isConnectedUserStream = false;
    }

    if (statusPublishSubject != null) {
      statusPublishSubject.onCompleted();
    }
    if (onStatusSubscription != null && !onStatusSubscription.isUnsubscribed()) {
      onStatusSubscription.unsubscribe();
    }

    if (deletionPublishSubject != null) {
      deletionPublishSubject.onCompleted();
    }
    if (onDeletionSubscription != null && !onDeletionSubscription.isUnsubscribed()) {
      onDeletionSubscription.unsubscribe();
    }

    if (reactionPublishSubject != null) {
      reactionPublishSubject.onCompleted();
    }
    if (onReactionSubscription != null && !onReactionSubscription.isUnsubscribed()) {
      onReactionSubscription.unsubscribe();
    }
  }

  private final UserStreamListener statusListener = new UserStreamAdapter() {

    @Override
    public void onStatus(final Status status) {
      if (isRetweetOfMine(status)) {
        final PerspectivalStatusImpl perspectivalStatus = new PerspectivalStatusImpl(status);
        perspectivalStatus.getRetweetedStatus().getStatusReaction().setRetweeted(true);
        statusPublishSubject.onNext(perspectivalStatus);
      } else {
        statusPublishSubject.onNext(status);
        if (status.isRetweet() && status.getRetweetedStatus().getUser().getId() == userId) {
          feedback.onNext(
              new UserFeedbackEvent(R.string.msg_retweeted_by_someone,
                  status.getUser().getScreenName()));
        }
      }
    }

    private boolean isRetweetOfMine(Status status) {
      return status.isRetweet() && status.getUser().getId() == userId;
    }

    @Override
    public void onFavorite(User source, User target, Status favoritedStatus) {
      Log.d(TAG, "onFavorite: src> " + source.getScreenName() + ", tgt> " + target.getScreenName() +
          ", stt> " + favoritedStatus.getText());
      if (target.getId() == userId) {
        feedback.onNext(
            new UserFeedbackEvent(R.string.msg_faved_by_someone, source.getScreenName()));
      }
      final PerspectivalStatusImpl perspectivalStatus = new PerspectivalStatusImpl(favoritedStatus);
      getBindingStatus(perspectivalStatus).getStatusReaction().setFavorited(true);
      reactionPublishSubject.onNext(perspectivalStatus);
    }

    @Override
    public void onFollow(User source, User followedUser) {
      if (followedUser.getId() == userId) {
        feedback.onNext(
            new UserFeedbackEvent(R.string.msg_followed_by_someone, source.getScreenName()));
      }
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

  private final Action1<Throwable> onErrorAction = throwable -> Log.d(TAG, "error: " + throwable);
}
