/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.Toolbar;
import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
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
}
