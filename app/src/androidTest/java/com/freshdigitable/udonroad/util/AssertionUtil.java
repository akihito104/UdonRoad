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

import com.freshdigitable.udonroad.R;

import twitter4j.Status;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.selectedDescendantsMatch;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofQuotedStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusViewAt;
import static org.hamcrest.Matchers.not;

/**
 * Created by akihit on 2016/11/25.
 */

public class AssertionUtil {

  public static void checkFavCountAt(int index, int expectedCount) {
    onView(ofStatusViewAt(R.id.timeline, index))
        .check(selectedDescendantsMatch(
            withId(R.id.tl_favcount),
            withText(Integer.toString(expectedCount))));
  }

  public static void checkFavCount(Status status, int expectedCount) {
    onView(ofStatusView(withText(status.getText())))
        .check(selectedDescendantsMatch(
            withId(R.id.tl_favcount),
            withText(Integer.toString(expectedCount))));
  }

  public static void checkFavCountDoesNotExist(Status status) {
    onView(ofStatusView(withText(status.getText())))
        .check(selectedDescendantsMatch(
            withId(R.id.tl_favcount), not(isDisplayed())));
  }

  public static void checkFavCountForQuoted(Status quotedStatus, int expectedCount) {
    onView(ofQuotedStatusView(withText(quotedStatus.getText())))
        .check(selectedDescendantsMatch(withId(R.id.q_favcount),
            withText(Integer.toString(expectedCount))));
  }

  public static void checkRTCountAt(int index, int expectedCount) {
    onView(ofStatusViewAt(R.id.timeline, index))
        .check(selectedDescendantsMatch(
            withId(R.id.tl_rtcount),
            withText(Integer.toString(expectedCount))));
  }

  private AssertionUtil() {}
}
