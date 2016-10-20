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

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.freshdigitable.udonroad.util.PerformUtil;
import com.freshdigitable.udonroad.util.TwitterResponseMock;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import twitter4j.Status;
import twitter4j.TwitterException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.assertion.ViewAssertions.selectedDescendantsMatch;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.StatusViewAssertion.recyclerViewDescendantsMatches;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusViewAt;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatus;
import static org.hamcrest.CoreMatchers.not;

/**
 * Created by akihit on 2016/06/15.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityInstTest extends TimelineInstTestBase {
  @Rule
  public ActivityTestRule<MainActivity> rule
      = new ActivityTestRule<>(MainActivity.class, false, false);

  @Override
  protected int setupTimeline() throws TwitterException {
    return setupDefaultTimeline();
  }

  @Test
  public void receive2ReverseStatusIdOrderTweetsAtSameTime_and_displayStatusIdOrder() throws Exception {
    final Status received = createStatus(25000, getLoginUser());
    receiveStatuses(received);
    onView(ofStatusView(withText(received.getText())))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 0));
    onView(ofStatusView(withText(received.getText())))
        .check(selectedDescendantsMatch(withId(R.id.tl_fav_icon), not(isDisplayed())));

    final Status received29 = createStatus(29000, getLoginUser());
    final Status received27 = createStatus(27000, getLoginUser());
    receiveStatuses(received29, received27);
    onView(ofStatusView(withText(received.getText())))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 2));
    onView(ofStatusView(withText(received29.getText())))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 0));
    onView(ofStatusView(withText(received27.getText())))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 1));
  }

  @Test
  public void receiveDelayed2ReverseStatusIdOrderTweetsAtSameTime_and_displayStatusIdOrder()
      throws Exception {
    final Status received25 = createStatus(25000);
    receiveStatuses(received25);
    onView(ofStatusView(withText(received25.getText())))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 0));

    final Status received29 = createStatus(29000);
    final Status received23 = createStatus(23000);
    receiveStatuses(received29, received23);
    onView(ofStatusView(withText(received25.getText())))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 1));
    onView(ofStatusView(withText(received29.getText())))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 0));
    onView(ofStatusView(withText(received23.getText())))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 2));
  }

  @Test
  public void fetchFav_then_favIconAndCountAreDisplayed() throws Exception {
    // setup
    setupCreateFavorite(0, 1);
    // exec.
    PerformUtil.selectItemViewAt(0);
    onView(withId(R.id.iffab_ffab)).check(matches(isDisplayed()));
    PerformUtil.favo();
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(selectedDescendantsMatch(withId(R.id.tl_favcount), withText("1")));
    // TODO tint color check
  }

  @Test
  public void fetchRT_then_RtIconAndCountAreDisplayed() throws Exception {
    // setup
    setupRetweetStatus(25000);
    // exec.
    PerformUtil.selectItemViewAt(0);
    onView(withId(R.id.ffab)).check(matches(isDisplayed()));
    PerformUtil.retweet();
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(selectedDescendantsMatch(withId(R.id.tl_rtcount), withText("1")));
    onView(withId(R.id.timeline)).perform(swipeDown());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(selectedDescendantsMatch(withId(R.id.tl_rtcount), withText("1")));
    // TODO tint color check
  }

  @Test
  public void receiveStatusDeletionNoticeForLatestStatus_then_removedTheTopOfTimeline()
      throws Exception {
    final Status target = findByStatusId(20000);
    final String deletedStatusText = target.getText();
    final Status top = findByStatusId(19000);
    receiveDeletionNotice(target);

    onView(ofStatusView(withText(deletedStatusText))).check(doesNotExist());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(top.getText()))));
  }

  @Test
  public void receiveStatusDeletionNoticeForRTStatus_then_removedRTStatus()
      throws Exception {
    // setup
    setupRetweetStatus(25000);
    // exec.
    final Status rtTarget = findByStatusId(20000);
    PerformUtil.selectItemViewAt(0);
    PerformUtil.retweet();
    final Status target = TwitterResponseMock.createRtStatus(rtTarget, 25000, false);
    onView(withId(R.id.timeline)).perform(swipeDown());
    receiveDeletionNotice(target);

    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(rtTarget.getText()))));
  }

  @Test
  public void receiveStatusDeletionNoticeForRTingStatus_then_removedOriginalAndRTedStatuses()
      throws Exception {
    // setup
    setupRetweetStatus(25000);
    // exec.
    final Status target = findByStatusId(20000);
    final Status top = findByStatusId(19000);
    PerformUtil.selectItemView(target);
    PerformUtil.retweet();
    final Status targetRt = TwitterResponseMock.createRtStatus(target, 25000, false);
    receiveDeletionNotice(target, targetRt);

    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(top.getText()))));
    onView(ofStatusView(withText(target.getText()))).check(doesNotExist());
  }

  @Test
  public void receiveStatusDeletionNoticeForFavedStatus_then_removedOriginalStatuses()
      throws Exception {
    // setup
    setupCreateFavorite(0, 1);
    // exec.
    final Status target = findByStatusId(20000);
    final Status top = findByStatusId(19000);
    PerformUtil.selectItemView(target);
    PerformUtil.favo();
    receiveDeletionNotice(target);

    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(top.getText()))));
    onView(ofStatusView(withText(target.getText()))).check(doesNotExist());
  }

  @Test
  public void receive2StatusDeletionNoticeAtSameTime_then_removed2Statuses()
      throws Exception {
    final Status status18000 = findByStatusId(18000);
    final Status status20000 = findByStatusId(20000);
    final Status top = findByStatusId(19000);
    receiveDeletionNotice(status18000, status20000);

    onView(ofStatusView(withText(status20000.getText()))).check(doesNotExist());
    onView(ofStatusView(withText(status18000.getText()))).check(doesNotExist());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(top.getText()))));
  }

  @Test
  public void receiveStatusDeletionNoticeForSelectedStatus_then_removedSelectedStatus()
      throws Exception {
    PerformUtil.selectItemViewAt(0);
    final Status target = findByStatusId(20000);
    final Status top = findByStatusId(19000);
    receiveDeletionNotice(target);

    onView(ofStatusView(withText(target.getText()))).check(doesNotExist());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(top.getText()))));
  }

  @Test
  public void clickSendIcon_then_openTweetInputViewAndShowFab() {
    // open
    onView(withId(R.id.action_write)).perform(click());
    onView(withId(R.id.main_tweet_input_view)).check(matches(isDisplayed()));
    onView(withId(R.id.main_send_tweet)).check(matches(isDisplayed()));
    onView(withId(R.id.action_cancel)).check(matches(isDisplayed()));
    // the menu is not matched any view so always fail.
//    onView(withId(R.id.action_write)).check(matches(not(isDisplayed())));

    // close
    onView(withId(R.id.action_cancel)).perform(click());
    onView(withId(R.id.action_write)).check(matches(isDisplayed()));
//    onView(withId(R.id.main_tweet_input_view)).check(matches(not(isDisplayed())));
    onView(withId(R.id.main_send_tweet)).check(matches(not(isDisplayed())));
  }

  @Override
  protected ActivityTestRule<MainActivity> getRule() {
    return rule;
  }
}