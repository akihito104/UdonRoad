/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
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

import com.freshdigitable.udonroad.util.UserUtil;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import twitter4j.IDs;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.User;

import static com.freshdigitable.udonroad.MockMainApplication.getApp;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TwitterRule extends TestWatcher {
  final Twitter twitter = getApp().twitterApiModule.twitter;
  private final TwitterStream twitterStream = getApp().twitterApiModule.twitterStream;

  @Override
  protected void starting(Description description) {
    super.starting(description);
    try {
      setupTwitter();
    } catch (TwitterException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void finished(Description description) {
    super.finished(description);
    getApp().twitterApiModule.reset();
  }

  private void setupTwitter() throws TwitterException {
    setupLoginUser();
    setupIgnoringUsers();
  }

  protected User getLoginUser() {
    return UserUtil.createUserA();
  }

  private void setupLoginUser() throws TwitterException {
    final User loginUser = getLoginUser();
    final long userId = loginUser.getId();
    when(twitter.getId()).thenReturn(userId);
    when(twitter.showUser(userId)).thenReturn(loginUser);
    when(twitter.verifyCredentials()).thenReturn(loginUser);
    when(twitterStream.getId()).thenReturn(userId);
  }

  private void setupIgnoringUsers() throws TwitterException {
    final IDs ignoringUserIDsMock = mock(IDs.class);
    when(ignoringUserIDsMock.getIDs()).thenReturn(new long[0]);
    when(ignoringUserIDsMock.getNextCursor()).thenReturn(0L);
    when(ignoringUserIDsMock.getPreviousCursor()).thenReturn(0L);
    when(ignoringUserIDsMock.hasNext()).thenReturn(false);
    when(twitter.getBlocksIDs()).thenReturn(ignoringUserIDsMock);
    when(twitter.getBlocksIDs(anyLong())).thenReturn(ignoringUserIDsMock);
    when(twitter.getMutesIDs(anyLong())).thenReturn(ignoringUserIDsMock);
  }
}
