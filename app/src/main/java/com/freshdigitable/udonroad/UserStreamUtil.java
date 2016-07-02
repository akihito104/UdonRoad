/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.util.Log;

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
  private TimelineAdapter adapter;

  public UserStreamUtil(TimelineAdapter adapter) {
//    app.getTwitterApiComponent().inject(this);
    this.adapter = adapter;
  }

  public void connect() {
    statusPublishSubject = PublishSubject.create();
    subscription = statusPublishSubject
        .buffer(500, TimeUnit.MILLISECONDS)
        .onBackpressureBuffer()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<List<Status>>() {
          @Override
          public void call(List<Status> statuses) {
            adapter.addNewStatuses(statuses);
          }
        }, new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            Log.d(TAG, "error: " + throwable);
          }
        });

    streamApi.loadAccessToken();
    streamApi.connectUserStream(statusListener);
  }

  public void disconnect() {
    streamApi.disconnectStreamListener();
    statusPublishSubject.onCompleted();
    if (subscription != null && subscription.isUnsubscribed()) {
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
              adapter.deleteStatus(deletedStatusId);
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
