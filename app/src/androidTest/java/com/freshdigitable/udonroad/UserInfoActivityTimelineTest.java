/*
 * Copyright (c) 2017. Matsuda, Akihit (akihito104)
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

import android.support.test.espresso.Espresso;

import com.freshdigitable.udonroad.util.AssertionUtil;
import com.freshdigitable.udonroad.util.PerformUtil;

import org.junit.Test;

import java.util.Arrays;

import twitter4j.PagableResponseList;
import twitter4j.Paging;
import twitter4j.Relationship;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.User;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.AssertionUtil.anywayNotVisible;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createResponseList;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatus;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatusHasImage;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by akihit on 2017/07/07.
 */
public class UserInfoActivityTimelineTest extends UserInfoActivityInstTest.Base {

  private Status tweetHasImage;
  private Status defaultTweet;

  @Override
  protected int setupTimeline() throws TwitterException {
    final Relationship relationship = mock(Relationship.class);
    final User loginUser = getLoginUser();
    tweetHasImage = createStatusHasImage(20000, getLoginUser());
    defaultTweet = createStatus(10000, getLoginUser());
    final ResponseList<Status> responseList = createResponseList(Arrays.asList(defaultTweet, tweetHasImage));
    this.responseList = responseList;
    final ResponseList<Status> emptyStatusResponseList = createResponseList();

    final int size = responseList.size();
    when(loginUser.getStatusesCount()).thenReturn(size);
    when(twitter.getUserTimeline(loginUser.getId())).thenReturn(responseList);
    when(twitter.getUserTimeline(anyLong(), any(Paging.class))).thenReturn(emptyStatusResponseList);
    when(twitter.getFavorites(anyLong())).thenReturn(emptyStatusResponseList);
    final PagableResponseList<User> emptyUserPagableResponseList = mock(PagableResponseList.class);
    when(twitter.getFollowersList(anyLong(), anyLong())).thenReturn(emptyUserPagableResponseList);
    when(twitter.getFriendsList(anyLong(), anyLong())).thenReturn(emptyUserPagableResponseList);

    when(twitter.showFriendship(anyLong(), anyLong())).thenReturn(relationship);
    return responseList.size();
  }

  @Test
  public void showStatusDetail() throws Exception {
    AssertionUtil.checkUserInfoActivityTitle("");
    onView(withText("TWEET\n2")).check(matches(isDisplayed()));
    PerformUtil.selectItemView(defaultTweet);
    PerformUtil.showDetail();
    AssertionUtil.checkUserInfoActivityTitle(R.string.title_detail);
    onView(withId(R.id.pager_tabs)).check(anywayNotVisible());
    onView(withId(R.id.action_heading)).check(doesNotExist());

    Espresso.pressBack();
    AssertionUtil.checkUserInfoActivityTitle("");
    onView(withId(R.id.pager_tabs)).check(matches(isDisplayed()));
    onView(withText("TWEET\n2")).check(matches(isDisplayed()));
    onView(withId(R.id.ffab)).check(matches(isCompletelyDisplayed()));
  }

  @Test
  public void restoreStatusDetail() throws Exception {
    AssertionUtil.checkUserInfoActivityTitle("");
    onView(withText("TWEET\n2")).check(matches(isDisplayed()));
    PerformUtil.selectItemView(tweetHasImage);
    PerformUtil.showDetail();
    AssertionUtil.checkUserInfoActivityTitle(R.string.title_detail);
    onView(withId(R.id.pager_tabs)).check(anywayNotVisible());
    onView(withId(R.id.action_heading)).check(doesNotExist());

    PerformUtil.clickThumbnailAt(0);
    Espresso.pressBack();

    //    AssertionUtil.checkUserInfoActivityTitle(R.string.title_detail);
    onView(withId(R.id.action_heading)).check(doesNotExist());
    onView(withId(R.id.iffabMenu_main_fav)).check(matches(isDisplayed()));
    onView(withId(R.id.iffabMenu_main_rt)).check(matches(isDisplayed()));
    onView(withId(R.id.pager_tabs)).check(anywayNotVisible());

    Espresso.pressBack();
    AssertionUtil.checkUserInfoActivityTitle("");
    onView(withId(R.id.pager_tabs)).check(matches(isDisplayed()));
    onView(withText("TWEET\n2")).check(matches(isDisplayed()));
    onView(withId(R.id.ffab)).check(matches(isCompletelyDisplayed()));
  }
}
