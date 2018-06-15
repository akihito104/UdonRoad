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

import com.freshdigitable.udonroad.util.TwitterResponseMock;
import com.freshdigitable.udonroad.util.UserUtil;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.util.ArrayList;
import java.util.List;

import twitter4j.IDs;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.User;
import twitter4j.UserStreamListener;

import static com.freshdigitable.udonroad.MockMainApplication.getApp;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createResponseList;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createRtStatus;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatus;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TwitterRule extends TestWatcher {
  final Twitter twitter = getApp().twitterApiModule.twitter;
  private final TwitterStream twitterStream = getApp().twitterApiModule.twitterStream;
  private final UserStreamListener streamListener = getApp().getUserStreamListener();
  private List<Status> statuses;

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
    setupDefaultTimeline();
  }

  protected User getLoginUser() {
    return UserUtil.createUserA();
  }

  protected List<Status> getResponse() {
    final List<Status> resLi = new ArrayList<>();
    final User user3000 = UserUtil.builder(3000, "user3000").build();
    for (int i = 1; i <= 20; i++) {
      final Status status = createStatus(i * 1000L, user3000);
      resLi.add(status);
    }
    return resLi;
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

  public void receiveStatuses(final Status... statuses) {
    TwitterResponseMock.receiveStatuses(streamListener, statuses);
  }

  public void receiveDeletionNotice(final Status... target) {
    TwitterResponseMock.receiveDeletionNotice(streamListener, target);
  }

  protected void setupCreateFavorite(final int rtCount, final int favCount) throws TwitterException {
    when(twitter.createFavorite(anyLong())).thenAnswer(invocation -> {
      final Long id = invocation.getArgument(0);
      final Status status = findByStatusId(id);
      when(status.getFavoriteCount()).thenReturn(favCount);
      when(status.getRetweetCount()).thenReturn(rtCount);
      when(status.isFavorited()).thenReturn(true);
      return status;
    });
  }

  protected void setupDestroyFavorite(final int rtCount, final int favCount) throws TwitterException {
    when(twitter.destroyFavorite(anyLong())).thenAnswer(invocation -> {
      final Long id = invocation.getArgument(0);
      final Status status = findByStatusId(id);
      when(status.getFavoriteCount()).thenReturn(favCount);
      when(status.getRetweetCount()).thenReturn(rtCount);
      when(status.isFavorited()).thenReturn(false);
      return status;
    });
  }

  protected void setupRetweetStatus(final long rtStatusId, final int rtCount, final int favCount)
      throws TwitterException {
    when(twitter.retweetStatus(anyLong())).thenAnswer(invocation -> {
      final Long id = invocation.getArgument(0);
      final Status rtedStatus = findByStatusId(id);
      TwitterResponseMock.receiveStatuses(streamListener,
          createRtStatus(rtedStatus, rtStatusId, false));
      return createRtStatus(rtedStatus, rtStatusId, rtCount, favCount, true);
    });
  }

  private void setupDefaultTimeline() throws TwitterException {
    statuses = getResponse();
    final ResponseList<Status> responseList = TwitterResponseMock.createResponseList(statuses);
    when(twitter.getHomeTimeline()).thenReturn(responseList);
    when(twitter.getHomeTimeline(any(Paging.class))).thenReturn(createResponseList());
  }

  private Status findByStatusId(long statusId) throws Exception {
    for (Status s : statuses) {
      if (s.getId() == statusId) {
        return s;
      }
      if (s.getQuotedStatusId() == statusId) {
        return s.getQuotedStatus();
      }
    }
    throw new TwitterException("status is not found. ID: " + statusId);
  }
}
