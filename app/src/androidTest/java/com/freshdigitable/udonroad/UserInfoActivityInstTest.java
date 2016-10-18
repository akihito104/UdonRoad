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
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.rule.ActivityTestRule;

import com.freshdigitable.udonroad.util.UserUtil;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import rx.functions.Action0;
import twitter4j.PagableResponseList;
import twitter4j.Paging;
import twitter4j.Relationship;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.User;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusView;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createResponseList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * UserInfoActivityInstTest tests UserInfoActivity in device.
 *
 * Created by akihit on 2016/08/11.
 */
public class UserInfoActivityInstTest extends TimelineInstTestBase {
  @Rule
  public ActivityTestRule<UserInfoActivity> rule
      = new ActivityTestRule<>(UserInfoActivity.class, false, false);
  private ConfigSetupIdlingResource idlingResource;

  @Test
  public void showTweetInputView_then_followMenuIconIsHiddenAndCancelMenuIconIsAppeared()
      throws Exception {
    final Status replied = findByStatusId(20000);
    onView(ofStatusView(withText(replied.getText()))).perform(click());
    onView(withId(R.id.iffab_ffab)).perform(swipeDown());
    // verify
    onView(withId(R.id.userInfo_heading)).check(matches(isDisplayed()));
    onView(withId(R.id.userInfo_reply_close)).check(matches(isDisplayed()));
  }

  @Test
  public void closeTweetInputView_then_followMenuIconIsAppearAndCancelMenuIconIsHidden()
      throws Exception {
    final Status replied = findByStatusId(20000);
    onView(ofStatusView(withText(replied.getText()))).perform(click());
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

  @Inject
  ConfigSubscriber configSubscriber;

  @Override
  public void setupConfig(User user) throws Exception {
    super.setupConfig(user);
    getComponent().inject(this);

    idlingResource = new ConfigSetupIdlingResource();
    Espresso.registerIdlingResources(idlingResource);

    InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
      @Override
      public void run() {
        configSubscriber.open();
        configSubscriber.setup(new Action0() {
          @Override
          public void call() {
            idlingResource.setDoneSetup(true);
          }
        });
      }
    });
  }

  @Override
  protected void setupTimeline() throws TwitterException {
    final User loginUser = getLoginUser();
    final ResponseList<Status> responseList = createDefaultResponseList(loginUser);
    super.responseList = responseList;
    final ResponseList<Status> emptyStatusResponseList = createResponseList();

    final int size = responseList.size();
    when(loginUser.getStatusesCount()).thenReturn(size);
    when(twitter.getUserTimeline(loginUser.getId())).thenReturn(responseList);
    when(twitter.getUserTimeline(anyLong(), any(Paging.class))).thenReturn(emptyStatusResponseList);
    when(twitter.getFavorites(anyLong())).thenReturn(emptyStatusResponseList);
    final PagableResponseList<User> emptyUserPagableResponseList = mock(PagableResponseList.class);
    when(twitter.getFollowersList(anyLong(), anyLong())).thenReturn(emptyUserPagableResponseList);
    when(twitter.getFriendsList(anyLong(), anyLong())).thenReturn(emptyUserPagableResponseList);

    final Relationship relationship = mock(Relationship.class);
    when(relationship.isSourceFollowingTarget()).thenReturn(true);
    when(relationship.isSourceMutingTarget()).thenReturn(false);
    when(twitter.showFriendship(anyLong(), anyLong())).thenReturn(relationship);
  }

  @Override
  protected void verifyAfterLaunch() {
    onView(withId(R.id.user_screen_name)).check(matches(withText("@akihito104")));
    onView(withId(R.id.userInfo_following)).check(matches(isDisplayed()));
    onView(withId(R.id.userInfo_heading)).check(matches(isDisplayed()));
  }

  @Override
  @After
  public void tearDown() throws Exception {
    Espresso.unregisterIdlingResources(idlingResource);
    InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
      @Override
      public void run() {
        configSubscriber.close();
      }
    });
    super.tearDown();
  }

  private static class ConfigSetupIdlingResource implements IdlingResource {
    @Override
    public String getName() {
      return "setupUserInfo";
    }

    @Override
    public boolean isIdleNow() {
      if (doneSetup.get()) {
        if (callback != null) {
          callback.onTransitionToIdle();
        }
        return true;
      }
      return false;
    }

    private ResourceCallback callback;

    @Override
    public void registerIdleTransitionCallback(ResourceCallback callback) {
      this.callback = callback;
    }

    private AtomicBoolean doneSetup = new AtomicBoolean(false);

    private void setDoneSetup(boolean doneSetup) {
      this.doneSetup.set(doneSetup);
    }
  }
}
