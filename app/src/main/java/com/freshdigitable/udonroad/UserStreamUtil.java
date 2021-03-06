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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.PerspectivalStatusImpl;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.datastore.WritableSortedCache;
import com.freshdigitable.udonroad.module.CurrentTimeProvider;
import com.freshdigitable.udonroad.module.twitter.TwitterStreamApi;
import com.freshdigitable.udonroad.subscriber.UserFeedbackEvent;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.processors.PublishProcessor;
import timber.log.Timber;
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
  private final PublishProcessor<UserFeedbackEvent> feedback;
  private long userId;
  private final AppSettingStore appSettings;
  private final WritableSortedCache<Status> sortedStatusCache;
  private final TypedCache<Status> pool;
  private CurrentTimeProvider currentTimeProvider;

  @Inject
  public UserStreamUtil(@NonNull TwitterStreamApi streamApi,
                        @NonNull WritableSortedCache<Status> sortedStatusCache,
                        @NonNull TypedCache<Status> pool,
                        @NonNull AppSettingStore appSettings,
                        @NonNull PublishProcessor<UserFeedbackEvent> feedback,
                        @NonNull CurrentTimeProvider currentTimeProvider) {
    this.streamApi = streamApi;
    this.feedback = feedback;
    this.appSettings = appSettings;
    this.sortedStatusCache = sortedStatusCache;
    this.pool = pool;
    this.currentTimeProvider = currentTimeProvider;
  }

  private boolean isConnectedUserStream = false;
  private PublishProcessor<Status> statusPublishSubject;
  private Disposable onStatusSubscription;
  private PublishProcessor<Long> deletionPublishSubject;
  private Disposable onDeletionSubscription;
  private PublishProcessor<Status> reactionPublishSubject;
  private Disposable onReactionSubscription;

  public void connect(String storeName, Context context) {
    if (currentTimeProvider.getCurrentTime() >= BuildConfig.STREAM_RETIREMENT_DATE) {
      return;
    }
    if (onStatusSubscription == null || onStatusSubscription.isDisposed()) {
      statusPublishSubject = PublishProcessor.create();
      onStatusSubscription = statusPublishSubject
          .buffer(500, TimeUnit.MILLISECONDS)
          .onBackpressureBuffer()
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(sortedStatusCache::upsert, onErrorAction);
    }
    if (onDeletionSubscription == null || onDeletionSubscription.isDisposed()) {
      deletionPublishSubject = PublishProcessor.create();
      onDeletionSubscription = deletionPublishSubject
          .buffer(500, TimeUnit.MILLISECONDS)
          .onBackpressureBuffer()
          .observeOn(AndroidSchedulers.mainThread())
          .flatMap(Flowable::fromIterable)
          .subscribe(sortedStatusCache::delete, onErrorAction);
    }
    if (reactionPublishSubject == null || onReactionSubscription.isDisposed()) {
      reactionPublishSubject = PublishProcessor.create();
      onReactionSubscription = reactionPublishSubject
          .buffer(500, TimeUnit.MILLISECONDS)
          .onBackpressureBuffer()
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(pool::upsert, onErrorAction);
    }
    if (!isConnectedUserStream) {
      pool.open();
      sortedStatusCache.open(storeName);
      appSettings.open();
      userId = appSettings.getCurrentUserId();
      final IntentFilter intentFilter = new IntentFilter();
      intentFilter.addAction(Intent.ACTION_TIME_TICK);
      context.registerReceiver(tickReceiver, intentFilter);
      streamApi.connectUserStream(statusListener);
      isConnectedUserStream = true;
    }
  }

  public void disconnect(Context context) {
    if (isConnectedUserStream) {
      streamApi.disconnectStreamListener();
      context.unregisterReceiver(tickReceiver);
    }

    if (statusPublishSubject != null) {
      statusPublishSubject.onComplete();
    }
    if (onStatusSubscription != null && !onStatusSubscription.isDisposed()) {
      onStatusSubscription.dispose();
    }

    if (deletionPublishSubject != null) {
      deletionPublishSubject.onComplete();
    }
    if (onDeletionSubscription != null && !onDeletionSubscription.isDisposed()) {
      onDeletionSubscription.dispose();
    }

    if (reactionPublishSubject != null) {
      reactionPublishSubject.onComplete();
    }
    if (onReactionSubscription != null && !onReactionSubscription.isDisposed()) {
      onReactionSubscription.dispose();
    }

    if (isConnectedUserStream) {
      appSettings.close();
      sortedStatusCache.close();
      pool.close();
      isConnectedUserStream = false;
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
      Timber.tag(TAG).d("onFavorite: src> %s, tgt> %s, sst> %s",
          source.getScreenName(), target.getScreenName(), favoritedStatus.getText());
      if (target.getId() == userId) {
        feedback.onNext(
            new UserFeedbackEvent(R.string.msg_faved_by_someone, source.getScreenName()));
      }
      final PerspectivalStatusImpl perspectivalStatus = new PerspectivalStatusImpl(favoritedStatus);
      getBindingStatus(perspectivalStatus).getStatusReaction().setFavorited(source.getId() == userId);
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
      Timber.tag(TAG).d(statusDeletionNotice.toString());
      deletionPublishSubject.onNext(statusDeletionNotice.getStatusId());
    }

    @Override
    public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
      Timber.tag(TAG).d("onTrackLimitationNotice: %s", numberOfLimitedStatuses);
    }

    @Override
    public void onScrubGeo(long userId, long upToStatusId) {
      Timber.tag(TAG).d("onScrubGeo: " + userId + ", " + upToStatusId);
    }

    @Override
    public void onStallWarning(StallWarning warning) {
      Timber.tag(TAG).d("onStallWarning: %s", warning.toString());
    }

    @Override
    public void onException(Exception ex) {
      Timber.tag(TAG).d("onException: %s", ex.toString());
    }
  };

  private final Consumer<Throwable> onErrorAction = throwable -> Timber.tag(TAG).d(throwable, "error: ");

  private final BroadcastReceiver tickReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (currentTimeProvider.getCurrentTime() >= BuildConfig.STREAM_RETIREMENT_DATE) {
        disconnect(context);
      }
    }
  };
}
