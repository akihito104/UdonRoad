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

import android.support.test.espresso.Espresso;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.freshdigitable.udonroad.util.PerformUtil;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.User;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusViewAt;
import static org.mockito.Mockito.when;

/**
 * Created by akihit on 2016/07/28.
 */
@RunWith(AndroidJUnit4.class)
public class MainToUserInfoActivityInstTest extends TimelineInstTestBase {
  @Rule
  public ActivityTestRule<MainActivity> rule
      = new ActivityTestRule<>(MainActivity.class, false, false);
  private Matcher<View> screenNameMatcher;

  @Override
  protected int setupTimeline() throws TwitterException {
    final int initCount = setupDefaultTimeline();
    setupDefaultUserInfoTimeline();
    return initCount;
  }

  @Override
  public void setup() throws Exception {
    super.setup();
    screenNameMatcher = withText("@" + getLoginUser().getScreenName());
  }

  @Test
  public void clickUserIcon_then_launchUserInfoActivity() throws Exception {
    // setup
    final User loginUser = getLoginUser();
    final ResponseList<Status> defaultResponseList = createDefaultResponseList(loginUser);
    when(twitter.getUserTimeline(loginUser.getId())).thenReturn(defaultResponseList);
    // exec.
    onView(withId(R.id.main_toolbar)).check(matches(isAssignableFrom(Toolbar.class)));
    onView(withId(R.id.main_toolbar)).check(matches(withToolbarTitle("Home")));
    PerformUtil.clickUserIconAt(0);
    onView(withId(R.id.user_screen_name)).check(matches(screenNameMatcher));
    onView(ofStatusView(withText(findByStatusId(20000).getText())))
        .check(matches(isDisplayed()));
    // tear down
    Espresso.pressBack();
    onView(withId(R.id.main_toolbar)).check(matches(withToolbarTitle("Home")));
  }

  private Matcher<View> withToolbarTitle(final String title) {
    final Matcher<View> titleMatcher = withText(title);
    return new BoundedMatcher<View, Toolbar>(Toolbar.class) {
      @Override
      public void describeTo(Description description) {
        titleMatcher.describeTo(description);
      }

      @Override
      protected boolean matchesSafely(Toolbar item) {
        return item.getTitle().equals(title);
      }
    };
  }

  @Test
  public void launchUserInfoTwiceAndBackMain_then_launchUserInfo() {
    PerformUtil.clickUserIconAt(0);
    onView(withId(R.id.user_screen_name)).check(matches(screenNameMatcher));
    Espresso.pressBack();
    PerformUtil.clickUserIconAt(0);
    onView(withId(R.id.user_screen_name)).check(matches(screenNameMatcher));
    Espresso.pressBack();
    PerformUtil.clickUserIconAt(0);
    onView(withId(R.id.user_screen_name)).check(matches(screenNameMatcher));

    // tear down
    Espresso.pressBack();
    onView(withId(R.id.main_toolbar)).check(matches(withToolbarTitle("Home")));
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
  }

  @Override
  protected ActivityTestRule<MainActivity> getRule() {
    return rule;
  }
}
