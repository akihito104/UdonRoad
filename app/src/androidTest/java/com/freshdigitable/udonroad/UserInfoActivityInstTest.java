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

import com.freshdigitable.udonroad.subscriber.ConfigRequestWorker;
import com.freshdigitable.udonroad.util.PerformUtil;
import com.freshdigitable.udonroad.util.UserUtil;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import rx.functions.Action0;
import twitter4j.Relationship;
import twitter4j.TwitterException;
import twitter4j.User;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * UserInfoActivityInstTest tests UserInfoActivity in device.
 *
 * Created by akihit on 2016/08/11.
 */
@RunWith(Enclosed.class)
public class UserInfoActivityInstTest {
  public static class WhenTargetIsFollowed extends UserInfoActivityInstTestBase {
    @Override
    protected int setupTimeline() throws TwitterException {
      final Relationship relationship = mock(Relationship.class);
      when(relationship.isSourceFollowingTarget()).thenReturn(true);
      when(relationship.isSourceBlockingTarget()).thenReturn(false);
      when(relationship.isSourceMutingTarget()).thenReturn(false);
      return setupUserInfoTimeline(relationship);
    }

    @Test
    public void showTweetInputView_then_followMenuIconIsHiddenAndCancelMenuIconIsAppeared()
        throws Exception {
      PerformUtil.selectItemViewAt(0);
      PerformUtil.reply();
      // verify
      onView(withId(R.id.tw_intext)).check(matches(withText("@" + getLoginUser().getScreenName() + " ")));
      onView(withId(R.id.action_heading)).check(matches(isDisplayed()));
      onView(withId(R.id.action_group_user)).check(doesNotExist());
      onView(withId(R.id.action_cancel)).check(matches(isDisplayed()));
    }

    @Test
    public void closeTweetInputView_then_followMenuIconIsAppearAndCancelMenuIconIsHidden()
        throws Exception {
      PerformUtil.selectItemViewAt(0);
      PerformUtil.reply();
      onView(withId(R.id.tw_intext)).check(matches(withText("@" + getLoginUser().getScreenName() + " ")));
      PerformUtil.clickCancelWriteOnMenu();
      // verify
      onView(withId(R.id.action_group_user)).check(matches(isDisplayed()));
      onView(withId(R.id.action_heading)).check(matches(isDisplayed()));
      onView(withId(R.id.action_write)).check(doesNotExist());
      onView(withId(R.id.action_cancel)).check(doesNotExist());
    }

    @Test
    public void checkFollowingIsAppeared() {
      onView(withId(R.id.user_following)).check(matches(withText(R.string.user_following)));
      onView(withId(R.id.user_muted)).check(matches(not(isDisplayed())));

      onView(withId(R.id.action_group_user)).perform(click());
      onView(withText(R.string.action_follow)).check(doesNotExist());
      onView(withText(R.string.action_unfollow)).check(matches(isDisplayed()));
      onView(withText(R.string.action_block)).check(matches(isDisplayed()));
      onView(withText(R.string.action_unblock)).check(doesNotExist());
      onView(withText(R.string.action_mute)).check(matches(isDisplayed()));
      onView(withText(R.string.action_unmute)).check(doesNotExist());
      Espresso.pressBack();
    }
  }

  public static class WhenTargetIsNotFollowed extends UserInfoActivityInstTestBase {
    @Override
    protected int setupTimeline() throws TwitterException {
      final Relationship relationship = mock(Relationship.class);
      when(relationship.isSourceFollowingTarget()).thenReturn(false);
      when(relationship.isSourceBlockingTarget()).thenReturn(false);
      when(relationship.isSourceMutingTarget()).thenReturn(false);
      return setupUserInfoTimeline(relationship);
    }

    @Test
    public void checkFollowingIsNotAppeared() {
      onView(withId(R.id.user_following)).check(matches(not(isDisplayed())));
      onView(withId(R.id.user_muted)).check(matches(not(isDisplayed())));

      onView(withId(R.id.action_group_user)).perform(click());
      onView(withText(R.string.action_follow)).check(matches(isDisplayed()));
      onView(withText(R.string.action_unfollow)).check(doesNotExist());
      onView(withText(R.string.action_block)).check(matches(isDisplayed()));
      onView(withText(R.string.action_unblock)).check(doesNotExist());
      onView(withText(R.string.action_mute)).check(matches(isDisplayed()));
      onView(withText(R.string.action_unmute)).check(doesNotExist());
      Espresso.pressBack();
    }
  }

  public static class WhenTargetIsBlocked extends UserInfoActivityInstTestBase {
    @Override
    protected int setupTimeline() throws TwitterException {
      final Relationship relationship = mock(Relationship.class);
      when(relationship.isSourceFollowingTarget()).thenReturn(false);
      when(relationship.isSourceBlockingTarget()).thenReturn(true);
      when(relationship.isSourceMutingTarget()).thenReturn(false);
      return setupUserInfoTimeline(relationship);
    }

    @Test
    public void checkBlockingIsAppeared() {
      onView(withId(R.id.user_following)).check(matches(withText(R.string.user_blocking)));
      onView(withId(R.id.user_muted)).check(matches(not(isDisplayed())));

      onView(withId(R.id.action_group_user)).perform(click());
      onView(withText(R.string.action_follow)).check(matches(isDisplayed()));
      onView(withText(R.string.action_unfollow)).check(doesNotExist());
      onView(withText(R.string.action_block)).check(doesNotExist());
      onView(withText(R.string.action_unblock)).check(matches(isDisplayed()));
      onView(withText(R.string.action_mute)).check(matches(isDisplayed()));
      onView(withText(R.string.action_unmute)).check(doesNotExist());
      Espresso.pressBack();
    }
  }

  public static class WhenTargetIsMuted extends UserInfoActivityInstTestBase {
    @Override
    protected int setupTimeline() throws TwitterException {
      final Relationship relationship = mock(Relationship.class);
      when(relationship.isSourceFollowingTarget()).thenReturn(false);
      when(relationship.isSourceBlockingTarget()).thenReturn(false);
      when(relationship.isSourceMutingTarget()).thenReturn(true);
      return setupUserInfoTimeline(relationship);
    }

    @Test
    public void checkMutingIsAppeared() {
      onView(withId(R.id.user_following)).check(matches(not(isDisplayed())));
      onView(withId(R.id.user_muted)).check(matches(withText(R.string.user_muting)));

      onView(withId(R.id.action_group_user)).perform(click());
      onView(withText(R.string.action_follow)).check(matches(isDisplayed()));
      onView(withText(R.string.action_unfollow)).check(doesNotExist());
      onView(withText(R.string.action_block)).check(matches(isDisplayed()));
      onView(withText(R.string.action_unblock)).check(doesNotExist());
      onView(withText(R.string.action_mute)).check(doesNotExist());
      onView(withText(R.string.action_unmute)).check(matches(isDisplayed()));
      Espresso.pressBack();
    }
  }

  public static class WhenTargetIsFollowedAndMuted extends UserInfoActivityInstTestBase {
    @Override
    protected int setupTimeline() throws TwitterException {
      final Relationship relationship = mock(Relationship.class);
      when(relationship.isSourceFollowingTarget()).thenReturn(true);
      when(relationship.isSourceBlockingTarget()).thenReturn(false);
      when(relationship.isSourceMutingTarget()).thenReturn(true);
      return setupUserInfoTimeline(relationship);
    }

    @Test
    public void checkFollowingAndMutingAreAppeared() {
      onView(withId(R.id.user_following)).check(matches(withText(R.string.user_following)));
      onView(withId(R.id.user_muted)).check(matches(withText(R.string.user_muting)));

      onView(withId(R.id.action_group_user)).perform(click());
      onView(withText(R.string.action_follow)).check(doesNotExist());
      onView(withText(R.string.action_unfollow)).check(matches(isDisplayed()));
      onView(withText(R.string.action_block)).check(matches(isDisplayed()));
      onView(withText(R.string.action_unblock)).check(doesNotExist());
      onView(withText(R.string.action_mute)).check(doesNotExist());
      onView(withText(R.string.action_unmute)).check(matches(isDisplayed()));
      Espresso.pressBack();
    }
  }

  public abstract static class UserInfoActivityInstTestBase extends TimelineInstTestBase {
    @Rule
    public ActivityTestRule<UserInfoActivity> rule
        = new ActivityTestRule<>(UserInfoActivity.class, false, false);
    private ConfigSetupIdlingResource idlingResource;

    @Override
    protected ActivityTestRule<UserInfoActivity> getRule() {
      return rule;
    }

    @Override
    protected Intent getIntent() {
      final User user = UserUtil.create();
      InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
        @Override
        public void run() {
          userCache.open();
          userCache.upsert(user);
          userCache.close();
        }
      });
      return UserInfoActivity.createIntent(
          InstrumentationRegistry.getTargetContext(), user);
    }

    @Inject
    ConfigRequestWorker configRequestWorker;

    @Override
    public void setupConfig(User user) throws Exception {
      super.setupConfig(user);
      getComponent().inject(this);

      idlingResource = new ConfigSetupIdlingResource();
      Espresso.registerIdlingResources(idlingResource);

      InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
        @Override
        public void run() {
          configRequestWorker.open();
          configRequestWorker.setup(new Action0() {
            @Override
            public void call() {
              idlingResource.setDoneSetup(true);
            }
          });
        }
      });
    }

    @Override
    protected void verifyAfterLaunch() {
      onView(withId(R.id.user_screen_name)).check(matches(screenNameMatcher));
      onView(withId(R.id.action_group_user)).check(matches(isDisplayed()));
      onView(withId(R.id.action_heading)).check(matches(isDisplayed()));
      onView(withId(R.id.action_write)).check(doesNotExist());
    }

    @Override
    @After
    public void tearDown() throws Exception {
      Espresso.unregisterIdlingResources(idlingResource);
      InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
        @Override
        public void run() {
          configRequestWorker.close();
        }
      });
      super.tearDown();
    }
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
