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

import android.support.annotation.NonNull;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.contrib.NavigationViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;

import com.freshdigitable.udonroad.listitem.UserItemView;
import com.freshdigitable.udonroad.util.AssertionUtil;
import com.freshdigitable.udonroad.util.PerformUtil;
import com.freshdigitable.udonroad.util.TwitterResponseMock;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Callable;

import twitter4j.PagableResponseList;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.User;
import twitter4j.UserList;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.IdlingResourceUtil.getOpenDrawerIdlingResource;
import static com.freshdigitable.udonroad.util.IdlingResourceUtil.getSimpleIdlingResource;
import static com.freshdigitable.udonroad.util.IdlingResourceUtil.runWithIdlingResource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Created by akihit on 2017/09/16.
 */
@RunWith(AndroidJUnit4.class)
public class OwnedListInstTest extends TimelineInstTestBase {
  @Rule
  public ActivityTestRule<MainActivity> rule
      = new ActivityTestRule<>(MainActivity.class, false, false);

  @Test
  public void showOwnedList() {
    PerformUtil.openDrawerNavigation();
    runWithIdlingResource(getOpenDrawerIdlingResource(rule.getActivity()), () ->
        onView(withId(R.id.nav_header_account)).check(matches(isDisplayed()))
    );

    onView(withId(R.id.nav_drawer)).perform(NavigationViewActions.navigateTo(R.id.drawer_menu_lists));
    runWithIdlingResource(getSimpleIdlingResource("show lists", isTimelineLoaded()), () -> {
      AssertionUtil.checkMainActivityTitle(R.string.title_owned_list);
      onView(withText("list1")).check(matches(isDisplayed()));
    });

    Espresso.pressBack();
    runWithIdlingResource(getSimpleIdlingResource("show home", isTimelineLoaded()), () ->
        AssertionUtil.checkMainActivityTitle(R.string.title_home));
  }

  @Test
  public void showTimelineFromOwnedList() {
    PerformUtil.openDrawerNavigation();
    runWithIdlingResource(getOpenDrawerIdlingResource(rule.getActivity()), () ->
        onView(withId(R.id.nav_header_account)).check(matches(isDisplayed()))
    );

    onView(withId(R.id.nav_drawer)).perform(NavigationViewActions.navigateTo(R.id.drawer_menu_lists));
    runWithIdlingResource(getSimpleIdlingResource("show lists", isTimelineLoaded()), () -> {
      AssertionUtil.checkMainActivityTitle(R.string.title_owned_list);
      onView(withText("list1")).check(matches(isDisplayed()));
    });

    PerformUtil.selectItemViewAt(0, UserItemView.class);
    runWithIdlingResource(getSimpleIdlingResource("show listTL", isTimelineLoaded()), () ->
        AssertionUtil.checkMainActivityTitle("list0"));

    Espresso.pressBack();
    runWithIdlingResource(getSimpleIdlingResource("show lists", isTimelineLoaded()), () -> {
      AssertionUtil.checkMainActivityTitle(R.string.title_owned_list);
      onView(withText("list0")).check(matches(isDisplayed()));
    });
  }

  @NonNull
  private Callable<Boolean> isTimelineLoaded() {
    return () -> {
      final RecyclerView timeline = rule.getActivity().findViewById(R.id.timeline);
      final int childCount = timeline.getLayoutManager().getChildCount();
      return childCount > 0;
    };
  }

  @Override
  protected void setupConfig(User loginUser) throws Exception {
    super.setupConfig(loginUser);
    final PagableResponseList<UserList> userListResponse = TwitterResponseMock.createUserListResponse(20);
    when(twitter.getUserListsOwnerships(anyLong(), anyInt(), anyLong())).thenReturn(userListResponse);
    final ResponseList<Status> statuses = createDefaultResponseList(getLoginUser());
    when(twitter.getUserListStatuses(anyLong(), any(Paging.class))).thenReturn(statuses);
  }

  @Override
  protected int setupTimeline() throws TwitterException {
    return setupDefaultTimeline();
  }

  @Override
  protected ActivityTestRule<? extends AppCompatActivity> getRule() {
    return rule;
  }
}
