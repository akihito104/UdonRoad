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
import android.support.v7.widget.Toolbar;

import com.freshdigitable.udonroad.util.PerformUtil;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.List;

import twitter4j.Relationship;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.User;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.AssertionUtil.anywayNotVisible;
import static com.freshdigitable.udonroad.util.AssertionUtil.checkMainActivityTitle;
import static com.freshdigitable.udonroad.util.IdlingResourceUtil.getSimpleIdlingResource;
import static com.freshdigitable.udonroad.util.IdlingResourceUtil.runWithIdlingResource;
import static com.freshdigitable.udonroad.util.MatcherUtil.onIFFAB;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusViewAt;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatus;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by akihit on 2016/07/28.
 */
@RunWith(AndroidJUnit4.class)
public class MainToUserInfoActivityInstTest extends TimelineInstTestBase {
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
  public void clickUserIcon_then_launchUserInfoActivity() throws Exception {
    // setup
    final User loginUser = getLoginUser();
    final ResponseList<Status> defaultResponseList = createDefaultResponseList(loginUser);
    when(twitter.getUserTimeline(loginUser.getId())).thenReturn(defaultResponseList);
    // exec.
    onView(withId(R.id.main_toolbar)).check(matches(isAssignableFrom(Toolbar.class)));
    checkMainActivityTitle(R.string.title_home);
    PerformUtil.clickUserIconAt(0);
    onView(withId(R.id.user_screen_name)).check(matches(screenNameMatcher));
    onView(ofStatusView(withText(findByStatusId(20000).getText())))
        .check(matches(isDisplayed()));
    // tear down
    Espresso.pressBack();
    checkMainActivityTitle(R.string.title_home);
    onIFFAB().check(anywayNotVisible());
  }

  @Test
  public void launchUserInfoTwiceAndBackMain_then_launchUserInfo() {
    PerformUtil.clickUserIconAt(0);
    onView(withId(R.id.user_screen_name)).check(matches(screenNameMatcher));
    Espresso.pressBack();
    onIFFAB().check(anywayNotVisible());

    PerformUtil.clickUserIconAt(0);
    onView(withId(R.id.user_screen_name)).check(matches(screenNameMatcher));
    Espresso.pressBack();
    onIFFAB().check(anywayNotVisible());

    PerformUtil.clickUserIconAt(0);
    onView(withId(R.id.user_screen_name)).check(matches(screenNameMatcher));

    // tear down
    Espresso.pressBack();
    checkMainActivityTitle(R.string.title_home);
    onIFFAB().check(anywayNotVisible());
  }

  @Test
  public void openTweetInputViewAndClose_and_launchUserInfoAndBackToMain()
      throws Exception {
    PerformUtil.clickWriteOnMenu();
    PerformUtil.clickCancelWriteOnMenu();
    PerformUtil.clickUserIconAt(0);
    onView(withId(R.id.user_screen_name)).check(matches(screenNameMatcher));
    Espresso.pressBack();

    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(findByStatusId(20000).getText()))));
    onIFFAB().check(anywayNotVisible());
  }

  @Test
  public void openDrawerAndClickUserIcon_then_launchUserInfo() {
    PerformUtil.openDrawerNavigation();
    onView(withId(R.id.nav_header_icon)).perform(click());
    onView(withId(R.id.user_screen_name)).check(matches(screenNameMatcher));

    Espresso.pressBack();
    checkMainActivityTitle(R.string.title_home);
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
    onIFFAB().check(anywayNotVisible());

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

  @Override
  protected ActivityTestRule<MainActivity> getRule() {
    return rule;
  }
}
