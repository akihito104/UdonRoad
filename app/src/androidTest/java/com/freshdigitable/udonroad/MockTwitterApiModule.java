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

import com.freshdigitable.udonroad.module.twitter.TwitterStreamApi;
import com.freshdigitable.udonroad.subscriber.UserFeedbackEvent;
import com.freshdigitable.udonroad.subscriber.UserFeedbackSubscriber;
import com.freshdigitable.udonroad.util.TwitterResponseMock;

import org.mockito.Mockito;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.processors.PublishProcessor;
import twitter4j.Twitter;
import twitter4j.TwitterAPIConfiguration;
import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.UserStreamListener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * MockTwitterModule is to inject Twitter module for test.
 *
 * Created by akihit on 2016/06/16.
 */
@Module
public class MockTwitterApiModule {

  final Twitter twitter;
  final TwitterStream twitterStream;
  final UserStreamListenerHolder userStreamListenerHolder;

  MockTwitterApiModule() {
    twitter = mock(Twitter.class);
    final TwitterAPIConfiguration twitterAPIConfigMock
        = TwitterResponseMock.createTwitterAPIConfigMock();
    when(twitterAPIConfigMock.getShortURLLength()).thenReturn(23);
    when(twitterAPIConfigMock.getShortURLLengthHttps()).thenReturn(23);
    try {
      when(twitter.getAPIConfiguration()).thenReturn(twitterAPIConfigMock);
    } catch (TwitterException e) {
      throw new RuntimeException(e);
    }

    twitterStream = mock(TwitterStream.class);
    userStreamListenerHolder = new UserStreamListenerHolder();
  }

  @Singleton
  @Provides
  Twitter provideTwitter() {
    return twitter;
  }

  @Singleton
  @Provides
  TwitterStream provideTwitterStream() {
    return twitterStream;
  }

  @Singleton
  @Provides
  UserFeedbackSubscriber provideUserFeedbackSubscriber(Context context, PublishProcessor<UserFeedbackEvent> pub) {
    return new UserFeedbackSubscriber(context, pub);
  }

  @Provides
  @Singleton
  PublishProcessor<UserFeedbackEvent> providePublishSubjectUserFeedbackEvent() {
    return PublishProcessor.create();
  }

  @Singleton
  @Provides
  TwitterStreamApi provideTwitterStreamApi(TwitterStream twitterStream) {
    return new TwitterStreamApi(twitterStream) {
      @Override
      public void connectUserStream(UserStreamListener listener) {
        userStreamListenerHolder.userStreamListener = listener;
        twitterStream.addListener(listener);
        twitterStream.user();
      }
    };
  }

  public void reset() {
    Mockito.reset(twitter, twitterStream);
  }

  static class UserStreamListenerHolder {
    UserStreamListener userStreamListener;
  }
}
