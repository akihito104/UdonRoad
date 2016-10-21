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
import android.content.SharedPreferences;

import com.freshdigitable.udonroad.R;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;

/**
 * TwitterApiModule is to inject twitter module.
 *
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
