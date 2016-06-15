/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

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
  private TwitterStreamApi streamApi;
  private TimelineAdapter adapter;

  private UserStreamUtil(TwitterStreamApi streamApi, TimelineAdapter adapter) {
    this.streamApi = streamApi;
    this.adapter = adapter;
  }

  public static UserStreamUtil setup(Context context, TimelineAdapter adapter) {
    TwitterStreamApi streamApi = TwitterStreamApi.setup(context);
    if (streamApi == null) {
      throw new RuntimeException();
    }
    return new UserStreamUtil(streamApi, adapter);
  }

  public void connect() {
    streamApi.connectUserStream(statusListener);
  }

  public void disconnect() {
    streamApi.disconnectStreamListener();
    statusPublishSubject.onCompleted();
    if (subscription != null && subscription.isUnsubscribed()) {
      subscription.unsubscribe();
    }
  }

  private final PublishSubject<Status> statusPublishSubject = PublishSubject.create();

  private final Subscription subscription = statusPublishSubject
      .buffer(500, TimeUnit.MILLISECONDS)
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
