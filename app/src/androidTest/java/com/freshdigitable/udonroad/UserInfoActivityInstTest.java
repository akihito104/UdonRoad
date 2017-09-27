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
import android.support.annotation.StringRes;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.rule.ActivityTestRule;

import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.subscriber.ConfigRequestWorker;
import com.freshdigitable.udonroad.util.AssertionUtil;
import com.freshdigitable.udonroad.util.TestInjectionUtil;
import com.freshdigitable.udonroad.util.UserUtil;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import twitter4j.Relationship;
import twitter4j.TwitterException;
import twitter4j.User;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.UserInfoActivityInstTest.ActionItem.BLOCK;
import static com.freshdigitable.udonroad.UserInfoActivityInstTest.ActionItem.BLOCK_RT;
import static com.freshdigitable.udonroad.UserInfoActivityInstTest.ActionItem.FOLLOW;
import static com.freshdigitable.udonroad.UserInfoActivityInstTest.ActionItem.MUTE;
import static com.freshdigitable.udonroad.UserInfoActivityInstTest.ActionItem.UNBLOCK;
import static com.freshdigitable.udonroad.UserInfoActivityInstTest.ActionItem.UNBLOCK_RT;
import static com.freshdigitable.udonroad.UserInfoActivityInstTest.ActionItem.UNFOLLOW;
import static com.freshdigitable.udonroad.UserInfoActivityInstTest.ActionItem.UNMUTE;
import static com.freshdigitable.udonroad.UserInfoActivityInstTest.AltAction.BLOCKING;
import static com.freshdigitable.udonroad.UserInfoActivityInstTest.AltAction.BLOCKING_RT;
import static com.freshdigitable.udonroad.UserInfoActivityInstTest.AltAction.FOLLOWING;
import static com.freshdigitable.udonroad.UserInfoActivityInstTest.AltAction.MUTING;
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * UserInfoActivityInstTest tests UserInfoActivity in device.
 *
 * Created by akihit on 2016/08/11.
 */
@RunWith(Enclosed.class)
public class UserInfoActivityInstTest {
  public static class WhenTargetIsFollowed extends Base {
    @Override
    protected int setupTimeline() throws TwitterException {
      final Relationship relationship = getRelationship();
      when(relationship.isSourceFollowingTarget()).thenReturn(true);
      when(relationship.isSourceBlockingTarget()).thenReturn(false);
      when(relationship.isSourceMutingTarget()).thenReturn(false);
      when(relationship.isSourceWantRetweets()).thenReturn(true);
      return setupUserInfoTimeline(relationship);
    }

    @Test
    public void checkFollowingIsAppeared() {
      AssertionUtil.checkUserInfoActivityTitle("");
      onView(withId(R.id.user_following)).check(matches(withText(R.string.user_following)));
      onView(withId(R.id.user_muted)).check(matches(not(isDisplayed())));

      onView(withId(R.id.action_group_user)).perform(click());
      FOLLOWING.checkVisibleItemIs(UNFOLLOW);
      BLOCKING.checkVisibleItemIs(BLOCK);
      MUTING.checkVisibleItemIs(MUTE);
      BLOCKING_RT.checkVisibleItemIs(BLOCK_RT);
      pressBack();
    }

    @Test
    public void clickUnfollowInOptionsMenu_then_FollowingIsDisappeared() throws Exception {
      AssertionUtil.checkUserInfoActivityTitle("");
      when(twitter.destroyFriendship(anyLong())).thenReturn(getLoginUser());

      onView(withId(R.id.user_following)).check(matches(isDisplayed()));
      onView(withId(R.id.action_group_user)).perform(click());
      onView(withText(R.string.action_unfollow)).perform(click());
      onView(withId(R.id.user_following)).check(matches(not(isDisplayed())));

      onView(withId(R.id.action_group_user)).perform(click());
      FOLLOW.checkVisible();
      pressBack();
    }
  }

  public static class WhenTargetIsNotFollowed extends Base {
    @Override
    protected int setupTimeline() throws TwitterException {
      final Relationship relationship = getRelationship();
      when(relationship.isSourceFollowingTarget()).thenReturn(false);
      when(relationship.isSourceBlockingTarget()).thenReturn(false);
      when(relationship.isSourceMutingTarget()).thenReturn(false);
      when(relationship.isSourceWantRetweets()).thenReturn(false);
      return setupUserInfoTimeline(relationship);
    }

    @Test
    public void checkFollowingIsNotAppeared() {
      AssertionUtil.checkUserInfoActivityTitle("");
      onView(withId(R.id.user_following)).check(matches(not(isDisplayed())));
      onView(withId(R.id.user_muted)).check(matches(not(isDisplayed())));

      onView(withId(R.id.action_group_user)).perform(click());
      FOLLOWING.checkVisibleItemIs(FOLLOW);
      BLOCKING.checkVisibleItemIs(BLOCK);
      MUTING.checkVisibleItemIs(MUTE);
      BLOCKING_RT.checkBothInvisible();
      pressBack();
    }

    @Test
    public void clickMuteInOptionsMenu_then_MutingIsAppear() throws Exception {
      AssertionUtil.checkUserInfoActivityTitle("");
      when(twitter.createMute(anyLong())).thenReturn(getLoginUser());

      onView(withId(R.id.user_muted)).check(matches(not(isDisplayed())));
      onView(withId(R.id.action_group_user)).perform(click());
      onView(withText(R.string.action_mute)).perform(click());
      onView(withId(R.id.user_muted)).check(matches(isDisplayed()));

      onView(withId(R.id.action_group_user)).perform(click());
      MUTING.checkVisibleItemIs(UNMUTE);
      pressBack();
    }

    @Test
    public void clickFollowInOptionsMenu_then_FollowingIsAppear() throws Exception {
      AssertionUtil.checkUserInfoActivityTitle("");
      when(twitter.createFriendship(anyLong())).thenReturn(getLoginUser());

      onView(withId(R.id.user_following)).check(matches(not(isDisplayed())));
      onView(withId(R.id.action_group_user)).perform(click());
      onView(withText(R.string.action_follow)).perform(click());
      onView(withId(R.id.user_following)).check(matches(isDisplayed()));

      onView(withId(R.id.action_group_user)).perform(click());
      FOLLOWING.checkVisibleItemIs(UNFOLLOW);
      pressBack();
    }

    @Test
    public void clickBlockInOptionsMenu_then_BlockingIsAppeared() throws Exception {
      AssertionUtil.checkUserInfoActivityTitle("");
      when(twitter.createBlock(anyLong())).thenReturn(getLoginUser());

      onView(withId(R.id.user_following)).check(matches(not(isDisplayed())));
      onView(withId(R.id.action_group_user)).perform(click());
      onView(withText(R.string.action_block)).perform(click());
      onView(withId(R.id.user_following)).check(matches(isDisplayed()));
      onView(withId(R.id.user_following)).check(matches(withText(R.string.user_blocking)));

      onView(withId(R.id.action_group_user)).perform(click());
      BLOCKING.checkVisibleItemIs(UNBLOCK);
      pressBack();
    }
  }

  public static class WhenTargetIsBlocked extends Base {
    @Override
    protected int setupTimeline() throws TwitterException {
      final Relationship relationship = getRelationship();
      when(relationship.isSourceFollowingTarget()).thenReturn(false);
      when(relationship.isSourceBlockingTarget()).thenReturn(true);
      when(relationship.isSourceMutingTarget()).thenReturn(false);
      when(relationship.isSourceWantRetweets()).thenReturn(false);
      return setupUserInfoTimeline(relationship);
    }

    @Test
    public void checkBlockingIsAppeared() {
      AssertionUtil.checkUserInfoActivityTitle("");
      onView(withId(R.id.user_following)).check(matches(withText(R.string.user_blocking)));
      onView(withId(R.id.user_muted)).check(matches(not(isDisplayed())));

      onView(withId(R.id.action_group_user)).perform(click());
      FOLLOWING.checkVisibleItemIs(FOLLOW);
      BLOCKING.checkVisibleItemIs(UNBLOCK);
      MUTING.checkVisibleItemIs(MUTE);
      BLOCKING_RT.checkBothInvisible();
      pressBack();
    }

    @Test
    public void clickUnblockInOptionsMenu_then_BlockingIsDisappeared() throws Exception {
      AssertionUtil.checkUserInfoActivityTitle("");
      when(twitter.destroyBlock(anyLong())).thenReturn(getLoginUser());

      onView(withId(R.id.user_following)).check(matches(isDisplayed()));
      onView(withId(R.id.action_group_user)).perform(click());
      onView(withText(R.string.action_unblock)).perform(click());
      onView(withId(R.id.user_following)).check(matches(not(isDisplayed())));

      onView(withId(R.id.action_group_user)).perform(click());
      BLOCKING.checkVisibleItemIs(BLOCK);
      pressBack();
    }
  }

  public static class WhenTargetIsMuted extends Base {
    @Override
    protected int setupTimeline() throws TwitterException {
      final Relationship relationship = getRelationship();
      when(relationship.isSourceFollowingTarget()).thenReturn(false);
      when(relationship.isSourceBlockingTarget()).thenReturn(false);
      when(relationship.isSourceMutingTarget()).thenReturn(true);
      when(relationship.isSourceWantRetweets()).thenReturn(false);
      return setupUserInfoTimeline(relationship);
    }

    @Test
    public void checkMutingIsAppeared() {
      AssertionUtil.checkUserInfoActivityTitle("");
      onView(withId(R.id.user_following)).check(matches(not(isDisplayed())));
      onView(withId(R.id.user_muted)).check(matches(withText(R.string.user_muting)));

      onView(withId(R.id.action_group_user)).perform(click());
      FOLLOWING.checkVisibleItemIs(FOLLOW);
      BLOCKING.checkVisibleItemIs(BLOCK);
      MUTING.checkVisibleItemIs(UNMUTE);
      BLOCKING_RT.checkBothInvisible();
      pressBack();
    }

    @Test
    public void clickUnmuteInOptionsMenu_then_MutingIsDisappear() throws Exception {
      AssertionUtil.checkUserInfoActivityTitle("");
      when(twitter.destroyMute(anyLong())).thenReturn(getLoginUser());

      onView(withId(R.id.user_muted)).check(matches(withText(R.string.user_muting)));
      onView(withId(R.id.action_group_user)).perform(click());
      onView(withText(R.string.action_unmute)).perform(click());
      onView(withId(R.id.user_muted)).check(matches(not(isDisplayed())));

      onView(withId(R.id.action_group_user)).perform(click());
      MUTING.checkVisibleItemIs(MUTE);
      pressBack();
    }
  }

  public static class WhenTargetIsFollowedAndMuted extends Base {
    @Override
    protected int setupTimeline() throws TwitterException {
      final Relationship relationship = getRelationship();
      when(relationship.isSourceFollowingTarget()).thenReturn(true);
      when(relationship.isSourceBlockingTarget()).thenReturn(false);
      when(relationship.isSourceMutingTarget()).thenReturn(true);
      when(relationship.isSourceWantRetweets()).thenReturn(true);
      return setupUserInfoTimeline(relationship);
    }

    @Test
    public void checkFollowingAndMutingAreAppeared() {
      AssertionUtil.checkUserInfoActivityTitle("");
      onView(withId(R.id.user_following)).check(matches(withText(R.string.user_following)));
      onView(withId(R.id.user_muted)).check(matches(withText(R.string.user_muting)));

      onView(withId(R.id.action_group_user)).perform(click());
      FOLLOWING.checkVisibleItemIs(UNFOLLOW);
      BLOCKING.checkVisibleItemIs(BLOCK);
      MUTING.checkVisibleItemIs(UNMUTE);
      BLOCKING_RT.checkVisibleItemIs(BLOCK_RT);
      pressBack();
    }
  }

  public static class WhenTargetIsBlockedRetweet extends Base {
    @Override
    protected int setupTimeline() throws TwitterException {
      final Relationship relationship = getRelationship();
      when(relationship.isSourceFollowingTarget()).thenReturn(true);
      when(relationship.isSourceBlockingTarget()).thenReturn(false);
      when(relationship.isSourceMutingTarget()).thenReturn(false);
      when(relationship.isSourceWantRetweets()).thenReturn(false);
      return setupUserInfoTimeline(relationship);
    }

    @Test
    public void checkBlockRTisAppearedInOptionMenu() {
      AssertionUtil.checkUserInfoActivityTitle("");
      onView(withId(R.id.user_following)).check(matches(withText(R.string.user_following)));

      onView(withId(R.id.action_group_user)).perform(click());
      FOLLOWING.checkVisibleItemIs(UNFOLLOW);
      BLOCKING.checkVisibleItemIs(BLOCK);
      MUTING.checkVisibleItemIs(MUTE);
      BLOCKING_RT.checkVisibleItemIs(UNBLOCK_RT);
      pressBack();
    }
  }

  enum AltAction {
    FOLLOWING(FOLLOW, UNFOLLOW), BLOCKING(BLOCK, UNBLOCK),
    MUTING(MUTE, UNMUTE), BLOCKING_RT(BLOCK_RT, UNBLOCK_RT),;

    final ActionItem a1, a2;
    AltAction(ActionItem a1, ActionItem a2) {
      this.a1 = a1;
      this.a2 = a2;
    }

    void checkVisibleItemIs(ActionItem item) {
      if (item != a1 && item != a2) {
        throw new IllegalArgumentException();
      }
      item.checkVisible();
      if (item == a1) {
        a2.checkInvisible();
      } else {
        a1.checkInvisible();
      }
    }

    void checkBothInvisible() {
      a1.checkInvisible();
      a2.checkInvisible();
    }
  }

  enum ActionItem {
    FOLLOW(R.string.action_follow), UNFOLLOW(R.string.action_unfollow),
    BLOCK(R.string.action_block),UNBLOCK(R.string.action_unblock),
    MUTE(R.string.action_mute), UNMUTE(R.string.action_unmute),
    BLOCK_RT(R.string.action_block_retweet), UNBLOCK_RT(R.string.action_unblock_retweet);

    @StringRes final int actionId;

    ActionItem(@StringRes int actionId) {
      this.actionId = actionId;
    }

    void checkVisible() {
      onView(withText(actionId)).check(matches(isDisplayed()));
    }

    private void checkInvisible() {
      onView(withText(actionId)).check(doesNotExist());
    }
  }

  public abstract static class Base extends TimelineInstTestBase {
    @Rule
    public final ActivityTestRule<UserInfoActivity> rule
        = new ActivityTestRule<>(UserInfoActivity.class, false, false);
    private ConfigSetupIdlingResource idlingResource;

    @Override
    protected ActivityTestRule<UserInfoActivity> getRule() {
      return rule;
    }

    @Inject
    TypedCache<User> userCache;

    @Override
    protected Intent getIntent() {
      final User user = UserUtil.createUserA();
      InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
        userCache.open();
        userCache.upsert(user);
      });
      return UserInfoActivity.createIntent(
          InstrumentationRegistry.getTargetContext(), user);
    }

    Relationship getRelationship() {
      final Relationship relationship = mock(Relationship.class);
      final long userId = UserUtil.createUserA().getId();
      when(relationship.getTargetUserId()).thenReturn(userId);
      return relationship;
    }

    @Inject
    ConfigRequestWorker configRequestWorker;

    @Override
    public void setupConfig(User user) throws Exception {
      super.setupConfig(user);
      TestInjectionUtil.getComponent().inject(this);

      idlingResource = new ConfigSetupIdlingResource();
      Espresso.registerIdlingResources(idlingResource);

      InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
          configRequestWorker.setup().subscribe(() -> idlingResource.setDoneSetup(true)));
    }

    @Override
    protected void verifyAfterLaunch() {
      onView(withId(R.id.user_screen_name)).check(matches(screenNameMatcher));
      onView(withId(R.id.action_group_user)).check(matches(isDisplayed()));
      onView(withId(R.id.action_heading)).check(matches(isDisplayed()));
      onView(withId(R.id.action_writeTweet)).check(doesNotExist());
    }

    @Override
    @After
    public void tearDown() throws Exception {
      InstrumentationRegistry.getInstrumentation().runOnMainSync(userCache::close);
      Espresso.unregisterIdlingResources(idlingResource);
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

    private final AtomicBoolean doneSetup = new AtomicBoolean(false);

    private void setDoneSetup(boolean doneSetup) {
      this.doneSetup.set(doneSetup);
    }
  }
}
