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

import com.freshdigitable.udonroad.module.TwitterApiModule;
import com.freshdigitable.udonroad.module.twitter.TwitterStreamApi;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import twitter4j.Twitter;
import twitter4j.TwitterStream;
import twitter4j.UserStreamListener;

import static org.mockito.Mockito.mock;

/**
 * MockTwitterModule is to inject Twitter module for test.
 *
 * Created by akihit on 2016/06/16.
 */
public class MockTwitterApiModule extends TwitterApiModule {
  public MockTwitterApiModule(Context context) {
    super(context);
  }

  @Override
  public Twitter provideTwitter() {
    return mock(Twitter.class);
  }

  @Override
  public TwitterStream provideTwitterStream() {
    return mock(TwitterStream.class);
  }

  @Module
  static class MockTwitterStreamApiModule {
    public UserStreamListener userStreamListener;

    @Singleton
    @Provides
    public TwitterStreamApi provideTwitterStreamApi(TwitterStream twitterStream) {
      return new TwitterStreamApi(twitterStream) {
        @Override
        public void connectUserStream(UserStreamListener listener) {
          userStreamListener = listener;
          twitterStream.addListener(listener);
          twitterStream.user();
        }
      };
    }
  }
}
