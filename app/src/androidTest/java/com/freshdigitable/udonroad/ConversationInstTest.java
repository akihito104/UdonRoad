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
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.contrib.DrawerMatchers;
import android.support.test.espresso.contrib.NavigationViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.app.AppCompatActivity;

import com.freshdigitable.udonroad.util.AssertionUtil;
import com.freshdigitable.udonroad.util.PerformUtil;
import com.freshdigitable.udonroad.util.UserUtil;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.PerformUtil.openDrawerNavigation;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusView;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createResponseList;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatus;
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.Mockito.when;

/**
 * Created by akihit on 2017/06/21.
 */
@RunWith(AndroidJUnit4.class)
public class ConversationInstTest extends TimelineInstTestBase {

  private Status replied;
  private Status hasReply;

  @Test
  public void favInConversationTimeline_and_returnHomeTimeline() throws Exception {
    setupCreateFavorite(0,1);
    AssertionUtil.checkHasReplyTo(hasReply);
    AssertionUtil.checkMainActivityTitle(R.string.title_home);

    PerformUtil.selectItemView(hasReply);
    PerformUtil.showDetail();
    AssertionUtil.checkMainActivityTitle(R.string.title_detail);

    onView(withId(R.id.iffabMenu_main_conv)).perform(click());

    final IdlingResource timelineIdlingResource = getTimelineIdlingResource("conv", 2);
    Espresso.registerIdlingResources(timelineIdlingResource);
    AssertionUtil.checkHasReplyTo(hasReply);
    AssertionUtil.checkMainActivityTitle(R.string.title_conv);
    checkFFAB(matches(not(isDisplayed())));
    Espresso.unregisterIdlingResources(timelineIdlingResource);
    PerformUtil.selectItemView(replied);
    checkFFAB(matches(isDisplayed()));
    PerformUtil.favo();
    Espresso.pressBack();
    AssertionUtil.checkFavCount(replied, 1);
    AssertionUtil.checkFavCountDoesNotExist(hasReply);

    Espresso.pressBack();
    AssertionUtil.checkMainActivityTitle(R.string.title_detail);
    onView(withId(R.id.iffabMenu_main_conv)).check(matches(isDisplayed()));

    Espresso.pressBack();
    AssertionUtil.checkMainActivityTitle(R.string.title_home);
    onView(ofStatusView(withText(replied.getText()))).check(doesNotExist());
    AssertionUtil.checkFavCountDoesNotExist(hasReply);
    checkFFAB(matches(isDisplayed()));
  }

  @Test
  public void favInConversationTimeline_and_returnHomeTimelineWithDrawerMenu() throws Exception {
    setupCreateFavorite(0,1);
    AssertionUtil.checkHasReplyTo(hasReply);
    AssertionUtil.checkMainActivityTitle(R.string.title_home);

    PerformUtil.selectItemView(hasReply);
    PerformUtil.showDetail();
    AssertionUtil.checkMainActivityTitle(R.string.title_detail);
    checkFFAB(matches(not(isDisplayed())));

    onView(withId(R.id.iffabMenu_main_conv)).perform(click());

    final IdlingResource timelineIdlingResource = getTimelineIdlingResource("conv", 2);
    Espresso.registerIdlingResources(timelineIdlingResource);
    AssertionUtil.checkHasReplyTo(hasReply);
    AssertionUtil.checkMainActivityTitle(R.string.title_conv);
    onView(withId(R.id.iffabMenu_main_conv)).check(matches(not(isDisplayed())));
    checkFFAB(matches(not(isDisplayed())));
    Espresso.unregisterIdlingResources(timelineIdlingResource);
    PerformUtil.selectItemView(replied);
    checkFFAB(matches(isDisplayed()));
    PerformUtil.favo();
    Espresso.pressBack();
    AssertionUtil.checkFavCount(replied, 1);
    AssertionUtil.checkFavCountDoesNotExist(hasReply);

    openDrawerNavigation();
    onView(withId(R.id.nav_drawer)).perform(NavigationViewActions.navigateTo(R.id.drawer_menu_home));

    AssertionUtil.checkMainActivityTitle(R.string.title_home);
    onView(withId(R.id.nav_drawer_layout)).check(matches(DrawerMatchers.isClosed()));
    checkFFAB(matches(isDisplayed()));

    onView(ofStatusView(withText(replied.getText()))).check(doesNotExist());
    AssertionUtil.checkFavCountDoesNotExist(hasReply);
  }

  private static void checkFFAB(ViewAssertion matches) {
    onView(withId(R.id.ffab)).check(matches);
  }

  @Override
  protected int setupTimeline() throws TwitterException {
    final long repliedId = 9000L;
    replied = createStatus(repliedId, getLoginUser());
    hasReply = createStatus(10002L, UserUtil.builder(20000, "userB").build());
    when(hasReply.getInReplyToStatusId()).thenReturn(repliedId);
    final ResponseList<Status> responseList = createResponseList(Arrays.asList(
        hasReply,
        createStatus(10001L, getLoginUser())
    ));
    when(twitter.getHomeTimeline()).thenReturn(responseList);
    when(twitter.showStatus(10002)).thenReturn(hasReply);
    when(twitter.showStatus(repliedId)).thenReturn(replied);
    super.responseList = createResponseList(responseList);
    super.responseList.add(replied);
    return responseList.size();
  }

  private final ActivityTestRule<MainActivity> rule
      = new ActivityTestRule<>(MainActivity.class, false, false);

  @Override
  protected ActivityTestRule<? extends AppCompatActivity> getRule() {
    return rule;
  }
}
