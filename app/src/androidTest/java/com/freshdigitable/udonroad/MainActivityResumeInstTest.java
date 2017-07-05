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

import com.freshdigitable.udonroad.util.AssertionUtil;
import com.freshdigitable.udonroad.util.PerformUtil;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import twitter4j.Relationship;
import twitter4j.Status;
import twitter4j.TwitterException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusViewAt;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatus;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;

/**
 * MainActivityResumeInstTest tests MainActivity can resume from home button pushed.
 *
 * Created by akihit on 2016/07/01.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityResumeInstTest extends TimelineInstTestBase {
  @Rule
  public final ActivityTestRule<MainActivity> rule
      = new ActivityTestRule<>(MainActivity.class, false, false);

  @Override
  protected int setupTimeline() throws TwitterException {
    final int initCount = setupDefaultTimeline();
    setupUserInfoTimeline(mock(Relationship.class));
    return initCount;
  }

  @Test
  public void heading_then_latestTweetAppears() throws Exception {
    PerformUtil.selectItemViewAt(0);
    final Status received = createStatus(22000);
    receiveStatuses(createStatus(21000), received);
    PerformUtil.clickHeadingOnMenu();
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(received.getText()))));
    onView(withId(R.id.ffab)).check(matches(not(isDisplayed())));
  }

  @Test
  public void receiveStatusesAfterRelaunch_then_latestTweetAppears() throws Exception {
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

    PerformUtil.selectItemViewAt(0);
    onView(withId(R.id.ffab)).check(matches(isDisplayed()));
    final Status received = createStatus(28000);
    receiveStatuses(createStatus(26000), received);
    PerformUtil.clickHeadingOnMenu();
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
    PerformUtil.selectItemViewAt(0);
    PerformUtil.favo();
    Espresso.pressBack();
    AssertionUtil.checkFavCountAt(0, 1);
  }

  @Test
  public void receiveStatusWhenStatusIsSelected_then_timelineIsNotScrolled() throws Exception {
    final Status target = findByStatusId(20000);
    PerformUtil.selectItemViewAt(0).check(matches(ofStatusView(withText(target.getText()))));
    final Status received = createStatus(22000);
    receiveStatuses(received);

    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(target.getText()))));
    PerformUtil.clickHeadingOnMenu();
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(received.getText()))));
    onView(ofStatusViewAt(R.id.timeline, 1))
        .check(matches(ofStatusView(withText(target.getText()))));
  }

  @Test
  public void receiveStatusWhenUserInfoIsAppeared_then_timelineIsNotScrolled() throws Exception {
    final Status top = findByStatusId(20000);
    onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(withText(top.getText()))));
    PerformUtil.clickUserIconAt(0);
    final Status received22 = createStatus(22000);
    receiveStatuses(received22);
    Espresso.pressBack();

    onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(withText(top.getText()))));

    final Status received25 = createStatus(25000);
    receiveStatuses(received25);
    onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(withText(top.getText()))));

    PerformUtil.clickHeadingOnMenu();
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
