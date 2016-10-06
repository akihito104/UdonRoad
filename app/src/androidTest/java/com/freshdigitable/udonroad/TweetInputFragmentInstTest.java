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

import com.freshdigitable.udonroad.util.UserUtil;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rx.Observable;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.User;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusView;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createText;
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TweetInputFragmentInstTest tests TweetInputFragment in MainActivity.
 *
 * Created by akihit on 2016/10/06.
 */
public class TweetInputFragmentInstTest extends MainActivityInstTestBase {
  @Rule
  public ActivityTestRule<MainActivity> rule
      = new ActivityTestRule<>(MainActivity.class, false, false);

  @Test
  public void sendValidInReplyTo() {
    when(twitterApi.updateStatus(any(StatusUpdate.class))).thenAnswer(new Answer<Observable<Status>>() {
      @Override
      public Observable<Status> answer(InvocationOnMock invocation) throws Throwable {
        final StatusUpdate text = invocation.getArgumentAt(0, StatusUpdate.class);
        final Status mockResponse = mock(Status.class);
        when(mockResponse.getId()).thenReturn(21000L);
        when(mockResponse.getText()).thenReturn(text.getStatus());
        final User user = UserUtil.create();
        when(mockResponse.getUser()).thenReturn(user);
        return Observable.just(mockResponse);
      }
    });

    onView(ofStatusView(withText(createText(20)))).perform(click());
    onView(withId(R.id.iffab_ffab)).perform(swipeDown());
    onView(withId(R.id.main_send_tweet)).check(matches(isEnabled()));
    onView(withId(R.id.tw_intext)).perform(typeText("reply tweet"))
        .check(matches(withText("@akihito104 reply tweet")));
    onView(withId(R.id.main_send_tweet)).perform(click());
    onView(withId(R.id.main_tweet_input_view)).check(matches(not(isDisplayed())));
  }

  @Override
  protected ActivityTestRule<? extends AppCompatActivity> getRule() {
    return rule;
  }
}
