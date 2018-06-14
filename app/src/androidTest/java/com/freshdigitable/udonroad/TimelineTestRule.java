/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
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
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.IdlingResource;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.util.IdlingResourceUtil;
import com.freshdigitable.udonroad.util.StorageUtil;
import com.freshdigitable.udonroad.util.UserUtil;

import org.hamcrest.Matcher;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.User;
import twitter4j.UserStreamListener;
import twitter4j.auth.AccessToken;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.MockMainApplication.getApp;
import static com.freshdigitable.udonroad.util.IdlingResourceUtil.runWithIdlingResource;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public abstract class TimelineTestRule<T extends Activity> extends ActivityTestRule<T> {
  private User loginUser;
  protected Matcher<View> screenNameMatcher;
  final AppSettingStore appSettings = getApp().sharedPreferenceModule.appSettingStore;
  private int initResListCount;

  public TimelineTestRule(Class<T> activityClass, boolean initialTouchMode, boolean launchActivity) {
    super(activityClass, initialTouchMode, launchActivity);
  }

  @Override
  protected void beforeActivityLaunched() {
    super.beforeActivityLaunched();
    StorageUtil.initStorage();

    loginUser = getLoginUser();
    screenNameMatcher = withText("@" + loginUser.getScreenName());
    try {
      setupConfig(loginUser);
      initResListCount = setupTimeline();
    } catch (TwitterException e) {
      throw new RuntimeException(e);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected User getLoginUser() {
    return UserUtil.createUserA();
  }

  protected void setupConfig(User loginUser) {
    final long userId = loginUser.getId();
    appSettings.open();
    appSettings.clear();
    appSettings.storeAccessToken(new AccessToken(userId + "-validToken", "validSecret"));
    appSettings.setCurrentUserId(userId);
    appSettings.close();
  }

  protected abstract int setupTimeline() throws TwitterException;

  @Override
  protected void afterActivityLaunched() {
    super.afterActivityLaunched();
    runWithIdlingResource(
        getTimelineIdlingResource("launch", initResListCount), () -> {
          try {
            verifyAfterLaunch();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @NonNull
  private IdlingResource getTimelineIdlingResource(String name, int initResListCount) {
    return IdlingResourceUtil.getSimpleIdlingResource(name,
        () -> getTimelineView() != null
            && getTimelineView().getAdapter().getItemCount() >= initResListCount);
  }

  protected void verifyAfterLaunch() throws Exception {
    onView(withId(R.id.timeline)).check(matches(isDisplayed()));
    onView(withId(R.id.action_sendTweet)).check(doesNotExist());
    final Twitter twitter = getApp().twitterApiModule.twitter;
    final TwitterStream twitterStream = getApp().twitterApiModule.twitterStream;
    final UserStreamListener userStreamListener = getApp().getUserStreamListener();
    verify(twitter, times(1)).getHomeTimeline();
    verify(twitter, times(1)).setOAuthAccessToken(any(AccessToken.class));
    verify(twitterStream, atLeast(1)).setOAuthAccessToken(any(AccessToken.class));
    assertThat(userStreamListener, is(notNullValue()));
  }

  protected RecyclerView getTimelineView() {
    final Callable<RecyclerView> findView = () -> {
      final Collection<Activity> resumedActivity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
      for (Activity a : resumedActivity) {
        final RecyclerView t = a.findViewById(R.id.timeline);
        if (t != null) {
          return t;
        }
      }
      return null;
    };
    try {
      if (Looper.getMainLooper().isCurrentThread()) {
        return findView.call();
      } else {
        final FutureTask<RecyclerView> findViewFuture = new FutureTask<>(findView);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(findViewFuture);
        return findViewFuture.get();
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}
