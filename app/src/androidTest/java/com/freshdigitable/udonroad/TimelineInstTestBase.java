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

import android.app.Activity;
import android.content.Intent;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.IdlingResource;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.util.IdlingResourceUtil;
import com.freshdigitable.udonroad.util.StorageUtil;
import com.freshdigitable.udonroad.util.TestInjectionUtil;
import com.freshdigitable.udonroad.util.TwitterResponseMock;
import com.freshdigitable.udonroad.util.UserUtil;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javax.inject.Inject;

import twitter4j.IDs;
import twitter4j.PagableResponseList;
import twitter4j.Paging;
import twitter4j.Relationship;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterAPIConfiguration;
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
import static com.freshdigitable.udonroad.util.AssertionUtil.anywayNotVisible;
import static com.freshdigitable.udonroad.util.IdlingResourceUtil.getSimpleIdlingResource;
import static com.freshdigitable.udonroad.util.IdlingResourceUtil.runWithIdlingResource;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofQuotedStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofRTStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusView;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createResponseList;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createRtStatus;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatus;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MainActivityTestBase provides setup and tearDown method for tests of MainActivity.
 * <p>
 * Created by akihit on 2016/07/03.
 */
public abstract class TimelineInstTestBase {
  protected ResponseList<Status> responseList;
  private User loginUser;
  protected Matcher<View> screenNameMatcher;

  private static MockMainApplication getApp() {
    return (MockMainApplication) InstrumentationRegistry.getTargetContext().getApplicationContext();
  }

  @Before
  public void setup() throws Exception {
    TestInjectionUtil.getComponent().inject(this);

    StorageUtil.initStorage();
    loginUser = UserUtil.createUserA();
    screenNameMatcher = withText("@" + loginUser.getScreenName());
    setupConfig(loginUser);

    final int initResListCount = setupTimeline();

    getRule().launchActivity(getIntent());
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
  IdlingResource getTimelineIdlingResource(String name, int initResListCount) {
    return IdlingResourceUtil.getSimpleIdlingResource(name,
        () -> getTimelineView() != null
            && getTimelineView().getAdapter().getItemCount() >= initResListCount);
  }


  @Inject
  Twitter twitter;
  @Inject
  TwitterStream twitterStream;
  @Inject
  AppSettingStore appSettings;

  protected void setupConfig(User loginUser) throws Exception {
    final TwitterAPIConfiguration twitterAPIConfigMock
        = TwitterResponseMock.createTwitterAPIConfigMock();
    when(twitterAPIConfigMock.getShortURLLength()).thenReturn(23);
    when(twitterAPIConfigMock.getShortURLLengthHttps()).thenReturn(23);
    when(twitter.getAPIConfiguration()).thenReturn(twitterAPIConfigMock);

    final long userId = loginUser.getId();
    when(twitter.getId()).thenReturn(userId);
    when(twitter.showUser(userId)).thenReturn(loginUser);
    when(twitter.verifyCredentials()).thenReturn(loginUser);
    when(twitterStream.getId()).thenReturn(userId);

    setupIgnoringUsers();

    appSettings.open();
    appSettings.storeAccessToken(new AccessToken(userId + "-validToken", "validSecret"));
    appSettings.setCurrentUserId(userId);
    appSettings.close();
  }

  void setupIgnoringUsers() throws TwitterException {
    final IDs ignoringUserIDsMock = mock(IDs.class);
    when(ignoringUserIDsMock.getIDs()).thenReturn(new long[0]);
    when(ignoringUserIDsMock.getNextCursor()).thenReturn(0L);
    when(ignoringUserIDsMock.getPreviousCursor()).thenReturn(0L);
    when(ignoringUserIDsMock.hasNext()).thenReturn(false);
    when(twitter.getBlocksIDs()).thenReturn(ignoringUserIDsMock);
    when(twitter.getBlocksIDs(anyLong())).thenReturn(ignoringUserIDsMock);
    when(twitter.getMutesIDs(anyLong())).thenReturn(ignoringUserIDsMock);
  }

  protected abstract int setupTimeline() throws TwitterException;

  protected void verifyAfterLaunch() throws Exception {
    onView(withId(R.id.timeline)).check(matches(isDisplayed()));
    onView(withId(R.id.action_sendTweet)).check(doesNotExist());
    verify(twitter, times(1)).getHomeTimeline();
    verify(twitter, times(1)).setOAuthAccessToken(any(AccessToken.class));
    verify(twitterStream, times(1)).setOAuthAccessToken(any(AccessToken.class));
    assertThat(getUserStreamListener(), is(notNullValue()));
  }

  private UserStreamListener getUserStreamListener() {
    return getApp().getUserStreamListener();
  }

  @After
  public void tearDown() throws Exception {
    reset(twitter);
    reset(twitterStream);
    final Activity activity = getActivity();
    if (activity != null) {
      IdlingResourceUtil.ActivityWaiter.create(activity).waitForDestroyed();
      StorageUtil.checkAllRealmInstanceClosed();
    }
  }

  protected abstract ActivityTestRule<? extends AppCompatActivity> getRule();

  private Activity getActivity() {
    final Callable<Activity> resumedActivity = () -> {
      final Collection<Activity> resumedActivities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
      return resumedActivities.isEmpty() ? null
          : resumedActivities.iterator().next();
    };
    try {
      if (Looper.getMainLooper().isCurrentThread()) {
        return resumedActivity.call();
      } else {
        final FutureTask<Activity> activityFutureTask = new FutureTask<>(resumedActivity);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(activityFutureTask);
        return activityFutureTask.get();
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

  protected Intent getIntent() {
    return new Intent();
  }

  protected void receiveDeletionNotice(final Status... target) {
    final RecyclerView timeline = getTimelineView();
    final int expectedCount = timeline.getAdapter().getItemCount() - target.length;

    TwitterResponseMock.receiveDeletionNotice(getUserStreamListener(), target);

    runWithIdlingResource(getSimpleIdlingResource("receive deletion notice", () ->
        timeline.getAdapter().getItemCount() == expectedCount), () -> {
      for (Status t : target) {
        if (t.isRetweet()) {
          onView(ofRTStatusView(t)).check(anywayNotVisible());
        } else {
          onView(ofStatusView(t)).check(anywayNotVisible());
          onView(ofQuotedStatusView(t)).check(anywayNotVisible());
        }
      }
    });
  }


  protected void receiveStatuses(final Status... statuses) throws Exception {
    receiveStatuses(true, statuses);
  }

  protected void receiveStatuses(boolean isIdlingResourceUsed, final Status... statuses) {
    if (!isIdlingResourceUsed) {
      TwitterResponseMock.receiveStatuses(getUserStreamListener(), statuses);
      return;
    }
    final RecyclerView recyclerView = getTimelineView();
    final int expectedCount = (recyclerView != null ? recyclerView.getAdapter().getItemCount() : 0)
        + statuses.length;
    receiveStatuses(expectedCount, statuses);
  }

  void receiveStatuses(int expectedCount, final Status... statuses) {
    TwitterResponseMock.receiveStatuses(getUserStreamListener(), statuses);
    runWithIdlingResource(getSimpleIdlingResource("receive status", () -> {
      final RecyclerView rv = getTimelineView();
      return rv != null && rv.getAdapter().getItemCount() == expectedCount;
    }), () ->
        onView(withId(R.id.timeline)).check(matches(isDisplayed())));
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

  protected Status findByStatusId(long statusId) throws Exception {
    for (Status s : responseList) {
      if (s.getId() == statusId) {
        return s;
      }
      if (s.getQuotedStatusId() == statusId) {
        return s.getQuotedStatus();
      }
    }
    throw new TwitterException("status is not found. ID: " + statusId);
  }

  protected User getLoginUser() {
    return loginUser;
  }

  protected void setupCreateFavorite(final int rtCount, final int favCount) throws TwitterException {
    when(twitter.createFavorite(anyLong())).thenAnswer(invocation -> {
      final Long id = invocation.getArgument(0);
      final Status status = findByStatusId(id);
      when(status.getFavoriteCount()).thenReturn(favCount);
      when(status.getRetweetCount()).thenReturn(rtCount);
      when(status.isFavorited()).thenReturn(true);
      return status;
    });
  }

  protected void setupRetweetStatus(final long rtStatusId, final int rtCount, final int favCount)
      throws TwitterException {
    when(twitter.retweetStatus(anyLong())).thenAnswer(invocation -> {
      final Long id = invocation.getArgument(0);
      final Status rtedStatus = findByStatusId(id);
      TwitterResponseMock.receiveStatuses(getUserStreamListener(),
          createRtStatus(rtedStatus, rtStatusId, false));
      return createRtStatus(rtedStatus, rtStatusId, rtCount, favCount, true);
    });
  }

  @NonNull
  protected static List<Status> createDefaultStatuses(User user) {
    List<Status> resLi = new ArrayList<>();
    for (int i = 1; i <= 20; i++) {
      final Status status = createStatus(i * 1000L, user);
      resLi.add(status);
    }
    return resLi;
  }

  @NonNull
  protected static ResponseList<Status> createDefaultResponseList(User user) {
    final List<Status> defaultStatuses = createDefaultStatuses(user);
    return createResponseList(defaultStatuses);
  }

  protected int setupDefaultTimeline() throws TwitterException {
    final ResponseList<Status> responseList = createDefaultResponseList(loginUser);
    this.responseList = responseList;
    when(twitter.getHomeTimeline()).thenReturn(responseList);
    when(twitter.getHomeTimeline(any(Paging.class))).thenReturn(createResponseList());
    return responseList.size();
  }

  protected int setupUserInfoTimeline(Relationship relationship) throws TwitterException {
    if (!relationship.isSourceFollowingTarget() && relationship.isSourceWantRetweets()) {
      throw new IllegalArgumentException("follower only can receive RT.");
    }
    final User loginUser = getLoginUser();
    final ResponseList<Status> responseList = createDefaultResponseList(loginUser);
    this.responseList = responseList;
    final ResponseList<Status> emptyStatusResponseList = createResponseList();

    final int size = responseList.size();
    when(loginUser.getStatusesCount()).thenReturn(size);
    when(twitter.getUserTimeline(loginUser.getId())).thenReturn(responseList);
    when(twitter.getUserTimeline(anyLong(), any(Paging.class))).thenReturn(emptyStatusResponseList);
    when(twitter.getFavorites(anyLong())).thenReturn(emptyStatusResponseList);
    final PagableResponseList<User> emptyUserPagableResponseList = mock(PagableResponseList.class);
    when(twitter.getFollowersList(anyLong(), anyLong())).thenReturn(emptyUserPagableResponseList);
    when(twitter.getFriendsList(anyLong(), anyLong())).thenReturn(emptyUserPagableResponseList);

    when(twitter.showFriendship(anyLong(), anyLong())).thenReturn(relationship);
    return responseList.size();
  }
}