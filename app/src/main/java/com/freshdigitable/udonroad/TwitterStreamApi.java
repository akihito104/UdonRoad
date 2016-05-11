/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.content.Context;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.UserStreamListener;
import twitter4j.auth.AccessToken;

public class TwitterStreamApi {

  public static TwitterStreamApi setup(Context context) {
    AccessToken accessToken = TwitterApi.loadAccessToken(context);
    if (accessToken == null) {
      return null;
    }
    String consumerKey = context.getString(R.string.consumer_key);
    String consumerSecret = context.getString(R.string.consumer_secret);

    TwitterStreamFactory streamFactory = new TwitterStreamFactory();
    TwitterStream twitterStream = streamFactory.getInstance();
    twitterStream.setOAuthConsumer(consumerKey, consumerSecret);
    twitterStream.setOAuthAccessToken(accessToken);

    return new TwitterStreamApi(twitterStream);
  }

  private final TwitterStream twitterStream;

  private TwitterStreamApi(TwitterStream stream) {
    this.twitterStream = stream;
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
