/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.util.Log;

import com.freshdigitable.udonroad.datastore.TimelineStore;

import java.util.List;
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
import twitter4j.UserStreamAdapter;
import twitter4j.UserStreamListener;

/**
 * Created by akihit on 2016/06/07.
 */
public class UserStreamUtil {
  private static final String TAG = UserStreamUtil.class.getSimpleName();

  @Inject
  TwitterStreamApi streamApi;
  private TimelineStore timelineStore;

  public UserStreamUtil(TimelineStore timelineStore) {
    this.timelineStore = timelineStore;
  }

  private boolean isConnectedUserStream = false;

  public void connect() {
    if (subscription == null || subscription.isUnsubscribed()) {
      statusPublishSubject = PublishSubject.create();
      subscription = statusPublishSubject
          .buffer(500, TimeUnit.MILLISECONDS)
          .onBackpressureBuffer()
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(new Action1<List<Status>>() {
            @Override
            public void call(List<Status> statuses) {
              timelineStore.upsert(statuses);
            }
          }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
              Log.d(TAG, "error: " + throwable);
            }
          });
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
    if (subscription != null && !subscription.isUnsubscribed()) {
      subscription.unsubscribe();
    }
  }

  private PublishSubject<Status> statusPublishSubject;
  private Subscription subscription;

  private final UserStreamListener statusListener = new UserStreamAdapter() {

    @Override
    public void onStatus(final Status status) {
      statusPublishSubject.onNext(status);
    }

    @Override
    public void onDeletionNotice(final StatusDeletionNotice statusDeletionNotice) {
      Log.d(TAG, statusDeletionNotice.toString());
      Observable.just(statusDeletionNotice.getStatusId())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(new Action1<Long>() {
            @Override
            public void call(Long deletedStatusId) {
              timelineStore.deleteStatus(deletedStatusId);
            }
          }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
              Log.e(TAG, "error: ", throwable);
            }
          });
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
}
