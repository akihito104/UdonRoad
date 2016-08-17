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

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import twitter4j.Twitter;
import twitter4j.TwitterStream;
import twitter4j.UserStreamListener;

import static org.mockito.Mockito.mock;

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
    return mock(TwitterApi.class);
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
