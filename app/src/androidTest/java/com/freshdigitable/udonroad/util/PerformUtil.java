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

import android.app.Activity;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.action.CoordinatesProvider;
import android.support.test.espresso.action.GeneralClickAction;
import android.support.test.espresso.action.GeneralLocation;
import android.support.test.espresso.action.GeneralSwipeAction;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Swipe;
import android.support.test.espresso.action.Tap;
import android.support.test.espresso.contrib.DrawerActions;
import android.view.View;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.listitem.QuotedStatusView;
import com.freshdigitable.udonroad.listitem.StatusView;

import org.hamcrest.Matcher;

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

  public static void selectQuotedItemView(Matcher<View> viewMatcher) {
    onView(viewMatcher).perform(clickForStatusView());
  }

  public static ViewInteraction selectItemViewAt(int index) {
    return onView(ofStatusViewAt(R.id.timeline, index)).perform(clickForStatusView());
  }

  public static ViewInteraction reply() {
    return onIFFAB().perform(swipeDown());
  }

  public static ViewInteraction showDetail() {
    return onIFFAB().perform(swipeLeft());
  }

  public static ViewInteraction favo() {
    return onIFFAB().perform(swipeUp());
  }

  public static ViewInteraction retweet() {
    return onIFFAB().perform(swipeRight());
  }

  public static ViewInteraction fav_retweet() {
    final ViewAction viewAction = actionWithAssertions(
        new GeneralSwipeAction(Swipe.FAST, GeneralLocation.CENTER,
            view -> {
              final float[] pos = GeneralLocation.TOP_RIGHT.calculateCoordinates(view);
              pos[0] += 0.5f * view.getWidth();
              pos[1] += -0.5f * view.getHeight();
              return pos;
            }, Press.FINGER));
    return onIFFAB().perform(viewAction);
  }

  private static ViewInteraction onIFFAB() {
    return onView(withId(R.id.ffab));
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

  public static void openDrawerNavigation() {
    onView(withId(R.id.nav_drawer_layout)).perform(DrawerActions.open());
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

  public static void launchHomeAndBackToApp(Activity base) throws InterruptedException {
    Intent home = new Intent();
    home.setAction(Intent.ACTION_MAIN);
    home.addCategory(Intent.CATEGORY_HOME);
    home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    Intent relaunch = new Intent(base, base.getClass());
    relaunch.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

    InstrumentationRegistry.getTargetContext().startActivity(home);
    Thread.sleep(500);
    base.startActivity(relaunch);
  }

  private PerformUtil() {
    throw new AssertionError();
  }
}
