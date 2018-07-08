/*
 * Copyright (c) 2017. Matsuda, Akihit (akihito104)
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

import com.freshdigitable.udonroad.util.AssertionUtil;
import com.freshdigitable.udonroad.util.MatcherUtil;
import com.freshdigitable.udonroad.util.PerformUtil;

import org.junit.Test;

import twitter4j.Relationship;
import twitter4j.TwitterException;

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
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.Mockito.when;

/**
 * Created by akihit on 2017/09/27.
 */

public class UserInfoTweetInputInstTest extends UserInfoActivityInstTest.Base {
  @Test
  public void showTweetInputView_then_followMenuIconIsHiddenAndCancelMenuIconIsAppeared() {
    AssertionUtil.checkUserInfoActivityTitle("");
    PerformUtil.selectItemViewAt(0);
    PerformUtil.reply();
    // verify
    MatcherUtil.onCancelWriteMenu().check(matches(isDisplayed()));
    AssertionUtil.checkUserInfoActivityTitle(R.string.title_reply);
    onView(withId(R.id.tw_intext)).check(matches(withText("")));
    onView(withId(R.id.action_heading)).check(matches(isDisplayed()));
    onView(withId(R.id.action_group_user)).check(doesNotExist());
    onView(withId(R.id.action_sendTweet))
        .check(matches(isDisplayed()))
        .check(matches(not(isEnabled())));

    // tear down
    PerformUtil.clickCancelWriteOnMenu();
    AssertionUtil.checkUserInfoActivityTitle("");
    MatcherUtil.onCancelWriteMenu().check(doesNotExist());
    onView(withId(R.id.action_sendTweet)).check(doesNotExist());
  }

  @Test
  public void closeTweetInputView_then_followMenuIconIsAppearAndCancelMenuIconIsHidden() {
    AssertionUtil.checkUserInfoActivityTitle("");
    PerformUtil.selectItemViewAt(0);
    PerformUtil.reply();
    AssertionUtil.checkUserInfoActivityTitle(R.string.title_reply);
    MatcherUtil.onCancelWriteMenu().check(matches(isDisplayed()));
    onView(withId(R.id.action_sendTweet)).check(matches(isDisplayed()))
        .check(matches(not(isEnabled())));
    onView(withId(R.id.tw_intext)).check(matches(withText("")));
    PerformUtil.clickCancelWriteOnMenu();
    // verify
    AssertionUtil.checkUserInfoActivityTitle("");
    MatcherUtil.onCancelWriteMenu().check(doesNotExist());
    onView(withId(R.id.action_group_user)).check(matches(isDisplayed()));
    onView(withId(R.id.action_heading)).check(matches(isDisplayed()));
    onView(withId(R.id.action_writeTweet)).check(doesNotExist());
    onView(withId(R.id.action_sendTweet)).check(doesNotExist());
  }

  @Test
  public void quoteDoesNotOpenWhenReplyAlreadyOpened() {
    PerformUtil.selectItemViewAt(0);
    PerformUtil.reply();
    AssertionUtil.checkUserInfoActivityTitle(R.string.title_reply);
    onView(withId(R.id.action_sendTweet)).check(matches(isDisplayed()));
    onView(withId(R.id.tw_replyTo)).check(matches(isDisplayed()));

    Espresso.closeSoftKeyboard();
    PerformUtil.selectItemViewAt(1);
    PerformUtil.quote();
    AssertionUtil.checkUserInfoActivityTitle(R.string.title_reply);
    onView(withId(R.id.tw_quote)).check(matches(not(isDisplayed())));

    PerformUtil.clickCancelWriteOnMenu();
    AssertionUtil.checkUserInfoActivityTitle("");
  }

  @Test
  public void failedSendTweet_then_actionResumeIsEnabled() throws Exception {
    final String inputText = "typed tweet";
    when(twitter.updateStatus(inputText)).thenThrow(new TwitterException("send error"));
    PerformUtil.selectItemViewAt(0);
    PerformUtil.reply();
    onView(withId(R.id.tw_intext)).perform(typeText(inputText))
        .check(matches(withText(inputText)));
    AssertionUtil.checkRemainCount(inputText);

    onView(withId(R.id.action_sendTweet)).perform(click());
    onView(withId(R.id.tw_intext)).check(matches(not(isDisplayed())));
    onView(withId(R.id.userInfo_user_info_view)).check(matches(isDisplayed()));

    onView(withId(R.id.action_resumeTweet)).check(matches(isDisplayed()))
        .perform(click());
    onView(withId(R.id.tw_intext)).check(matches(isDisplayed()))
        .check(matches(withText(inputText)));
    onView(withId(R.id.userInfo_user_info_view)).check(matches(not(isDisplayed())));
    MatcherUtil.onCancelWriteMenu().check(matches(isDisplayed())).perform(click());
  }

  @Test
  public void pressBackAfterTweetInputIsAppeared_then_hideTweetInput() {
    PerformUtil.selectItemViewAt(0);
    PerformUtil.reply();
    AssertionUtil.checkUserInfoActivityTitle(R.string.title_reply);
    Espresso.closeSoftKeyboard();
    pressBack();

    AssertionUtil.checkUserInfoActivityTitle("");
    onView(withId(R.id.main_tweet_input_view)).check(doesNotExist());
    onView(withId(R.id.action_sendTweet)).check(doesNotExist());
  }

  @Override
  protected int setupTimeline() throws TwitterException {
    final Relationship relationship = getRelationship();
    when(relationship.isSourceFollowingTarget()).thenReturn(false);
    when(relationship.isSourceBlockingTarget()).thenReturn(false);
    when(relationship.isSourceMutingTarget()).thenReturn(false);
    when(relationship.isSourceWantRetweets()).thenReturn(false);
    return setupUserInfoTimeline(relationship);
  }
}
