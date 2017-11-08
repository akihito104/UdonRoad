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

import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.lifecycle.Stage;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.freshdigitable.udonroad.util.AssertionUtil;
import com.freshdigitable.udonroad.util.PerformUtil;
import com.freshdigitable.udonroad.util.TwitterResponseMock;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;

import twitter4j.Paging;
import twitter4j.Relationship;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.AssertionUtil.anywayNotVisible;
import static com.freshdigitable.udonroad.util.IdlingResourceUtil.getActivityStageIdlingResource;
import static com.freshdigitable.udonroad.util.IdlingResourceUtil.runWithIdlingResource;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.getReactionContainerMatcher;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofQuotedStatusView;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createResponseList;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createRtStatus;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatus;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatusHasImage;
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by akihit on 2016/10/19.
 */
public class StatusDetailInstTest extends TimelineInstTestBase {
  @Rule
  public final ActivityTestRule<MainActivity> rule
      = new ActivityTestRule<>(MainActivity.class, false, false);
  private Status target;
  private Status simple;
  private Status quoted;
  private Status hasImage;

  @Test
  public void showStatusDetailForSimpleStatus() {
    AssertionUtil.checkMainActivityTitle(R.string.title_home);
    PerformUtil.selectItemView(simple);
    PerformUtil.showDetail();
    AssertionUtil.checkMainActivityTitle(R.string.title_detail);
    onView(withId(R.id.timeline)).check(doesNotExist());
    onView(withId(R.id.d_tweet)).check(matches(withText(simple.getText())));
  }

  @Test
  public void retweetSimpleStatusOnStatusDetail() throws Exception {
    final Status rtStatus = createRtStatus(simple, 21000, 4, 6, true);
    when(twitter.retweetStatus(simple.getId())).thenReturn(rtStatus);

    PerformUtil.selectItemView(simple);
    PerformUtil.showDetail();
    AssertionUtil.checkMainActivityTitle(R.string.title_detail);
    onView(withId(R.id.timeline)).check(doesNotExist());
    onView(withId(R.id.d_tweet)).check(matches(withText(simple.getText())));
    onView(withId(R.id.iffabMenu_main_rt)).perform(click());
    onView(withId(R.id.iffabMenu_main_rt)).check(matches(withDrawableState(android.R.attr.state_checked)));
    checkRTCount(4);
    checkFavCount(6);
  }

  @Test
  public void favoSimpleStatusOnStatusDetail() throws Exception {
    setupCreateFavorite(3, 9);

    PerformUtil.selectItemView(simple);
    PerformUtil.showDetail();
    AssertionUtil.checkMainActivityTitle(R.string.title_detail);
    onView(withId(R.id.timeline)).check(doesNotExist());
    onView(withId(R.id.d_tweet)).check(matches(withText(simple.getText())));
    onView(withId(R.id.iffabMenu_main_fav)).perform(click());
    onView(withId(R.id.iffabMenu_main_fav)).check(matches(withDrawableState(android.R.attr.state_checked)));
    checkRTCount(3);
    checkFavCount(9);
  }

  @Test
  public void showStatusDetailForSimpleStatus_then_clickUserIcon() {
    PerformUtil.selectItemView(simple);
    PerformUtil.showDetail();
    AssertionUtil.checkMainActivityTitle(R.string.title_detail);
    onView(withId(R.id.timeline)).check(doesNotExist());
    onView(withId(R.id.d_tweet)).check(matches(withText(simple.getText())));
    onView(withId(R.id.d_icon)).perform(click());
    onView(withId(R.id.user_name)).check(matches(withText(getLoginUser().getName())));
    onView(withId(R.id.user_screen_name)).check(matches(screenNameMatcher));
    Espresso.pressBack();
    runWithIdlingResource(
        getActivityStageIdlingResource("back to main", MainActivity.class, Stage.RESUMED), () -> {
          AssertionUtil.checkMainActivityTitle(R.string.title_detail);
          onView(withId(R.id.iffabMenu_main_detail)).check(anywayNotVisible());
        });
  }

  @Test
  public void showStatusDetailForQuotingStatus() {
    PerformUtil.selectItemView(target);
    PerformUtil.showDetail();
    AssertionUtil.checkMainActivityTitle(R.string.title_detail);
    onView(withId(R.id.timeline)).check(doesNotExist());
    onView(withId(R.id.d_tweet)).check(matches(withText(target.getText())));
  }

  @Test
  public void showStatusDetailForQuotedStatus() {
    PerformUtil.selectQuotedItemView(quoted);
    PerformUtil.showDetail();
    AssertionUtil.checkMainActivityTitle(R.string.title_detail);
    onView(withId(R.id.timeline)).check(doesNotExist());
    onView(withId(R.id.d_tweet)).check(matches(withText(quoted.getText())));
  }

  @Test
  public void showStatusDetailForQuotedStatus_then_clickUserIcon() {
    PerformUtil.selectQuotedItemView(quoted);
    PerformUtil.showDetail();
    AssertionUtil.checkMainActivityTitle(R.string.title_detail);
    onView(withId(R.id.timeline)).check(doesNotExist());
    onView(withId(R.id.d_tweet)).check(matches(withText(quoted.getText())));
    onView(withId(R.id.d_icon)).perform(click());
    onView(withId(R.id.user_name)).check(matches(withText(getLoginUser().getName())));
    onView(withId(R.id.user_screen_name)).check(matches(screenNameMatcher));
    Espresso.pressBack();
  }

  @Test
  public void favQuotedTweet() throws Exception {
    setupCreateFavorite(0, 1);
    PerformUtil.selectQuotedItemView(quoted);
    PerformUtil.favo();
    Espresso.pressBack();
    AssertionUtil.checkFavCountDoesNotExist(target);
    AssertionUtil.checkFavCountForQuoted(quoted, 1);
  }

  @Test
  public void deleteQuotedStatus() {
    receiveDeletionNotice(quoted);
    onView(ofQuotedStatusView(quoted)).check(matches(not(isDisplayed())));
  }

  @Test
  public void restoreSimpleTweet() {
    PerformUtil.selectItemView(hasImage);
    PerformUtil.showDetail();
    AssertionUtil.checkMainActivityTitle(R.string.title_detail);
    PerformUtil.clickThumbnailAt(0);
    Espresso.pressBack();
    AssertionUtil.checkMainActivityTitle(R.string.title_detail);
    Espresso.pressBack();
    AssertionUtil.checkMainActivityTitle(R.string.title_home);
  }

  @Test
  public void restoreTweetHasReaction() throws Exception {
    setupRetweetStatus(23000, 4, 10);
    setupCreateFavorite(4, 12);

    PerformUtil.selectItemView(hasImage);
    PerformUtil.showDetail();
    AssertionUtil.checkMainActivityTitle(R.string.title_detail);
    onView(withId(R.id.iffabMenu_main_rt)).perform(click());
    onView(withId(R.id.iffabMenu_main_fav)).perform(click());
    onView(withId(R.id.iffabMenu_main_rt)).check(matches(withDrawableState(android.R.attr.state_checked)));
    onView(withId(R.id.iffabMenu_main_fav)).check(matches(withDrawableState(android.R.attr.state_checked)));

    PerformUtil.clickThumbnailAt(0);
    Espresso.pressBack();
    AssertionUtil.checkMainActivityTitle(R.string.title_detail);
    onView(withId(R.id.iffabMenu_main_rt)).check(matches(withDrawableState(android.R.attr.state_checked)));
    onView(withId(R.id.iffabMenu_main_fav)).check(matches(withDrawableState(android.R.attr.state_checked)));

    Espresso.pressBack();
    AssertionUtil.checkMainActivityTitle(R.string.title_home);
  }

  @Override
  protected int setupTimeline() throws TwitterException {
    quoted = createStatus(10000, getLoginUser());
    target = createStatus(20000, getLoginUser());
    simple = createStatus(5000, getLoginUser());
    hasImage = createStatusHasImage(3000, getLoginUser());
    final long quotedId = quoted.getId();
    when(target.getQuotedStatusId()).thenReturn(quotedId);
    when(target.getQuotedStatus()).thenReturn(quoted);

    final ResponseList<Status> responseList
        = TwitterResponseMock.createResponseList(Arrays.asList(target, simple, quoted, hasImage));
    super.responseList = responseList;
    when(twitter.getHomeTimeline()).thenReturn(responseList);
    when(twitter.getHomeTimeline(any(Paging.class))).thenReturn(createResponseList());
    final Relationship relationship = mock(Relationship.class);
    when(relationship.isSourceFollowingTarget()).thenReturn(true);
    when(twitter.showFriendship(anyLong(), anyLong())).thenReturn(relationship);
    return responseList.size();
  }

  private static void checkRTCount(int expectedCount) {
    final int expectedVisibility = expectedCount > 0 ? View.VISIBLE : View.INVISIBLE;
    AssertionUtil.checkReactionIcon(withId(R.id.d_reaction_container), getRTIcon(), expectedVisibility, expectedCount);
  }

  private static void checkFavCount(int expectedCount) {
    final int expectedVisibility = expectedCount > 0 ? View.VISIBLE : View.INVISIBLE;
    AssertionUtil.checkReactionIcon(withId(R.id.d_reaction_container), getFavIcon(), expectedVisibility, expectedCount);
  }

  private static Matcher<View> getRTIcon() {
    return getReactionContainerMatcher(
        R.id.d_reaction_container, R.drawable.ic_retweet, IconAttachedTextView.class);
  }

  private static Matcher<View> getFavIcon() {
    return getReactionContainerMatcher(
        R.id.d_reaction_container, R.drawable.ic_like, IconAttachedTextView.class);
  }

  @NonNull
  private Matcher<View> withDrawableState(@AttrRes int expectedState) {
    return new BoundedMatcher<View, ImageView>(ImageView.class) {
      @Override
      protected boolean matchesSafely(ImageView item) {
        final int[] states = item.getDrawableState();
        for (int s : states) {
          if (s == expectedState) {
            return true;
          }
        }
        return false;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("drawable state: " + expectedState);
      }
    };
  }

  @Override
  protected ActivityTestRule<? extends AppCompatActivity> getRule() {
    return rule;
  }
}
