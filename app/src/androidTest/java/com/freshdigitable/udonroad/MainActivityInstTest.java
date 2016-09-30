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

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import twitter4j.Status;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.action.ViewActions.swipeUp;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.assertion.ViewAssertions.selectedDescendantsMatch;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.StatusViewAssertion.recyclerViewDescendantsMatches;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusViewAt;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createRtStatus;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatus;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createText;
import static org.hamcrest.CoreMatchers.not;

/**
 * Created by akihit on 2016/06/15.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityInstTest extends MainActivityInstTestBase {
  @Rule
  public ActivityTestRule<MainActivity> rule
      = new ActivityTestRule<>(MainActivity.class, false, false);

  @Test
  public void receive2ReverseStatusIdOrderTweetsAtSameTime_and_displayStatusIdOrder() throws Exception {
    receiveStatuses(createStatus(25));
    onView(ofStatusView(withText(createText(25))))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 0));
    onView(ofStatusView(withText(createText(25))))
        .check(selectedDescendantsMatch(withId(R.id.tl_fav_icon), not(isDisplayed())));

    receiveStatuses(
        createStatus(29),
        createStatus(27));
    onView(ofStatusView(withText(createText(25))))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 2));
    onView(ofStatusView(withText(createText(29))))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 0));
    onView(ofStatusView(withText(createText(27))))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 1));
  }

  @Test
  public void receiveDelayed2ReverseStatusIdOrderTweetsAtSameTime_and_displayStatusIdOrder()
      throws Exception {
    receiveStatuses(createStatus(25));
    onView(ofStatusView(withText(createText(25))))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 0));

    receiveStatuses(
        createStatus(29),
        createStatus(23));
    onView(ofStatusView(withText(createText(25))))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 1));
    onView(ofStatusView(withText(createText(29))))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 0));
    onView(ofStatusView(withText(createText(23))))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 2));
  }

  @Test
  public void fetchFav_then_favIconAndCountAreDisplayed() throws Exception {
    onView(ofStatusView(withText(createText(20)))).perform(click());
    onView(withId(R.id.iffab_ffab)).check(matches(isDisplayed()));
    onView(withId(R.id.iffab_ffab)).perform(swipeUp());
    onView(ofStatusView(withText(createText(20))))
        .check(selectedDescendantsMatch(withId(R.id.tl_favcount), withText("1")));
    // TODO tint color check
  }

  @Test
  public void fetchRT_then_RtIconAndCountAreDisplayed() throws Exception {
    onView(ofStatusView(withText(createText(20)))).perform(click());
    onView(withId(R.id.ffab)).check(matches(isDisplayed()));
    onView(withId(R.id.iffab_ffab)).perform(swipeRight());
    receiveStatuses(createRtStatus(rtStatusId, 20, false));

    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(createText(20)))));
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(selectedDescendantsMatch(withId(R.id.tl_rtcount), withText("1")));
    onView(withId(R.id.timeline)).perform(swipeDown());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(selectedDescendantsMatch(withId(R.id.tl_rtcount), withText("1")));
    // TODO tint color check
  }

  @Test
  public void receiveStatusDeletionNoticeForLatestStatus_then_removedTheTopOfTimeline()
      throws Exception {
    final Status target = createStatus(20);
    receiveDeletionNotice(target);

    onView(ofStatusView(withText(createText(20)))).check(doesNotExist());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(createText(19)))));
  }

  @Test
  public void receiveStatusDeletionNoticeForRTStatus_then_removedRTStatus()
      throws Exception {
    onView(ofStatusView(withText(createText(20)))).perform(click());
    onView(withId(R.id.iffab_ffab)).perform(swipeRight());
    final Status target = createRtStatus(rtStatusId, 20, false);
    receiveStatuses(target);
    onView(withId(R.id.timeline)).perform(swipeDown());
    receiveDeletionNotice(target);

    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(createText(20)))));
  }

  @Test
  public void receiveStatusDeletionNoticeForRTingStatus_then_removedOriginalAndRTedStatuses()
      throws Exception {
    onView(ofStatusView(withText(createText(20)))).perform(click());
    onView(withId(R.id.iffab_ffab)).perform(swipeRight());
    final Status targetRt = createRtStatus(rtStatusId, 20, false);
    receiveStatuses(targetRt);
    final Status target = createStatus(20);
    receiveDeletionNotice(target, targetRt);

    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(createText(19)))));
    onView(ofStatusView(withText(createText(20)))).check(doesNotExist());
  }

  @Test
  public void receiveStatusDeletionNoticeForFavedStatus_then_removedOriginalStatuses()
      throws Exception {
    onView(ofStatusView(withText(createText(20)))).perform(click());
    onView(withId(R.id.iffab_ffab)).perform(swipeUp());
    final Status target = createStatus(20);
    receiveDeletionNotice(target);

    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(createText(19)))));
    onView(ofStatusView(withText(createText(20)))).check(doesNotExist());
  }

  @Test
  public void receive2StatusDeletionNoticeAtSameTime_then_removed2Statuses()
      throws Exception {
    receiveDeletionNotice(createStatus(18), createStatus(20));

    onView(ofStatusView(withText(createText(20)))).check(doesNotExist());
    onView(ofStatusView(withText(createText(18)))).check(doesNotExist());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(createText(19)))));
  }

  @Test
  public void receiveStatusDeletionNoticeForSelectedStatus_then_removedSelectedStatus()
      throws Exception {
    onView(ofStatusViewAt(R.id.timeline, 0)).perform(click());
    receiveDeletionNotice(createStatus(20));

    onView(ofStatusView(withText(createText(20)))).check(doesNotExist());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(createText(19)))));
  }

  @Test
  public void clickSendIcon_then_openTweetInputViewAndShowFab() {
    // open
    onView(withId(R.id.action_write)).perform(click());
    onView(withId(R.id.main_tweet_input_view)).check(matches(isDisplayed()));
    onView(withId(R.id.main_send_tweet)).check(matches(isDisplayed()));
    onView(withId(R.id.action_cancel)).check(matches(isDisplayed()));
    // the menu is not matched any view so always fail.
//    onView(withId(R.id.action_write)).check(matches(not(isDisplayed())));

    // close
    onView(withId(R.id.action_cancel)).perform(click());
    onView(withId(R.id.action_write)).check(matches(isDisplayed()));
//    onView(withId(R.id.main_tweet_input_view)).check(matches(not(isDisplayed())));
    onView(withId(R.id.main_send_tweet)).check(matches(not(isDisplayed())));
  }

  @Override
  protected ActivityTestRule<MainActivity> getRule() {
    return rule;
  }
}