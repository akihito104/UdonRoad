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
import com.freshdigitable.udonroad.util.TwitterResponseMock;

import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;

import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.mockito.Mockito.when;

/**
 * Created by akihit on 2016/10/19.
 */
public class StatusDetailInstTest extends TimelineInstTestBase {
  @Rule
  public ActivityTestRule<MainActivity> rule
      = new ActivityTestRule<>(MainActivity.class, false, false);
  private Status target;
  private Status simple;
  private Status quoted;

  @Test
  public void showStatusDetailForSimpleStatus() {
    PerformUtil.selectItemViewAt(1);
    PerformUtil.showDetail();
    onView(withId(R.id.timeline)).check(matches(withEffectiveVisibility(GONE)));
    onView(withId(R.id.d_tweet)).check(matches(withText(simple.getText())));
  }

  @Test
  public void showStatusDetailForSimpleStatus_then_clickUserIcon() {
    PerformUtil.selectItemViewAt(1);
    PerformUtil.showDetail();
    onView(withId(R.id.timeline)).check(matches(withEffectiveVisibility(GONE)));
    onView(withId(R.id.d_tweet)).check(matches(withText(simple.getText())));
    onView(withId(R.id.d_icon)).perform(click());
    onView(withId(R.id.user_name)).check(matches(withText(getLoginUser().getName())));
    onView(withId(R.id.user_screen_name)).check(matches(withText("@" + getLoginUser().getScreenName())));
  }

  @Test
  public void showStatusDetailForQuotingStatus() {
    PerformUtil.selectItemViewAt(0);
    PerformUtil.showDetail();
    onView(withId(R.id.timeline)).check(matches(withEffectiveVisibility(GONE)));
    onView(withId(R.id.d_tweet)).check(matches(withText(target.getText())));
  }

  @Test
  public void showStatusDetailForQuotedStatus() {
    PerformUtil.selectQuotedItemView(quoted);
    PerformUtil.showDetail();
    onView(withId(R.id.timeline)).check(matches(withEffectiveVisibility(GONE)));
    onView(withId(R.id.d_tweet)).check(matches(withText(quoted.getText())));
  }

  @Test
  public void showStatusDetailForQuotedStatus_then_clickUserIcon() {
    PerformUtil.selectQuotedItemView(quoted);
    PerformUtil.showDetail();
    onView(withId(R.id.timeline)).check(matches(withEffectiveVisibility(GONE)));
    onView(withId(R.id.d_tweet)).check(matches(withText(quoted.getText())));
    onView(withId(R.id.d_icon)).perform(click());
    onView(withId(R.id.user_name)).check(matches(withText(getLoginUser().getName())));
    onView(withId(R.id.user_screen_name)).check(matches(withText("@" + getLoginUser().getScreenName())));
  }

  @Override
  protected int setupTimeline() throws TwitterException {
    quoted = TwitterResponseMock.createStatus(10000, getLoginUser());
    target = TwitterResponseMock.createStatus(20000, getLoginUser());
    simple = TwitterResponseMock.createStatus(5000, getLoginUser());
    final long quotedId = quoted.getId();
    when(target.getQuotedStatusId()).thenReturn(quotedId);
    when(target.getQuotedStatus()).thenReturn(quoted);

    final ResponseList<Status> responseList
        = TwitterResponseMock.createResponseList(Arrays.asList(target, simple));
    super.responseList = responseList;
    when(twitter.getHomeTimeline()).thenReturn(responseList);
    return responseList.size();
  }

  @Override
  protected ActivityTestRule<? extends AppCompatActivity> getRule() {
    return rule;
  }
}
