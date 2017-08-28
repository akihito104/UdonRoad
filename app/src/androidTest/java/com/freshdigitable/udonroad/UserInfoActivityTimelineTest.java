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
import com.freshdigitable.udonroad.util.PerformUtil;

import org.junit.Test;

import twitter4j.Relationship;
import twitter4j.TwitterException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.Mockito.mock;

/**
 * Created by akihit on 2017/07/07.
 */
public class UserInfoActivityTimelineTest extends UserInfoActivityInstTest.Base {
  @Override
  protected int setupTimeline() throws TwitterException {
    final Relationship relationship = mock(Relationship.class);
    return setupUserInfoTimeline(relationship);
  }

  @Test
  public void showStatusDetail() throws Exception {
    AssertionUtil.checkUserInfoActivityTitle("");
    onView(withText("TWEET\n20")).check(matches(isDisplayed()));
    PerformUtil.selectItemViewAt(0);
    PerformUtil.showDetail();
    AssertionUtil.checkUserInfoActivityTitle(R.string.title_detail);
    onView(withId(R.id.userInfo_tabs)).check(matches(not(isDisplayed())));
    onView(withId(R.id.action_heading)).check(doesNotExist());
    Espresso.pressBack();
    AssertionUtil.checkUserInfoActivityTitle("");
    onView(withId(R.id.userInfo_tabs)).check(matches(isDisplayed()));
    onView(withId(R.id.ffab)).check(matches(isCompletelyDisplayed()));
  }

  @Test
  public void jumpAnotherAppFromStatusDetail_and_returnToStatusDetail() throws Exception {
    AssertionUtil.checkUserInfoActivityTitle("");
    onView(withText("TWEET\n20")).check(matches(isDisplayed()));
    PerformUtil.selectItemViewAt(0);
    PerformUtil.showDetail();
    AssertionUtil.checkUserInfoActivityTitle(R.string.title_detail);
    onView(withId(R.id.userInfo_tabs)).check(matches(not(isDisplayed())));
    onView(withId(R.id.action_heading)).check(doesNotExist());

    PerformUtil.launchHomeAndBackToApp(rule.getActivity());
//    AssertionUtil.checkUserInfoActivityTitle(R.string.title_detail);
    onView(withId(R.id.userInfo_tabs)).check(matches(not(isDisplayed())));
    onView(withId(R.id.action_heading)).check(doesNotExist());

    Espresso.pressBack();
    AssertionUtil.checkUserInfoActivityTitle("");
    onView(withId(R.id.userInfo_tabs)).check(matches(isDisplayed()));
    onView(withId(R.id.ffab)).check(matches(isCompletelyDisplayed()));
  }
}
