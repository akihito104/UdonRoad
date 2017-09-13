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

import android.app.Activity;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.contrib.NavigationViewActions;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.lifecycle.Stage;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.freshdigitable.udonroad.util.IdlingResourceUtil;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import twitter4j.TwitterException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.freshdigitable.udonroad.util.IdlingResourceUtil.getActivityStageIdlingResource;
import static com.freshdigitable.udonroad.util.IdlingResourceUtil.runWithIdlingResource;
import static com.freshdigitable.udonroad.util.PerformUtil.closeDrawerNavigation;
import static com.freshdigitable.udonroad.util.PerformUtil.openDrawerNavigation;
import static org.hamcrest.CoreMatchers.not;

/**
 * Created by akihit on 2017/09/11.
 */
@RunWith(AndroidJUnit4.class)
public class NavDrawerInstTest extends TimelineInstTestBase {
  @Rule
  public final ActivityTestRule<MainActivity> rule
      = new ActivityTestRule<>(MainActivity.class, false, false);

  @Test
  public void openDrawer() {
    onView(withId(R.id.nav_header_account)).check(matches(not(isDisplayed())));
    openDrawerNavigation();

    runWithIdlingResource(getOpenDrawerIdlingResource(), () -> {
      onView(withId(R.id.nav_header_account)).check(matches(isDisplayed()));
      onView(withId(R.id.nav_drawer)).check(matches(isDefaultNavMenu()));
    });
  }

  @Test
  public void clickAccountAndShownAddAccount() {
    openDrawerNavigation();

    runWithIdlingResource(getOpenDrawerIdlingResource(), () -> {
      onView(withId(R.id.nav_header_account)).check(matches(isDisplayed())).perform(click());
      onView(withId(R.id.nav_drawer)).check(matches(isSelectAccountMenu()));
    });
  }

  @Test
  public void clickAddAccount_then_OAuthActivityIsShown() {
    openDrawerNavigation();

    runWithIdlingResource(getOpenDrawerIdlingResource(), () -> {
      onView(withId(R.id.nav_header_account)).check(matches(isDisplayed())).perform(click());
      onView(withId(R.id.nav_drawer)).check(matches(isSelectAccountMenu()));
    });

    onView(withId(R.id.nav_drawer)).perform(NavigationViewActions.navigateTo(R.id.drawer_menu_add_account));
    runWithIdlingResource(
        getActivityStageIdlingResource("launch OAuth", OAuthActivity.class, Stage.RESUMED), () ->
            onView(withId(R.id.oauth_pin)).check(matches(isDisplayed())));

    InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
      final Activity activity = IdlingResourceUtil.findActivityByStage(OAuthActivity.class, Stage.RESUMED);
      if (activity != null) {
        activity.finish();
      }
    });
  }

  @Test
  public void pressBackOnOAuth_then_MainActivityIsShown() {
    openDrawerNavigation();

    runWithIdlingResource(getOpenDrawerIdlingResource(), () -> {
      onView(withId(R.id.nav_header_account)).check(matches(isDisplayed())).perform(click());
      onView(withId(R.id.nav_drawer)).check(matches(isSelectAccountMenu()));
    });

    onView(withId(R.id.nav_drawer)).perform(NavigationViewActions.navigateTo(R.id.drawer_menu_add_account));
    runWithIdlingResource(
        getActivityStageIdlingResource("launch OAuth", OAuthActivity.class, Stage.RESUMED), () ->
            onView(withId(R.id.oauth_pin)).check(matches(isDisplayed())));

    Espresso.pressBack();
    runWithIdlingResource(
        getActivityStageIdlingResource("launch Main", MainActivity.class, Stage.RESUMED), () ->
            onView(withId(R.id.main_toolbar)).check(matches(isDisplayed())));

    InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
      final Activity mainActivity = IdlingResourceUtil.findActivityByStage(MainActivity.class, Stage.RESUMED);
      if (mainActivity != null) {
        mainActivity.finish();
      }
    });
  }

  @Test
  public void closeAddAccountWithBackButton() {
    openDrawerNavigation();

    runWithIdlingResource(getOpenDrawerIdlingResource(), () -> {
      onView(withId(R.id.nav_header_account)).check(matches(isDisplayed())).perform(click());
      onView(withId(R.id.nav_drawer)).check(matches(isSelectAccountMenu()));

      Espresso.pressBack();
      onView(withId(R.id.nav_drawer)).check(matches(isDefaultNavMenu()));
    });
  }

  @Test
  public void clickAccountAndReopen_then_shownDefaultMenu() {
    openDrawerNavigation();

    runWithIdlingResource(getOpenDrawerIdlingResource(), () -> {
      onView(withId(R.id.nav_header_account)).check(matches(isDisplayed())).perform(click());
      onView(withId(R.id.nav_drawer)).check(matches(isSelectAccountMenu()));
    });

    closeDrawerNavigation();

    runWithIdlingResource(getCloseDrawerIdlingResource(), () ->
        onView(withId(R.id.nav_header_account)).check(matches(not(isDisplayed()))));

    openDrawerNavigation();

    runWithIdlingResource(getOpenDrawerIdlingResource(), () ->
        onView(withId(R.id.nav_drawer)).check(matches(isDefaultNavMenu())));
  }

  @NonNull
  private IdlingResource getOpenDrawerIdlingResource() {
    return IdlingResourceUtil.getSimpleIdlingResource("open drawer", () -> {
      final DrawerLayout drawerLayout = rule.getActivity().findViewById(R.id.nav_drawer_layout);
      final View drawer = rule.getActivity().findViewById(R.id.nav_drawer);
      return drawerLayout.isDrawerOpen(drawer);
    });
  }

  @NonNull
  private IdlingResource getCloseDrawerIdlingResource() {
    return IdlingResourceUtil.getSimpleIdlingResource("close drawer", () -> {
      final DrawerLayout drawerLayout = rule.getActivity().findViewById(R.id.nav_drawer_layout);
      final View drawer = rule.getActivity().findViewById(R.id.nav_drawer);
      return !drawerLayout.isDrawerOpen(drawer);
    });
  }

  private static Matcher<View> isDefaultNavMenu() {
    return new BoundedMatcher<View, NavigationView>(NavigationView.class) {
      @Override
      protected boolean matchesSafely(NavigationView view) {
        return checkVisibleMenuGroup(view, R.id.drawer_menu_default);
      }

      @Override
      public void describeTo(Description description) {}
    };
  }

  private static Matcher<View> isSelectAccountMenu() {
    return new BoundedMatcher<View, NavigationView>(NavigationView.class) {
      @Override
      protected boolean matchesSafely(NavigationView item) {
        return checkVisibleMenuGroup(item, R.id.drawer_menu_accounts);
      }

      @Override
      public void describeTo(Description description) {}
    };
  }

  private static boolean checkVisibleMenuGroup(NavigationView view, @IdRes int visibleGroupId) {
    final Menu menu = view.getMenu();
    if (menu.size() < 1) {
      return false;
    }
    for (int i = 0; i < menu.size(); i++) {
      final MenuItem item = menu.getItem(i);
      final boolean visible = item.getGroupId() == visibleGroupId;
      if (item.isVisible() != visible) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected int setupTimeline() throws TwitterException {
    return 0;
  }

  @Override
  protected ActivityTestRule<? extends AppCompatActivity> getRule() {
    return rule;
  }
}
