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

import org.junit.Test;

import java.util.Arrays;

import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.User;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.selectedDescendantsMatch;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusViewAt;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createResponseList;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatus;
import static org.mockito.Mockito.when;

/**
 * Created by akihit on 2016/12/14.
 */

public class VerifiedUserTest extends TimelineInstTestBase {

  private ActivityTestRule<MainActivity> rule
      = new ActivityTestRule<>(MainActivity.class, false, false);

  @Test
  public void showVerifiedIconForVerifiedUsersTweet() {
    final User verifiedUser = UserUtil.createVerifiedUser();
    onView(ofStatusViewAt(R.id.timeline, 2))
        .check(selectedDescendantsMatch(withId(R.id.tl_names),
            withText(verifiedUser.getName() + " @" + verifiedUser.getScreenName() + " ")));
  }

  @Test
  public void showProtectedIconForProtectedUsersTweet() {
    final User protectedUser = UserUtil.createProtectedUser();
    onView(ofStatusViewAt(R.id.timeline, 1))
        .check(selectedDescendantsMatch(withId(R.id.tl_names),
            withText(protectedUser.getName() + " @" + protectedUser.getScreenName() + " ")));
  }

  @Test
  public void showBothIconForVerifiedAndProtectedUsersTweet() {
    final User bothUser = UserUtil.createVerifiedAndProtectedUser();
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(selectedDescendantsMatch(withId(R.id.tl_names),
            withText(bothUser.getName() + " @" + bothUser.getScreenName() + "  ")));
  }

  @Override
  protected int setupTimeline() throws TwitterException {
    final ResponseList<Status> responseList = createResponseList(Arrays.asList(
        createStatus(10002L, UserUtil.createVerifiedAndProtectedUser()),
        createStatus(10001L, UserUtil.createProtectedUser()),
        createStatus(10000L, UserUtil.createVerifiedUser())
    ));
    when(twitter.getHomeTimeline()).thenReturn(responseList);
    return responseList.size();
  }

  @Override
  protected ActivityTestRule<? extends AppCompatActivity> getRule() {
    return rule;
  }
}
