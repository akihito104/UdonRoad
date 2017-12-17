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
import android.support.test.espresso.intent.rule.IntentsTestRule;

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.input.TweetInputActivity;
import com.freshdigitable.udonroad.util.StorageUtil;
import com.freshdigitable.udonroad.util.TestInjectionUtil;
import com.freshdigitable.udonroad.util.TwitterResponseMock;
import com.freshdigitable.udonroad.util.UserUtil;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.inject.Inject;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.User;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by akihit on 2017/12/17.
 */

public class TweetInputActivityInstTest {
  @Rule
  public IntentsTestRule<TweetInputActivity> rule = new IntentsTestRule<>(TweetInputActivity.class, false, false);

  @Inject
  Twitter twitter;
  @Inject
  AppSettingStore appSettings;
  private User userA;

  @Before
  public void setup() {
    TestInjectionUtil.getComponent().inject(this);
    StorageUtil.initStorage();
    userA = UserUtil.createUserA();
    appSettings.open();
    appSettings.addAuthenticatedUser(userA);
    appSettings.close();
  }

  @Test
  public void launchWithIntent() throws Exception {
    final String expectedText = "text";
    launchWithSendIntent(expectedText);

    onView(withId(R.id.tweetInput_text)).check(matches(withText(expectedText)));
    onView(withId(R.id.tweetInput_count)).check(matches(withText(getRemainCount(expectedText))));

    final Status resStatus = TwitterResponseMock.createStatus(1000L, userA);
    when(twitter.updateStatus(anyString())).thenReturn(resStatus);
    onView(withId(R.id.action_sendTweet)).perform(click());
    verify(twitter).updateStatus(expectedText);
  }

  @Test
  public void inputTextAfterLaunchWithIntent() {
    final String expectedText = "text";
    launchWithSendIntent(expectedText);

    onView(withId(R.id.tweetInput_text)).check(matches(withText(expectedText)));
    onView(withId(R.id.tweetInput_count)).check(matches(withText(getRemainCount(expectedText))));

    final String additionText = " hoge";
    onView(withId(R.id.tweetInput_text))
        .perform(typeText(additionText))
        .check(matches(withText(expectedText + additionText)));
    onView(withId(R.id.tweetInput_count))
        .check(matches(withText(getRemainCount(expectedText + additionText))));
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
