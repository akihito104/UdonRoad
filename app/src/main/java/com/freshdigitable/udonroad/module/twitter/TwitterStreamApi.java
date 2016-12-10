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

package com.freshdigitable.udonroad.module.twitter;

import javax.inject.Inject;

import twitter4j.TwitterStream;
import twitter4j.UserStreamListener;
import twitter4j.auth.AccessToken;

public class TwitterStreamApi {
  private final TwitterStream twitterStream;

  @Inject
  public TwitterStreamApi(TwitterStream twitterStream) {
    this.twitterStream = twitterStream;
  }

  public void setOAuthAccessToken(AccessToken accessToken) {
    twitterStream.setOAuthAccessToken(accessToken);
  }

  public void connectUserStream(UserStreamListener listener) {
    twitterStream.addListener(listener);
    twitterStream.user();
  }

  public void disconnectStreamListener() {
    twitterStream.shutdown();
    twitterStream.clearListeners();
  }
}
