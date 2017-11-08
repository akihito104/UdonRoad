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

package com.freshdigitable.udonroad.util;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.core.internal.deps.guava.collect.Iterables;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;

import com.freshdigitable.udonroad.R;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import twitter4j.Status;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.assertion.ViewAssertions.selectedDescendantsMatch;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.util.TreeIterables.breadthFirstViewTraversal;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.getFavIcon;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.getFavIconOfQuoted;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.getHasReplyIcon;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.getRTIcon;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofQuotedStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusViewAt;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;

/**
 * Created by akihit on 2016/11/25.
 */

public class AssertionUtil {

  public static void checkFavCountAt(int index, int expectedCount) {
    checkFavCount(ofStatusViewAt(R.id.timeline, index), expectedCount);
  }

  public static void checkFavCount(Status status, int expectedCount) {
    checkFavCount(ofStatusView(status), expectedCount);
  }

  public static void checkFavCountDoesNotExist(Status status) {
    checkFavCount(status, 0);
  }

  private static void checkFavCount(Matcher<View> root, int expectedCount) {
    final int visibility = expectedCount > 0 ? View.VISIBLE : View.INVISIBLE;
    checkReactionIcon(root, getFavIcon(), visibility, expectedCount);
  }

  public static void checkFavCountForQuoted(Status quotedStatus, int expectedCount) {
    checkReactionIcon(ofQuotedStatusView(quotedStatus), getFavIconOfQuoted(),
        expectedCount > 0 ? View.VISIBLE : View.INVISIBLE, expectedCount);
  }

  public static void checkRTCountAt(int index, int expectedCount) {
    checkRTCount(ofStatusViewAt(R.id.timeline, index), expectedCount);
  }

  public static void checkRTCount(Status status, int expectedCount) {
    checkRTCount(ofStatusView(status), expectedCount);
  }

  private static void checkRTCount(Matcher<View> matcher, int expectedCount) {
    checkReactionIcon(matcher, getRTIcon(),
        expectedCount > 0 ? View.VISIBLE : View.INVISIBLE, expectedCount);
  }

  public static void checkHasReplyTo(Status status) {
    onView(ofStatusView(status)).check(selectedDescendantsMatch(getHasReplyIcon(), isDisplayed()));
  }

  public static void checkReactionIcon(Matcher<View> root, Matcher<View> icon,
                                        int expectedVisibility, int expectedCount) {
    final Matcher<View> displayed = expectedVisibility == View.VISIBLE ? isDisplayed() : not(isDisplayed());
    onView(root).check(selectedDescendantsMatch(icon, displayed));
    if (expectedVisibility == View.VISIBLE) {
      onView(root).check(selectedDescendantsMatch(icon, withText(Integer.toString(expectedCount))));
    }
  }

  public static void checkMainActivityTitle(@StringRes int titleRes) {
    final Matcher<View> titleMatcher = withText(titleRes);
    onView(withId(R.id.main_toolbar)).check(matches(getToolbarMatcher(titleMatcher)));
  }

  public static void checkMainActivityTitle(String title) {
    final Matcher<View> titleMatcher = withText(title);
    onView(withId(R.id.main_toolbar)).check(matches(getToolbarMatcher(titleMatcher)));
  }

  @NonNull
  private static Matcher<View> getToolbarMatcher(Matcher<View> titleMatcher) {
    return new BoundedMatcher<View, Toolbar>(Toolbar.class) {
      @Override
      public void describeTo(Description description) {
        titleMatcher.describeTo(description);
      }

      @Override
      protected boolean matchesSafely(Toolbar item) {
        return Iterables.filter(breadthFirstViewTraversal(item),
            view -> view != null && titleMatcher.matches(view)).iterator().hasNext();
      }
    };
  }

  private static void checkUserInfoActivityTitle(Matcher<View> matcher) {
    final Matcher<View> toolbarTitle = withId(R.id.userInfo_toolbar_title);
    final Matcher<View> toolbarMarcher = withId(R.id.userInfo_toolbar);
    onView(matcher).check(matches(new BaseMatcher<View>() {
      @Override
      public void describeTo(Description description) {
        toolbarTitle.describeTo(description);
        toolbarMarcher.describeTo(description);
      }

      @Override
      public boolean matches(Object item) {
        return toolbarTitle.matches(item) || withParent(toolbarMarcher).matches(item);
      }
    }));
  }

  public static void checkUserInfoActivityTitle(@StringRes int titleRes) {
    checkUserInfoActivityTitle(withText(titleRes));
  }

  public static void checkUserInfoActivityTitle(@NonNull String title) {
    if (TextUtils.isEmpty(title)) {
      onView(withId(R.id.userInfo_toolbar)).check(matches(getToolbarMatcher(withText(""))));
      onView(withId(R.id.userInfo_toolbar_title)).check(matches(not(isDisplayed())));
      return;
    }
    checkUserInfoActivityTitle(withText(title));
  }

  @NonNull
  public static ViewAssertion anywayNotVisible() {
    return (view, noViewFoundException) ->
        assertTrue(view == null || view.getVisibility() != View.VISIBLE);
  }

  private AssertionUtil() {}
}
