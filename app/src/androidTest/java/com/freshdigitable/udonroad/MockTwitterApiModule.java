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
import twitter4j.TwitterStream;
import twitter4j.UserStreamListener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by akihit on 2016/06/16.
 */
@Module
public class MockTwitterApiModule {
  private Context context;

  public MockTwitterApiModule(Context context) {
    this.context = context;
  }

  @Singleton
  @Provides
  public Twitter provideTwitter() {
    return mock(Twitter.class);
  }

  @Singleton
  @Provides
  public TwitterStream provideTwitterStream() {
    return mock(TwitterStream.class);
  }

  @Provides
  public SharedPreferences provideSharedPreferences() {
    return context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE);
  }

  @Singleton
  @Provides
  public TwitterApi provideTwitterApi() {
    final TwitterApi mock = mock(TwitterApi.class);
    when(mock.loadAccessToken()).thenReturn(true);
    return mock;
  }

  public UserStreamListener userStreamListener;

  @Singleton
  @Provides
  public TwitterStreamApi provideTwitterStreamApi(
      TwitterStream twitterStream, SharedPreferences pref) {
    return new TwitterStreamApi(twitterStream, pref) {
      @Override
      public void connectUserStream(UserStreamListener listener) {
        userStreamListener = listener;
      }
    };
  }
}
