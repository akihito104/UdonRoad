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

import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.action.CoordinatesProvider;
import android.support.test.espresso.action.GeneralClickAction;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Tap;
import android.view.View;

import com.freshdigitable.udonroad.QuotedStatusView;
import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.StatusView;

import twitter4j.Status;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.actionWithAssertions;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.action.ViewActions.swipeUp;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.asUserIcon;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofQuotedStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusViewAt;

/**
 * Created by akihit on 2016/10/20.
 */
public class PerformUtil {
  public static ViewInteraction selectItemView(Status target) {
    return onView(ofStatusView(withText(target.getText()))).perform(clickForStatusView());
  }

  public static ViewInteraction selectQuotedItemView(Status target) {
    return onView(ofQuotedStatusView(withText(target.getText()))).perform(clickForStatusView());
  }

  public static ViewInteraction selectItemViewAt(int index) {
    return onView(ofStatusViewAt(R.id.timeline, index)).perform(clickForStatusView());
  }

  public static ViewInteraction reply() {
    return onView(withId(R.id.iffab_ffab)).perform(swipeDown());
  }

  public static ViewInteraction showDetail() {
    return onView(withId(R.id.iffab_ffab)).perform(swipeLeft());
  }

  public static ViewInteraction favo() {
    return onView(withId(R.id.iffab_ffab)).perform(swipeUp());
  }

  public static ViewInteraction retweet() {
    return onView(withId(R.id.iffab_ffab)).perform(swipeRight());
  }

  public static ViewInteraction pullDownTimeline() {
    return onView(withId(R.id.timeline)).perform(swipeDown());
  }

  public static ViewInteraction clickWriteOnMenu() {
    return onView(withId(R.id.action_write)).perform(click());
  }

  public static ViewInteraction clickCancelWriteOnMenu() {
    return onView(withId(R.id.action_cancel)).perform(click());
  }

  public static ViewInteraction clickHeadingOnMenu() {
    return onView(withId(R.id.action_heading)).perform(click());
  }

  public static ViewInteraction clickUserIconAt(int index) {
    return onView(asUserIcon(R.id.tl_icon, ofStatusViewAt(R.id.timeline, index))).perform(click());
  }

  private static ViewAction clickForStatusView() {
    return actionWithAssertions(new GeneralClickAction(Tap.SINGLE, new CoordinatesProvider() {
      @Override
      public float[] calculateCoordinates(View view) {
        if (view instanceof StatusView) {
          final View v = view.findViewById(R.id.tl_tweet);
          return calcCenterCoord(v);
        } else if (view instanceof QuotedStatusView) {
          final View v = view.findViewById(R.id.q_tweet);
          return calcCenterCoord(v);
        }
        return calcCenterCoord(view);
      }

      private float[] calcCenterCoord(View v) {
        int[] pos = new int[2];
        v.getLocationOnScreen(pos);
        return new float[]{
            pos[0] + v.getWidth() / 2.0f,
            pos[1] + v.getHeight() / 2.0f,
        };
      }
    }, Press.FINGER));
  }

  public PerformUtil() {
    throw new AssertionError();
  }
}
