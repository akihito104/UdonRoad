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

import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.freshdigitable.udonroad.util.IdlingResourceUtil;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;

import twitter4j.TwitterException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.freshdigitable.udonroad.util.PerformUtil.closeDrawerNavigation;
import static com.freshdigitable.udonroad.util.PerformUtil.openDrawerNavigation;
import static org.hamcrest.CoreMatchers.not;

/**
 * Created by akihit on 2017/09/11.
 */
@RunWith(AndroidJUnit4.class)
public class NavDrawerInstTest extends TimelineInstTestBase {
  private final ActivityTestRule<MainActivity> rule
      = new ActivityTestRule<>(MainActivity.class, false, false);

  @Test
  public void openDrawer() {
    onView(withId(R.id.nav_header_account)).check(matches(not(isDisplayed())));
    openDrawerNavigation();

    final IdlingResource ir = getOpenDrawerIdlingResource();
    try {
      Espresso.registerIdlingResources(ir);
      onView(withId(R.id.nav_header_account)).check(matches(isDisplayed()));
      onView(withId(R.id.nav_drawer)).check(matches(isDefaultNavMenu()));
    } finally {
      Espresso.unregisterIdlingResources(ir);
    }
  }

  @Test
  public void clickAccountAndShownAddAccount() {
    openDrawerNavigation();

    final IdlingResource ir = getOpenDrawerIdlingResource();
    try {
      Espresso.registerIdlingResources(ir);
      onView(withId(R.id.nav_header_account)).check(matches(isDisplayed())).perform(click());
      onView(withId(R.id.nav_drawer)).check(matches(isSelectAccountMenu()));
    } finally {
      Espresso.unregisterIdlingResources(ir);
    }
  }

  @Test
  public void closeAddAccountWithBackButton() {
    openDrawerNavigation();

    final IdlingResource ir = getOpenDrawerIdlingResource();
    try {
      Espresso.registerIdlingResources(ir);
      onView(withId(R.id.nav_header_account)).check(matches(isDisplayed())).perform(click());
      onView(withId(R.id.nav_drawer)).check(matches(isSelectAccountMenu()));

      Espresso.pressBack();
      onView(withId(R.id.nav_drawer)).check(matches(isDefaultNavMenu()));
    } finally {
      Espresso.unregisterIdlingResources(ir);
    }
  }

  @Test
  public void clickAccountAndReopen_then_shownDefaultMenu() {
    openDrawerNavigation();

    final IdlingResource ir = getOpenDrawerIdlingResource();
    try {
      Espresso.registerIdlingResources(ir);
      onView(withId(R.id.nav_header_account)).check(matches(isDisplayed())).perform(click());
      onView(withId(R.id.nav_drawer)).check(matches(isSelectAccountMenu()));
    } finally {
      Espresso.unregisterIdlingResources(ir);
    }

    closeDrawerNavigation();

    final IdlingResource closeIR = getCloseDrawerIdlingResource();
    try {
      Espresso.registerIdlingResources(closeIR);
      onView(withId(R.id.nav_header_account)).check(matches(not(isDisplayed())));
    } finally {
      Espresso.unregisterIdlingResources(closeIR);
    }

    openDrawerNavigation();

    try {
      Espresso.registerIdlingResources(ir);
      onView(withId(R.id.nav_drawer)).check(matches(isDefaultNavMenu()));
    } finally {
      Espresso.unregisterIdlingResources(ir);
    }
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
      protected boolean matchesSafely(NavigationView item) {
        final Menu menu = item.getMenu();
        return checkMenuItemVisibility(menu, R.id.drawer_menu_home, true)
            && checkMenuItemVisibility(menu, R.id.drawer_menu_lists, true)
            && checkMenuItemVisibility(menu, R.id.drawer_menu_license, true)
            && checkMenuItemVisibility(menu, R.id.drawer_menu_add_account, false);
      }

      @Override
      public void describeTo(Description description) {}
    };
  }

  private static Matcher<View> isSelectAccountMenu() {
    return new BoundedMatcher<View, NavigationView>(NavigationView.class) {
      @Override
      protected boolean matchesSafely(NavigationView item) {
        final Menu menu = item.getMenu();
        return checkMenuItemVisibility(menu, R.id.drawer_menu_home, false)
            && checkMenuItemVisibility(menu, R.id.drawer_menu_lists, false)
            && checkMenuItemVisibility(menu, R.id.drawer_menu_license, false)
            && checkMenuItemVisibility(menu, R.id.drawer_menu_add_account, true);
      }

      @Override
      public void describeTo(Description description) {}
    };
  }

  private static boolean checkMenuItemVisibility(Menu menu, @IdRes int menuId, boolean visible) {
    final MenuItem item = menu.findItem(menuId);
    if (visible) {
      return item != null && item.isVisible();
    } else {
      return item == null || !item.isVisible();
    }
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
