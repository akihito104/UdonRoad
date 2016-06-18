/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;

/**
 * Created by akihit on 2016/06/16.
 */
@Module
public class TwitterApiModule {

  private Context context;

  public TwitterApiModule(Context context) {
    this.context = context;
  }

  @Singleton
  @Provides
  public Twitter provideTwitter() {
    final TwitterFactory twitterFactory = new TwitterFactory();
    final Twitter twitter = twitterFactory.getInstance();
    final String key = context.getString(R.string.consumer_key);
    final String secret = context.getString(R.string.consumer_secret);
    twitter.setOAuthConsumer(key, secret);
    return twitter;
  }

  @Singleton
  @Provides
  public TwitterStream provideTwitterStream() {
    final TwitterStreamFactory twitterStreamFactory = new TwitterStreamFactory();
    final TwitterStream twitterStream = twitterStreamFactory.getInstance();
    final String key = context.getString(R.string.consumer_key);
    final String secret = context.getString(R.string.consumer_secret);
    twitterStream.setOAuthConsumer(key, secret);
    return twitterStream;
  }

  @Provides
  public SharedPreferences provideSharedPreferences() {
    return context.getSharedPreferences("udonroad_prefs", Context.MODE_PRIVATE);
  }
}
