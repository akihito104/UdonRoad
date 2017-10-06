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

import android.app.Activity;
import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.support.v4.app.Fragment;

import com.freshdigitable.udonroad.util.AssertionUtil;
import com.freshdigitable.udonroad.util.IdlingResourceUtil;
import com.freshdigitable.udonroad.util.PerformUtil;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.List;

import twitter4j.Relationship;
import twitter4j.Status;
import twitter4j.TwitterException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.IdlingResourceUtil.getActivityStageIdlingResource;
import static com.freshdigitable.udonroad.util.IdlingResourceUtil.getSimpleIdlingResource;
import static com.freshdigitable.udonroad.util.IdlingResourceUtil.runWithIdlingResource;
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
  public void receiveStatusesAfterRelaunch_then_latestTweetAppears() throws Exception {
    launchHome();

    final Status received = createStatus(22000);
    receiveStatuses(createStatus(21000), received);
    PerformUtil.clickHeadingOnMenu();
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(received.getText()))));
  }

  @Test
  public void headingAfterRelaunch_then_latestTweetAppears() throws Exception {
    launchHome();

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
    launchHome();
    setupCreateFavorite(0, 1);
    // exec.
    PerformUtil.selectItemViewAt(0);
    PerformUtil.favo();
    Espresso.pressBack();
    AssertionUtil.checkFavCountAt(0, 1);
  }

  @Test
  public void receiveStatusWhenUserInfoIsAppeared_then_timelineIsNotScrolled() throws Exception {
    final Status top = findByStatusId(20000);
    onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(withText(top.getText()))));

    PerformUtil.clickUserIconAt(0);
    runWithIdlingResource(getSimpleIdlingResource("userInfo", () -> {
      final Collection<Activity> activities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
      for (Activity a : activities) {
        if (!(a instanceof UserInfoActivity)) {
          continue;
        }
        final List<Fragment> fragments = ((UserInfoActivity) a).getSupportFragmentManager().getFragments();
        for (Fragment f : fragments) {
          if (f instanceof UserInfoPagerFragment) {
            return true;
          }
        }
      }
      return false;
    }), () ->
        onView(withText("TWEET\n20")).check(matches(isDisplayed())));

    final Status received22 = createStatus(22000);
    receiveStatuses(false, received22);
    Espresso.pressBack();
    runWithIdlingResource(getSimpleIdlingResource("back to main", () ->
        getTimelineView().getAdapter().getItemCount() == 21), () ->
        onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(withText(top.getText())))));

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

  private void launchHome() throws InterruptedException {
    PerformUtil.launchHomeAndBackToApp(rule.getActivity());
    IdlingResourceUtil.runWithIdlingResource(
        getActivityStageIdlingResource("relaunch", MainActivity.class, Stage.RESUMED), () ->
            onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(isDisplayed())));
  }

  @Override
  protected ActivityTestRule<MainActivity> getRule() {
    return rule;
  }
}
