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

package com.freshdigitable.udonroad.module;

import android.content.Context;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.subscriber.UserFeedbackEvent;
import com.freshdigitable.udonroad.subscriber.UserFeedbackSubscriber;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.processors.PublishProcessor;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

/**
 * TwitterApiModule is to inject twitter module.
 *
 * Created by akihit on 2016/06/16.
 */
@Module
public class TwitterApiModule {
  @Singleton
  @Provides
  Configuration provideConfiguration(Context context) {
    return new ConfigurationBuilder()
        .setTweetModeExtended(true)
        .setOAuthConsumerKey(context.getString(R.string.consumer_key))
        .setOAuthConsumerSecret(context.getString(R.string.consumer_secret))
        .build();
  }

  @Singleton
  @Provides
  Twitter provideTwitter(Configuration config) {
    return new TwitterFactory(config).getInstance();
  }

  @Singleton
  @Provides
  TwitterStream provideTwitterStream(Configuration config) {
    return new TwitterStreamFactory(config).getInstance();
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
}
