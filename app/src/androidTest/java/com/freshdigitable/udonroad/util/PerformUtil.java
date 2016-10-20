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
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofQuotedStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusViewAt;

/**
 * Created by akihit on 2016/10/20.
 */
public class PerformUtil {
  public static void selectItemView(Status target) {
    onView(ofStatusView(withText(target.getText()))).perform(clickForStatusView());
  }

  public static void selectQuotedItemView(Status target) {
    onView(ofQuotedStatusView(withText(target.getText()))).perform(clickForStatusView());
  }

  public static void selectItemViewAt(int index) {
    onView(ofStatusViewAt(R.id.timeline, index)).perform(clickForStatusView());
  }

  public static void reply() {
    onView(withId(R.id.iffab_ffab)).perform(swipeDown());
  }

  public static void showDetail() {
    onView(withId(R.id.iffab_ffab)).perform(swipeLeft());
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
