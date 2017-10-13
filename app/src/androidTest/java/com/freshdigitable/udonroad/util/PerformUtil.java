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
import android.support.test.espresso.action.GeneralClickAction;
import android.support.test.espresso.action.GeneralLocation;
import android.support.test.espresso.action.GeneralSwipeAction;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Swipe;
import android.support.test.espresso.action.Tap;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.espresso.core.internal.deps.guava.base.Function;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.view.View;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.listitem.QuotedStatusView;
import com.freshdigitable.udonroad.listitem.StatusView;
import com.freshdigitable.udonroad.listitem.UserItemView;
import com.freshdigitable.udonroad.media.ThumbnailContainer;
import com.freshdigitable.udonroad.media.ThumbnailView;

import org.hamcrest.Description;
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
import static com.freshdigitable.udonroad.util.MatcherUtil.onIFFAB;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.asUserIcon;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofItemViewAt;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofQuotedStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofRTStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusViewAt;

/**
 * Created by akihit on 2016/10/20.
 */
public class PerformUtil {
  public static ViewInteraction selectItemView(Status target) {
    final Matcher<View> viewMatcher = target.isRetweet() ? ofRTStatusView(withText(target.getText()))
        : ofStatusView(withText(target.getText()));
    return onView(viewMatcher).perform(clickForStatusView());
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

  public static <T extends View> ViewInteraction selectItemViewAt(int index, Class<T> clz) {
    return onView(ofItemViewAt(R.id.timeline, index, clz)).perform(clickForStatusView());
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

  public static ViewInteraction quote() {
    final ViewAction viewAction = actionWithAssertions(
        new GeneralSwipeAction(Swipe.FAST, GeneralLocation.CENTER,
            view -> {
              final float[] pos = GeneralLocation.BOTTOM_RIGHT.calculateCoordinates(view);
              pos[0] += 0.5f * view.getWidth();
              pos[1] += 0.5f * view.getHeight();
              return pos;
            }, Press.FINGER));
    return onIFFAB().perform(viewAction);
  }

  public static ViewInteraction pullDownTimeline() {
    return onView(withId(R.id.timeline)).perform(swipeDown());
  }

  public static ViewInteraction clickWriteOnMenu() {
    return onView(withId(R.id.action_writeTweet)).perform(click());
  }

  public static ViewInteraction clickCancelWriteOnMenu() {
    return MatcherUtil.onCancelWriteMenu().perform(click());
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

  public static void closeDrawerNavigation() {
    onView(withId(R.id.nav_drawer_layout)).perform(DrawerActions.close());
  }

  private static ViewAction clickForStatusView() {
    return actionWithAssertions(new GeneralClickAction(Tap.SINGLE, view -> {
      if (view instanceof StatusView || view instanceof UserItemView) {
        final View v = view.findViewById(R.id.tl_tweet);
        return calcCenterCoord(v);
      } else if (view instanceof QuotedStatusView) {
        final View v = view.findViewById(R.id.q_tweet);
        return calcCenterCoord(v);
      }
      return calcCenterCoord(view);
    }, Press.FINGER, 0, 0));
  }

  public static void clickThumbnailAt(Status hasImage, int thumbIndex) {
    final Matcher<View> viewMatcher = ofStatusView(withText(hasImage.getText()));
    clickThumbnailAt(item -> {
      final ThumbnailContainer container = (ThumbnailContainer) item.getParent();
      final View v = container.getChildAt(thumbIndex);
      return viewMatcher.matches(container.getParent()) && v == item;
    }, thumbIndex);
  }

  public static void clickThumbnailAt(int thumbIndex) {
    clickThumbnailAt(item -> {
      final ThumbnailContainer container = (ThumbnailContainer) item.getParent();
      return container.getChildAt(thumbIndex) == item;
    }, thumbIndex);
  }

  private static void clickThumbnailAt(Function<ThumbnailView, Boolean> matchesSafely, int thumbIndex) {
    onView(new BoundedMatcher<View, ThumbnailView>(ThumbnailView.class) {
      @Override
      protected boolean matchesSafely(ThumbnailView item) {
        return matchesSafely.apply(item);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("thumbnail of " + thumbIndex);
      }
    }).perform(actionWithAssertions(new GeneralClickAction(
        Tap.SINGLE, PerformUtil::calcCenterCoord, Press.FINGER, 0, 0)));
  }

  private static float[] calcCenterCoord(View v) {
    int[] pos = new int[2];
    v.getLocationOnScreen(pos);
    return new float[]{
        pos[0] + v.getWidth() / 2.0f,
        pos[1] + v.getHeight() / 2.0f,
    };
  }

  private PerformUtil() {
    throw new AssertionError();
  }
}
