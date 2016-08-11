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

import android.support.test.espresso.Espresso;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.Toolbar;
import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.asUserIcon;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusViewAt;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createText;

/**
 * Created by akihit on 2016/07/28.
 */
@RunWith(AndroidJUnit4.class)
public class MainToUserInfoActivityInstTest extends MainActivityInstTestBase {
  @Rule
  public ActivityTestRule<MainActivity> rule
      = new ActivityTestRule<>(MainActivity.class, false, false);

  @Test
  public void clickUserIcon_then_launchUserInfoActivity() throws Exception {
    onView(withId(R.id.main_toolbar)).check(matches(isAssignableFrom(Toolbar.class)));
    onView(withId(R.id.main_toolbar)).check(matches(withToolbarTitle("Home")));
    onView(asUserIcon(R.id.tl_icon, ofStatusViewAt(R.id.timeline, 0))).perform(click());
    onView(withId(R.id.user_screen_name)).check(matches(withText("@akihito104")));
    onView(ofStatusView(withText(createText(20)))).check(matches(isDisplayed()));
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
    onView(asUserIcon(R.id.tl_icon, ofStatusViewAt(R.id.timeline, 0))).perform(click());
    onView(withId(R.id.user_screen_name)).check(matches(withText("@akihito104")));
    Espresso.pressBack();
    onView(asUserIcon(R.id.tl_icon, ofStatusViewAt(R.id.timeline, 0))).perform(click());
    onView(withId(R.id.user_screen_name)).check(matches(withText("@akihito104")));
    Espresso.pressBack();
    onView(asUserIcon(R.id.tl_icon, ofStatusViewAt(R.id.timeline, 0))).perform(click());
    onView(withId(R.id.user_screen_name)).check(matches(withText("@akihito104")));

    // tear down
    Espresso.pressBack();
    onView(withId(R.id.main_toolbar)).check(matches(withToolbarTitle("Home")));
  }

  @Override
  protected ActivityTestRule<MainActivity> getRule() {
    return rule;
  }
}