/*
 * Copyright (c) 2016. Akihito Matsuda (akihito104)
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
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createText;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.receiveStatuses;
import static org.hamcrest.Matchers.not;

/**
 * Created by akihit on 2016/07/01.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityResumeInstTest extends MainActivityInstTestBase {
  @Rule
  public ActivityTestRule<MainActivity> rule
      = new ActivityTestRule<>(MainActivity.class, false, false);

  @Test
  public void heading_then_latestTweetAppears() throws Exception {
    onView(ofStatusViewAt(R.id.timeline, 0)).perform(click());
    receiveStatuses(app.getUserStreamListener(),
        createStatus(21), createStatus(22));
    onView(withId(R.id.action_heading)).perform(click());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(createText(22)))));
    onView(withId(R.id.ffab)).check(matches(not(isDisplayed())));
  }

  @Test
  public void receiveStatusesAfterRelaunch_then_latestTweetAppers() throws Exception {
    launchHomeAndBackToApp();
    Thread.sleep(200);

    receiveStatuses(app.getUserStreamListener(),
        createStatus(21), createStatus(22));
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(createText(22)))));
  }

  @Test
  public void headingAfterRelaunch_then_latestTweetAppears() throws Exception {
    launchHomeAndBackToApp();

    onView(ofStatusViewAt(R.id.timeline, 0)).perform(click());
    onView(withId(R.id.ffab)).check(matches(isDisplayed()));
    receiveStatuses(app.getUserStreamListener(),
        createStatus(26), createStatus(28));
    onView(withId(R.id.action_heading)).perform(click());
    onView(withId(R.id.ffab)).check(matches(not(isDisplayed())));
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(createText(28)))));
  }

  @Test
  public void createFavAfterRelaunch_then_success() throws Exception {
    launchHomeAndBackToApp();

    onView(ofStatusViewAt(R.id.timeline, 0)).perform(click());
    onView(withId(R.id.ffab)).perform(swipeUp());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(selectedDescendantsMatch(withId(R.id.tl_favcount), withText("1")));
  }

  @Test
  public void receiveStatusWhenStatusIsSelected_then_timelineIsNotScrolled() throws Exception {
    onView(ofStatusViewAt(R.id.timeline, 0)).perform(click())
        .check(matches(ofStatusView(withText(createText(20)))));
    receiveStatuses(app.getUserStreamListener(), createStatus(22));

    onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(withText(createText(20)))));
    onView(withId(R.id.action_heading)).perform(click());
    onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(withText(createText(22)))));
    onView(ofStatusViewAt(R.id.timeline, 1)).check(matches(ofStatusView(withText(createText(20)))));
  }

  @Test
  public void receiveStatusWhenUserInfoIsAppeared_then_timelineIsNotScrolled() throws InterruptedException {
    onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(withText(createText(20)))));
    onView(asUserIcon(R.id.tl_icon, ofStatusViewAt(R.id.timeline, 0))).perform(click());
    receiveStatuses(app.getUserStreamListener(), createStatus(22));
    Espresso.pressBack();

    onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(withText(createText(20)))));

    receiveStatuses(app.getUserStreamListener(), createStatus(25));
    onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(withText(createText(20)))));

    onView(withId(R.id.action_heading)).perform(click());
    onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(withText(createText(25)))));
    onView(ofStatusViewAt(R.id.timeline, 1)).check(matches(ofStatusView(withText(createText(22)))));
    onView(ofStatusViewAt(R.id.timeline, 2)).check(matches(ofStatusView(withText(createText(20)))));
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
