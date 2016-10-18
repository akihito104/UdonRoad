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

import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import twitter4j.Status;
import twitter4j.TwitterException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeUp;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.assertion.ViewAssertions.selectedDescendantsMatch;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.asUserIcon;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusViewAt;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatus;
import static org.hamcrest.Matchers.not;

/**
 * MainActivityResumeInstTest tests MainActivity can resume from home button pushed.
 *
 * Created by akihit on 2016/07/01.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityResumeInstTest extends TimelineInstTestBase {
  @Rule
  public ActivityTestRule<MainActivity> rule
      = new ActivityTestRule<>(MainActivity.class, false, false);

  @Override
  protected void setupTimeline() throws TwitterException {
    setupDefaultTimeline();
    setupDefaultUserInfoTimeline();
  }

  @Test
  public void heading_then_latestTweetAppears() throws Exception {
    onView(ofStatusViewAt(R.id.timeline, 0)).perform(click());
    final Status received = createStatus(22000);
    receiveStatuses(createStatus(21000), received);
    onView(withId(R.id.action_heading)).perform(click());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(received.getText()))));
    onView(withId(R.id.ffab)).check(matches(not(isDisplayed())));
  }

  @Test
  public void receiveStatusesAfterRelaunch_then_latestTweetAppers() throws Exception {
    launchHomeAndBackToApp();
    Thread.sleep(200);

    final Status received = createStatus(22000);
    receiveStatuses(createStatus(21000), received);
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(received.getText()))));
  }

  @Test
  public void headingAfterRelaunch_then_latestTweetAppears() throws Exception {
    launchHomeAndBackToApp();

    onView(ofStatusViewAt(R.id.timeline, 0)).perform(click());
    onView(withId(R.id.ffab)).check(matches(isDisplayed()));
    final Status received = createStatus(28000);
    receiveStatuses(createStatus(26000), received);
    onView(withId(R.id.action_heading)).perform(click());
    onView(withId(R.id.ffab)).check(matches(not(isDisplayed())));
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(received.getText()))));
  }

  @Test
  public void createFavAfterRelaunch_then_success() throws Exception {
    // setup
    launchHomeAndBackToApp();
    setupCreateFavorite(0, 1);
    // exec.
    onView(ofStatusViewAt(R.id.timeline, 0)).perform(click());
    onView(withId(R.id.iffab_ffab)).perform(swipeUp());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(selectedDescendantsMatch(withId(R.id.tl_favcount), withText("1")));
  }

  @Test
  public void receiveStatusWhenStatusIsSelected_then_timelineIsNotScrolled() throws Exception {
    final Status target = findByStatusId(20000);
    onView(ofStatusViewAt(R.id.timeline, 0)).perform(click())
        .check(matches(ofStatusView(withText(target.getText()))));
    final Status received = createStatus(22000);
    receiveStatuses(received);

    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(target.getText()))));
    onView(withId(R.id.action_heading)).perform(click());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(received.getText()))));
    onView(ofStatusViewAt(R.id.timeline, 1))
        .check(matches(ofStatusView(withText(target.getText()))));
  }

  @Test
  public void receiveStatusWhenUserInfoIsAppeared_then_timelineIsNotScrolled() throws Exception {
    final Status top = findByStatusId(20000);
    onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(withText(top.getText()))));
    onView(asUserIcon(R.id.tl_icon, ofStatusViewAt(R.id.timeline, 0))).perform(click());
    final Status received22 = createStatus(22000);
    receiveStatuses(received22);
    Espresso.pressBack();

    onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(withText(top.getText()))));

    final Status received25 = createStatus(25000);
    receiveStatuses(received25);
    onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(withText(top.getText()))));

    onView(withId(R.id.action_heading)).perform(click());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(received25.getText()))));
    onView(ofStatusViewAt(R.id.timeline, 1))
        .check(matches(ofStatusView(withText(received22.getText()))));
    onView(ofStatusViewAt(R.id.timeline, 2))
        .check(matches(ofStatusView(withText(top.getText()))));
  }

  private void launchHomeAndBackToApp() throws InterruptedException {
    Intent home = new Intent();
    home.setAction(Intent.ACTION_MAIN);
    home.addCategory(Intent.CATEGORY_HOME);
    home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    Intent relaunch = new Intent(rule.getActivity(), rule.getActivity().getClass());
    relaunch.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

    InstrumentationRegistry.getTargetContext().startActivity(home);
    Thread.sleep(500);
    rule.getActivity().startActivity(relaunch);
  }

  @Override
  protected ActivityTestRule<MainActivity> getRule() {
    return rule;
  }
}
