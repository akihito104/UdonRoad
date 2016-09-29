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

import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;

import com.freshdigitable.udonroad.util.UserUtil;

import org.junit.Rule;
import org.junit.Test;

import twitter4j.User;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusView;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createText;

/**
 * UserInfoActivityInstTest tests UserInfoActivity in device.
 *
 * Created by akihit on 2016/08/11.
 */
public class UserInfoActivityInstTest extends MainActivityInstTestBase {
  @Rule
  public ActivityTestRule<UserInfoActivity> rule
      = new ActivityTestRule<>(UserInfoActivity.class, false, false);

  @Override
  protected void verifyAfterLaunch() {
    onView(withId(R.id.user_screen_name)).check(matches(withText("@akihito104")));
    onView(withId(R.id.userInfo_following)).check(matches(isDisplayed()));
    onView(withId(R.id.userInfo_heading)).check(matches(isDisplayed()));
  }

  @Test
  public void showTweetInputView_then_followMenuIconIsHiddenAndCancelMenuIconIsAppeared() {
    onView(ofStatusView(withText(createText(20)))).perform(click());
    onView(withId(R.id.iffab_ffab)).perform(swipeDown());
    // verify
    onView(withId(R.id.userInfo_heading)).check(matches(isDisplayed()));
    onView(withId(R.id.userInfo_reply_close)).check(matches(isDisplayed()));
  }

  @Test
  public void closeTweetInputView_then_followMenuIconIsAppearAndCancelMenuIconIsHidden() {
    onView(ofStatusView(withText(createText(20)))).perform(click());
    onView(withId(R.id.iffab_ffab)).perform(swipeDown());
    onView(withId(R.id.userInfo_reply_close)).perform(click());
    // verify
    onView(withId(R.id.userInfo_following)).check(matches(isDisplayed()));
    onView(withId(R.id.userInfo_heading)).check(matches(isDisplayed()));

  }

  @Override
  protected ActivityTestRule<UserInfoActivity> getRule() {
    return rule;
  }

  @Override
  protected Intent getIntent() {
    final User user = UserUtil.create();
    userCache.open();
    userCache.upsert(user);
    userCache.close();
    return UserInfoActivity.createIntent(
        InstrumentationRegistry.getTargetContext(), user);
  }
}
