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

import android.support.test.rule.ActivityTestRule;
import android.support.v7.app.AppCompatActivity;

import com.freshdigitable.udonroad.util.PerformUtil;
import com.freshdigitable.udonroad.util.UserUtil;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.TwitterException;
import twitter4j.User;
import twitter4j.UserMentionEntity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatus;
import static org.hamcrest.Matchers.not;
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
  public ActivityTestRule<MainActivity> rule
      = new ActivityTestRule<>(MainActivity.class, false, false);

  @Override
  protected int setupTimeline() throws TwitterException {
    return setupDefaultTimeline();
  }

  @Test
  public void sendValidInReplyTo() throws Exception {
    when(twitter.updateStatus(any(StatusUpdate.class))).thenAnswer(new Answer<Status>() {
      @Override
      public Status answer(InvocationOnMock invocation) throws Throwable {
        final StatusUpdate statusUpdate = invocation.getArgumentAt(0, StatusUpdate.class);
        final User user = UserUtil.create();
        final Status mockResponse = createStatus(21000L, user);
        when(mockResponse.getText()).thenReturn(statusUpdate.getStatus());
        final UserMentionEntity umeMock = mock(UserMentionEntity.class);
        when(umeMock.getId()).thenReturn(statusUpdate.getInReplyToStatusId());
        when(mockResponse.getUserMentionEntities()).thenReturn(new UserMentionEntity[]{umeMock});
        return mockResponse;
      }
    });

    final Status replied = findByStatusId(20000);
    PerformUtil.selectItemView(replied);
    PerformUtil.reply();
    onView(withId(R.id.main_send_tweet)).check(matches(isEnabled()));
    onView(withId(R.id.tw_intext)).perform(typeText("reply tweet"))
        .check(matches(withText("@akihito104 reply tweet")));
    onView(withId(R.id.main_send_tweet)).perform(click());
    onView(withId(R.id.main_tweet_input_view)).check(matches(not(isDisplayed())));
    onView(withId(R.id.action_write)).check(matches(isDisplayed()));
    onView(withId(R.id.action_cancel)).check(doesNotExist());
  }

  @Override
  protected ActivityTestRule<? extends AppCompatActivity> getRule() {
    return rule;
  }
}
