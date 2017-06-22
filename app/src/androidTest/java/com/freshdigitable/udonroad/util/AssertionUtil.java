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

import android.view.View;

import com.freshdigitable.udonroad.R;

import org.hamcrest.Matcher;

import twitter4j.Status;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.selectedDescendantsMatch;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.getFavIcon;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.getFavIconOfQuoted;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.getHasReplyIcon;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.getRTIcon;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofQuotedStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusViewAt;
import static org.hamcrest.Matchers.not;

/**
 * Created by akihit on 2016/11/25.
 */

public class AssertionUtil {

  public static void checkFavCountAt(int index, int expectedCount) {
    checkFavCount(ofStatusViewAt(R.id.timeline, index), expectedCount);
  }

  public static void checkFavCount(Status status, int expectedCount) {
    checkFavCount(ofStatusView(withText(status.getText())), expectedCount);
  }

  public static void checkFavCountDoesNotExist(Status status) {
    checkFavCount(status, 0);
  }

  private static void checkFavCount(Matcher<View> root, int expectedCount) {
    final int visibility = expectedCount > 0 ? View.VISIBLE : View.INVISIBLE;
    checkReactionIcon(root, getFavIcon(), visibility, expectedCount);
  }

  public static void checkFavCountForQuoted(Status quotedStatus, int expectedCount) {
    checkReactionIcon(ofQuotedStatusView(withText(quotedStatus.getText())), getFavIconOfQuoted(),
        expectedCount > 0 ? View.VISIBLE : View.INVISIBLE, expectedCount);
  }

  public static void checkRTCountAt(int index, int expectedCount) {
    checkReactionIcon(ofStatusViewAt(R.id.timeline, index), getRTIcon(),
        expectedCount > 0 ? View.VISIBLE : View.INVISIBLE, expectedCount);
  }

  public static void checkHasReplyTo(Status status) {
    onView(ofStatusView(withText(status.getText())))
        .check(selectedDescendantsMatch(getHasReplyIcon(), isDisplayed()));
  }

  private static void checkReactionIcon(Matcher<View> root, Matcher<View> icon,
                                        int expectedVisibility, int expectedCount) {
    final Matcher<View> displayed = expectedVisibility == View.VISIBLE ? isDisplayed() : not(isDisplayed());
    onView(root).check(selectedDescendantsMatch(icon, displayed));
    if (expectedVisibility == View.VISIBLE) {
      onView(root).check(selectedDescendantsMatch(icon, withText(Integer.toString(expectedCount))));
    }
  }

  private AssertionUtil() {}
}
