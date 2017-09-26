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

import android.support.test.espresso.Espresso;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.v7.app.AppCompatActivity;

import com.freshdigitable.udonroad.util.AssertionUtil;
import com.freshdigitable.udonroad.util.MatcherUtil;
import com.freshdigitable.udonroad.util.PerformUtil;
import com.freshdigitable.udonroad.util.UserUtil;

import org.junit.Rule;
import org.junit.Test;

import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.TwitterException;
import twitter4j.User;
import twitter4j.UserMentionEntity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.IdlingResourceUtil.getOpenDrawerIdlingResource;
import static com.freshdigitable.udonroad.util.IdlingResourceUtil.runWithIdlingResource;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createRtStatus;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatus;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TweetInputFragmentInstTest tests TweetInputFragment in MainActivity.
 *
 * Created by akihit on 2016/10/06.
 */
public class TweetInputFragmentInstTest extends TimelineInstTestBase {
  @Rule
  public final ActivityTestRule<MainActivity> rule
      = new ActivityTestRule<>(MainActivity.class, false, false);
  private final User userB = UserUtil.builder(2100, "userB").name("user B").build();
  private final User userC = UserUtil.builder(2200, "userC").build();

  @Override
  protected int setupTimeline() throws TwitterException {
    when(twitter.updateStatus(any(StatusUpdate.class))).thenAnswer(invocation -> {
      final StatusUpdate statusUpdate = invocation.getArgument(0);
      final User user = UserUtil.createUserA();
      final Status mockResponse = createStatus(21000L, user);
      when(mockResponse.getText()).thenReturn(statusUpdate.getStatus());
      final UserMentionEntity umeMock = mock(UserMentionEntity.class);
      when(umeMock.getId()).thenReturn(statusUpdate.getInReplyToStatusId());
      when(mockResponse.getUserMentionEntities()).thenReturn(new UserMentionEntity[]{umeMock});
      return mockResponse;
    });
    return setupDefaultTimeline();
  }

  @Test
  public void sendValidInReplyTo() throws Exception {
    MatcherUtil.onOpenDrawerMenu().check(matches(isDisplayed()));
    sendReplyToMe();
  }

  @Test
  public void sendValidInReplyToAndOpenOnceMore_then_clearedTheView() throws Exception {
    sendReplyToMe();

    PerformUtil.clickWriteOnMenu();

    AssertionUtil.checkMainActivityTitle(R.string.title_tweet);
    onView(withId(R.id.tw_replyTo)).check(matches(not(isDisplayed())));
  }

  @Test
  public void openTweetInputForReplyOtherUser_then_inputUserScreenName() throws Exception {
    final Status target = createStatus(30000, userB);
    receiveStatuses(target);

    PerformUtil.selectItemView(target);
    PerformUtil.reply();

    AssertionUtil.checkMainActivityTitle(R.string.title_reply);
    onView(withId(R.id.tw_intext))
        .check(matches(withText("@" + userB.getScreenName() + " ")));
    PerformUtil.clickCancelWriteOnMenu();
    onView(withId(R.id.action_writeTweet)).check(matches(isDisplayed()))
        .check(matches(isEnabled()));
    MatcherUtil.onOpenDrawerMenu().check(matches(isDisplayed()));
  }

  @Test
  public void openTweetInputForReplyOtherUsersRetweet_then_inputUserScreenName() throws Exception {
    final Status retweeted = createStatus(30000, userB);
    final Status target = createRtStatus(retweeted, 31000, true);
    when(target.getUser()).thenReturn(userC);
    receiveStatuses(target);

    PerformUtil.selectItemView(target);
    PerformUtil.reply();

    AssertionUtil.checkMainActivityTitle(R.string.title_reply);
    onView(withId(R.id.tw_intext))
        .check(matches(withText(containsString("@" + userB.getScreenName()))));
    onView(withId(R.id.tw_intext))
        .check(matches(withText(containsString("@" + userC.getScreenName()))));
    onView(withId(R.id.tw_intext))
        .check(matches(withText(not(containsString("@" + getLoginUser().getScreenName())))));
    PerformUtil.clickCancelWriteOnMenu();
    onView(withId(R.id.action_writeTweet)).check(matches(isDisplayed()))
        .check(matches(isEnabled()));
    AssertionUtil.checkMainActivityTitle(R.string.title_home);
    MatcherUtil.onOpenDrawerMenu().check(matches(isDisplayed()));
  }

  @Test
  public void pressBackAfterTweetInputIsAppeared_then_hideTweetInput() {
    PerformUtil.clickWriteOnMenu();
    AssertionUtil.checkMainActivityTitle(R.string.title_tweet);
    Espresso.closeSoftKeyboard();
    pressBack();
    AssertionUtil.checkMainActivityTitle(R.string.title_home);
    onActionWrite().check(matches(isDisplayed()));
    onView(withId(R.id.main_tweet_input_view)).check(matches(not(isDisplayed())));
    onView(withId(R.id.action_sendTweet)).check(doesNotExist());
  }

  @Test
  public void clickSendIcon_then_openTweetInputViewAndShowFab() {
    // open
    PerformUtil.clickWriteOnMenu();
    AssertionUtil.checkMainActivityTitle(R.string.title_tweet);
    onView(withId(R.id.main_tweet_input_view)).check(matches(isDisplayed()));
    onView(withId(R.id.action_sendTweet)).check(matches(isDisplayed()));
    onActionCancel().check(matches(isDisplayed()));
    onActionWrite().check(doesNotExist());

    // close
    PerformUtil.clickCancelWriteOnMenu();
    onView(withId(R.id.action_writeTweet)).check(matches(isDisplayed()))
        .check(matches(isEnabled()));
    MatcherUtil.onOpenDrawerMenu().check(matches(isDisplayed()));
    AssertionUtil.checkMainActivityTitle(R.string.title_home);
    onActionWrite().check(matches(isDisplayed()));
    onView(withId(R.id.main_tweet_input_view)).check(matches(not(isDisplayed())));
    onView(withId(R.id.action_sendTweet)).check(doesNotExist());
  }

  @Test
  public void openTweetInputAndThenOpenDrawer() {
    PerformUtil.clickWriteOnMenu();
    PerformUtil.clickCancelWriteOnMenu();
    onView(withId(R.id.action_writeTweet)).check(matches(isDisplayed()))
        .check(matches(isEnabled()));
    MatcherUtil.onOpenDrawerMenu().perform(click());
    runWithIdlingResource(getOpenDrawerIdlingResource(rule.getActivity()), () ->
        onView(withId(R.id.nav_header_account)).check(matches(isDisplayed())));
  }

  @Test
  public void failedSendTweet_then_actionResumeIsEnabled() throws Exception {
    final String inputText = "typed tweet";
    when(twitter.updateStatus(inputText)).thenThrow(new TwitterException("send error"));
    PerformUtil.clickWriteOnMenu();
    onView(withId(R.id.tw_intext)).perform(typeText(inputText))
        .check(matches(withText(inputText)));
    onView(withId(R.id.action_sendTweet)).perform(click());
    onView(withId(R.id.action_resumeTweet)).check(matches(isDisplayed()))
        .perform(click());
    onView(withId(R.id.tw_intext)).check(matches(isDisplayed()))
        .check(matches(withText(inputText)));
  }

  private void sendReplyToMe() throws Exception {
    final Status replied = findByStatusId(20000);
    PerformUtil.selectItemView(replied);
    PerformUtil.reply();
    AssertionUtil.checkMainActivityTitle(R.string.title_reply);
    onView(withId(R.id.tw_replyTo)).check(matches(isDisplayed()));
    final String inputText = "reply tweet";
    onView(withId(R.id.tw_intext)).perform(typeText(inputText))
        .check(matches(withText(inputText)));
    onView(withId(R.id.action_sendTweet)).perform(click());
    onView(withId(R.id.main_tweet_input_view)).check(matches(not(isDisplayed())));
    onActionWrite().check(matches(isDisplayed()));
    onActionCancel().check(doesNotExist());
    MatcherUtil.onOpenDrawerMenu().check(matches(isDisplayed()));
    AssertionUtil.checkMainActivityTitle(R.string.title_home);
  }

  private static ViewInteraction onActionCancel() {
    return MatcherUtil.onCancelWriteMenu();
  }

  private static ViewInteraction onActionWrite() {
    return onView(withId(R.id.action_writeTweet));
  }

  @Override
  protected ActivityTestRule<? extends AppCompatActivity> getRule() {
    return rule;
  }
}
