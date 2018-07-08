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

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.espresso.matcher.BoundedMatcher;

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.input.TweetInputActivity;
import com.freshdigitable.udonroad.util.StorageUtil;
import com.freshdigitable.udonroad.util.TwitterResponseMock;
import com.freshdigitable.udonroad.util.UserUtil;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.User;
import twitter4j.auth.AccessToken;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by akihit on 2017/12/17.
 */

public class TweetInputActivityInstTest {
  @Rule
  public IntentsTestRule<TweetInputActivity> rule
      = new IntentsTestRule<>(TweetInputActivity.class, false, false);

  Twitter twitter;
  AppSettingStore appSettings;
  private User userA;
  private User userAsub;

  @Before
  public void setup() throws Throwable {
    StorageUtil.initStorage();
    final MockMainApplication app = MockMainApplication.getApp();
    twitter = app.twitterApiModule.twitter;
    appSettings = app.sharedPreferenceModule.appSettingStore;
    app.twitterApiModule.setup();

    userA = UserUtil.createUserA();
    final long userId = userA.getId();
    appSettings.open();
    appSettings.clear();
    appSettings.storeAccessToken(new AccessToken(userId + "-validToken", "validSecret"));
    appSettings.addAuthenticatedUser(userA);

    userAsub = UserUtil.createUserAsub();
    appSettings.storeAccessToken(new AccessToken(userAsub.getId() + "-validToken", "validSecret"));
    appSettings.addAuthenticatedUser(userAsub);
    appSettings.setCurrentUserId(userId);
    appSettings.close();

    when(twitter.showUser(userA.getId())).thenReturn(userA);
    when(twitter.showUser(userAsub.getId())).thenReturn(userAsub);
  }

  @After
  public void tearDown() {
    MockMainApplication.getApp().twitterApiModule.reset();
  }

  @Test
  public void launchWithIntent() throws Exception {
    final Status resStatus = TwitterResponseMock.createStatus(1000L, userA);
    when(twitter.updateStatus(anyString())).thenReturn(resStatus);

    final String expectedText = "text";
    launchWithSendIntent(expectedText);

    onView(withId(R.id.tweetInput_text)).check(matches(withText(expectedText)));
    onView(withId(R.id.tweetInput_count)).check(matches(withText(getRemainCount(expectedText))));

    onView(withId(R.id.action_sendTweet)).perform(click());
    verify(twitter, atLeast(1)).setOAuthAccessToken(getAccessToken(userA));
    verify(twitter).updateStatus(expectedText);
  }

  @NonNull
  private AccessToken getAccessToken(User user) {
    return new AccessToken(user.getId() + "-validToken", "validSecret");
  }

  @Test
  public void inputTextAfterLaunchWithIntent() {
    final String expectedText = "text";
    launchWithSendIntent(expectedText);

    onView(withId(R.id.tweetInput_text)).check(matches(withText(expectedText)));
    onView(withId(R.id.tweetInput_count)).check(matches(withText(getRemainCount(expectedText))));

    final String additionText = " hoge";
    onView(withId(R.id.tweetInput_text)).perform(typeText(additionText))
        .check(matches(withText(expectedText + additionText)));
    onView(withId(R.id.tweetInput_count))
        .check(matches(withText(getRemainCount(expectedText + additionText))));
  }

  @Test
  public void selectSecondAccountAtSpinner() throws Exception {
    final Status resStatus = TwitterResponseMock.createStatus(2000L, userAsub);
    when(twitter.updateStatus(anyString())).thenReturn(resStatus);

    launchWithSendIntent("");
    onView(withId(R.id.tweetInput_account)).perform(click());
    onData(ofUser(userAsub))
        .inRoot(withDecorView(not(is(rule.getActivity().getWindow().getDecorView()))))
        .perform(click());
    onView(withId(R.id.accountSpinner_name)).check(matches(withText(userAsub.getName() + "\n@" + userAsub.getScreenName())));
    final String expectedText = "tweet as A sub";
    onView(withId(R.id.tweetInput_text)).perform(typeText(expectedText));
    onView(withId(R.id.tweetInput_count)).check(matches(withText(getRemainCount(expectedText))));
    onView(withId(R.id.action_sendTweet)).perform(click());
    verify(twitter).setOAuthAccessToken(getAccessToken(userAsub));
    verify(twitter).updateStatus(expectedText);
  }

  @NonNull
  private Matcher<Object> ofUser(User user) {
    return allOf(is(instanceOf(User.class)), new BoundedMatcher<Object, User>(User.class) {
      @Override
      protected boolean matchesSafely(User item) {
        return item.getId() == user.getId();
      }
      @Override
      public void describeTo(Description description) {}
    });
  }

  private void launchWithSendIntent(String expectedText) {
    final Intent intent = new Intent()
        .setAction(Intent.ACTION_SEND)
        .putExtra(Intent.EXTRA_TEXT, expectedText);
    rule.launchActivity(intent);
  }

  private static String getRemainCount(String text) {
    return Integer.toString(140 - text.length());
  }
}
