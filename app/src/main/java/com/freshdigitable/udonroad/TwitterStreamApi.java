/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.content.SharedPreferences;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import twitter4j.TwitterStream;
import twitter4j.UserStreamListener;
import twitter4j.auth.AccessToken;

public class TwitterStreamApi {
  private final TwitterStream twitterStream;
  private final SharedPreferences sharedPreferences;

  @Inject
  public TwitterStreamApi(TwitterStream twitterStream, SharedPreferences sharedPreferences) {
    this.twitterStream = twitterStream;
    this.sharedPreferences = sharedPreferences;
  }

  public boolean loadAccessToken() {
    final AccessToken accessToken = TwitterApi.loadAccessToken(sharedPreferences);
    if (accessToken == null) {
      return false;
    }
    twitterStream.setOAuthAccessToken(accessToken);
    return true;
  }

  public void connectUserStream(UserStreamListener listener) {
    twitterStream.addListener(listener);
    twitterStream.user();
  }

  public void disconnectStreamListener() {
    Observable.create(new Observable.OnSubscribe<Void>() {
      @Override
      public void call(Subscriber<? super Void> subscriber) {
        twitterStream.shutdown();
        twitterStream.clearListeners();
      }
    }).subscribeOn(Schedulers.io())
        .subscribe();
  }
}
