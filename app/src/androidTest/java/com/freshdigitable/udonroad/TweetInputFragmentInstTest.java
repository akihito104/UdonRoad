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

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.StringRes;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.view.View;

import com.freshdigitable.udonroad.util.AssertionUtil;
import com.freshdigitable.udonroad.util.MatcherUtil;
import com.freshdigitable.udonroad.util.PerformUtil;
import com.freshdigitable.udonroad.util.UserUtil;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import twitter4j.Paging;
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
import static com.freshdigitable.udonroad.util.AssertionUtil.checkRemainCount;
import static com.freshdigitable.udonroad.util.IdlingResourceUtil.getOpenDrawerIdlingResource;
import static com.freshdigitable.udonroad.util.IdlingResourceUtil.getSimpleIdlingResource;
import static com.freshdigitable.udonroad.util.IdlingResourceUtil.runWithIdlingResource;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createResponseList;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createRtStatus;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatus;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatusHasImage;
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
@RunWith(Enclosed.class)
public class TweetInputFragmentInstTest {

  public static class WhenDefault extends Base {
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

      checkDefaultTweetInputFragment(false);
    }

    @Test
    public void pressBackAfterTweetInputIsAppeared_then_hideTweetInput() {
      PerformUtil.clickWriteOnMenu();
      checkDefaultTweetInputFragment(false);
      Espresso.closeSoftKeyboard();
      pressBack();
      AssertionUtil.checkMainActivityTitle(R.string.title_home);
      MatcherUtil.onActionWrite().check(matches(isDisplayed()));
      MatcherUtil.onOpenDrawerMenu().check(matches(isDisplayed()));
      onView(withId(R.id.main_tweet_input_view)).check(matches(not(isDisplayed())));
      onView(withId(R.id.action_sendTweet)).check(doesNotExist());
    }

    @Test
    public void clickSendIcon_then_openTweetInputViewAndShowFab() {
      // open
      PerformUtil.clickWriteOnMenu();
      checkDefaultTweetInputFragment(false);
      onActionCancel().check(matches(isDisplayed()));
      MatcherUtil.onActionWrite().check(doesNotExist());

      // close
      PerformUtil.clickCancelWriteOnMenu();
      onView(withId(R.id.action_writeTweet)).check(matches(isDisplayed()))
          .check(matches(isEnabled()));
      MatcherUtil.onOpenDrawerMenu().check(matches(isDisplayed()));
      AssertionUtil.checkMainActivityTitle(R.string.title_home);
      MatcherUtil.onActionWrite().check(matches(isDisplayed()));
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
      checkRemainCount(inputText);

      onView(withId(R.id.action_sendTweet)).perform(click());
      onView(withId(R.id.action_resumeTweet)).check(matches(isDisplayed())).perform(click());
      onView(withId(R.id.tw_intext)).check(matches(isDisplayed()))
          .check(matches(withText(inputText)));
    }

    @Test
    public void openTweetInputForQuote_then_qtMarkIsShown() {
      PerformUtil.selectItemViewAt(0);
      PerformUtil.quote();
      checkRemainCount("", true);
      onView(withId(R.id.action_sendTweet)).check(matches(isDisplayed()));
      AssertionUtil.checkMainActivityTitle(R.string.title_comment);
      onView(withId(R.id.tw_quote)).check(matches(isDisplayed()));
    }

    @Test
    public void replyButDoesNotOpenWhenAlreadyOpened() {
      PerformUtil.clickWriteOnMenu();
      checkDefaultTweetInputFragment(false);

      PerformUtil.selectItemViewAt(0);
      PerformUtil.reply();
      checkDefaultTweetInputFragment(false);

      PerformUtil.clickCancelWriteOnMenu();
      AssertionUtil.checkMainActivityTitle(R.string.title_home);
    }

    @Test
    public void quoteDoesNotOpenWhenAlreadyOpened() {
      PerformUtil.clickWriteOnMenu();
      checkDefaultTweetInputFragment(false);

      PerformUtil.selectItemViewAt(0);
      PerformUtil.quote();
      checkRemainCount("");
      checkDefaultTweetInputFragment(false);

      PerformUtil.clickCancelWriteOnMenu();
      AssertionUtil.checkMainActivityTitle(R.string.title_home);
    }

    @Test
    public void quoteDoesNotOpenWhenReplyAlreadyOpened() {
      PerformUtil.selectItemViewAt(0);
      PerformUtil.reply();
      checkReplyTweetInputFragment(false);

      PerformUtil.selectItemViewAt(1);
      PerformUtil.quote();
      checkRemainCount("");
      AssertionUtil.checkMainActivityTitle(R.string.title_reply);
      onView(withId(R.id.tw_quote)).check(matches(not(isDisplayed())));

      PerformUtil.clickCancelWriteOnMenu();
      AssertionUtil.checkMainActivityTitle(R.string.title_home);
    }
  }

  public static class WhenSpecificTweetUse extends Base {
    private final User userB = UserUtil.builder(2100, "userB").name("user B").build();
    private final User userC = UserUtil.builder(2200, "userC").build();

    @Test
    public void openTweetInputForReplyOtherUser_then_inputUserScreenName() throws Exception {
      PerformUtil.selectItemView(normalTweet);
      PerformUtil.reply();

      checkReplyTweetInputFragment(true);
      final String inputText = "@" + userB.getScreenName() + " ";
      onView(withId(R.id.tw_intext)).check(matches(withText(inputText)));
      checkRemainCount(inputText);
      PerformUtil.clickCancelWriteOnMenu();
      onView(withId(R.id.action_writeTweet)).check(matches(isDisplayed()))
          .check(matches(isEnabled()));
      MatcherUtil.onOpenDrawerMenu().check(matches(isDisplayed()));
    }

    @Test
    public void openTweetInputForReplyOtherUsersRetweet_then_inputUserScreenName() throws Exception {
      PerformUtil.selectItemView(rtTweet);
      PerformUtil.reply();

      checkReplyTweetInputFragment(true);
      final String userBName = "@" + userB.getScreenName();
      onView(withId(R.id.tw_intext)).check(matches(withText(containsString(userBName))));
      final String userCName = "@" + userC.getScreenName();
      onView(withId(R.id.tw_intext)).check(matches(withText(containsString(userCName))));
      checkRemainCount(userBName + " " + userCName + " ");
      onView(withId(R.id.tw_intext))
          .check(matches(withText(not(containsString("@" + getLoginUser().getScreenName())))));
      PerformUtil.clickCancelWriteOnMenu();
      onView(withId(R.id.action_writeTweet)).check(matches(isDisplayed()))
          .check(matches(isEnabled()));
      AssertionUtil.checkMainActivityTitle(R.string.title_home);
      MatcherUtil.onOpenDrawerMenu().check(matches(isDisplayed()));
    }

    @Test
    public void restoreDefaultState() throws Exception {
      PerformUtil.clickWriteOnMenu();
      checkDefaultTweetInputFragment(false);
      final String inputText = "this is default tweet.";
      onView(withId(R.id.tw_intext)).perform(typeText(inputText));
      checkRemainCount(inputText);

      showMediaView();
      Espresso.pressBack();
      checkDefaultTweetInputFragment(true);
      onView(withId(R.id.tw_intext)).check(matches(withText(inputText)));
      checkRemainCount(inputText);

      PerformUtil.clickCancelWriteOnMenu();
      AssertionUtil.checkMainActivityTitle(R.string.title_home);
      onView(withId(R.id.action_writeTweet)).check(matches(isDisplayed()));
    }

    @Test
    public void restoreReplyState() throws Exception {
      PerformUtil.selectItemView(normalTweet);
      PerformUtil.reply();
      checkReplyTweetInputFragment(true);

      showMediaView();
      Espresso.pressBack();
      checkReplyTweetInputFragment(true);

      PerformUtil.clickCancelWriteOnMenu();
      AssertionUtil.checkMainActivityTitle(R.string.title_home);
      onView(withId(R.id.action_writeTweet)).check(matches(isDisplayed()));
    }

    @Test
    public void restoreQuoteState() throws Exception {
      PerformUtil.selectItemView(normalTweet);
      PerformUtil.quote();
      checkQuoteTweetInputFragment();
      checkRemainCount("", true);

      showMediaView();
      Espresso.pressBack();
      checkQuoteTweetInputFragment();

      PerformUtil.clickCancelWriteOnMenu();
      AssertionUtil.checkMainActivityTitle(R.string.title_home);
      onView(withId(R.id.action_writeTweet)).check(matches(isDisplayed()));
    }

    private void showMediaView() {
      Espresso.closeSoftKeyboard();
      PerformUtil.pullDownTimeline();
      PerformUtil.clickThumbnailAt(hasImage, 0);
      onView(withId(R.id.media_pager)).check(matches(isDisplayed()));
    }

    private final Status normalTweet = createStatus(30000, userB);
    private final Status rtTweet = createRtStatus(normalTweet, 31000, true);
    private final Status hasImage = createStatusHasImage(32000, userB);

    @Override
    protected void setupConfig(User loginUser) throws Exception {
      super.setupConfig(loginUser);
      final Context context = InstrumentationRegistry.getTargetContext();
      final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
      final String key = context.getString(R.string.settings_key_sensitive);
      sp.edit().putBoolean(key, true).apply();
    }

    @Override
    public void tearDown() throws Exception {
      final Context context = InstrumentationRegistry.getTargetContext();
      final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
      final String key = context.getString(R.string.settings_key_sensitive);
      sp.edit().putBoolean(key, false).apply();
      super.tearDown();
    }

    @Override
    protected int setupTimeline() throws TwitterException {
      when(rtTweet.getUser()).thenReturn(userC);

      final List<Status> statuses = Arrays.asList(normalTweet, rtTweet, hasImage);
      super.responseList = createResponseList(statuses);
      when(twitter.getHomeTimeline()).thenReturn(responseList);
      when(twitter.getHomeTimeline(any(Paging.class))).thenReturn(createResponseList());

      return statuses.size();
    }
  }

  static abstract class Base extends TimelineInstTestBase {
    @Rule
    public final ActivityTestRule<MainActivity> rule
        = new ActivityTestRule<>(MainActivity.class, false, false);

    void sendReplyToMe() throws Exception {
      final Status replied = findByStatusId(20000);
      PerformUtil.selectItemView(replied);
      PerformUtil.reply();
      checkReplyTweetInputFragment(false);
      final String inputText = "reply tweet";
      onView(withId(R.id.tw_intext)).perform(typeText(inputText))
          .check(matches(withText(inputText)));

      onView(withId(R.id.action_sendTweet)).perform(click());
      waitForSending(() ->
          onView(withId(R.id.main_tweet_input_view)).check(matches(not(isDisplayed()))));
      MatcherUtil.onActionWrite().check(matches(isDisplayed()));
      onActionCancel().check(doesNotExist());
      MatcherUtil.onOpenDrawerMenu().check(matches(isDisplayed()));
      AssertionUtil.checkMainActivityTitle(R.string.title_home);
    }

    private void waitForSending(Runnable task) {
      runWithIdlingResource(getSimpleIdlingResource("end sending", () -> {
        final View resumeMenu = rule.getActivity().findViewById(R.id.action_sendTweet);
        return resumeMenu == null || resumeMenu.getVisibility() != View.VISIBLE;
      }), task);
    }

    @Override
    protected ActivityTestRule<? extends AppCompatActivity> getRule() {
      return rule;
    }
  }

  private static void checkDefaultTweetInputFragment(boolean sendable) {
    checkTweetInputFragment(R.string.title_tweet, sendable, false, false);
  }

  private static void checkReplyTweetInputFragment(boolean enabledSend) {
    checkTweetInputFragment(R.string.title_reply, enabledSend, true, false);
  }

  private static void checkQuoteTweetInputFragment() {
    checkTweetInputFragment(R.string.title_comment, false, false, true);
  }

  private static void checkTweetInputFragment(@StringRes int titleRes, boolean sendable, boolean shownReplyTo, boolean shownQuote) {
    AssertionUtil.checkMainActivityTitle(titleRes);
    onView(withId(R.id.main_tweet_input_view)).check(matches(isDisplayed()));
    onView(withId(R.id.action_sendTweet)).check(matches(isDisplayed()))
        .check(matches(sendable ? isEnabled() : not(isEnabled())));
    onView(withId(R.id.tw_replyTo)).check(matches(shownReplyTo ? isDisplayed() : not(isDisplayed())));
    onView(withId(R.id.tw_quote)).check(matches(shownQuote ? isDisplayed() : not(isDisplayed())));
  }

  private static ViewInteraction onActionCancel() {
    return MatcherUtil.onCancelWriteMenu();
  }
}
