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
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.rule.ActivityTestRule;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.datastore.SortedCache;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.util.StreamIdlingResource;
import com.freshdigitable.udonroad.util.StreamIdlingResource.Operation;
import com.freshdigitable.udonroad.util.TwitterResponseMock;
import com.freshdigitable.udonroad.util.UserUtil;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

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
import twitter4j.User;
import twitter4j.UserStreamListener;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.subscriber.ConfigSubscriber.TWITTER_API_CONFIG_DATE;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createResponseList;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatus;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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
  @Inject
  Twitter twitter;
  @Inject
  TypedCache<Status> statusCache;
  @Inject
  TypedCache<User> userCache;
  @Inject
  SortedCache<Status> homeTLStore;
  @Inject
  SortedCache<Status> userHomeTLStore;
  @Inject
  SortedCache<Status> userFavsTLStore;
  @Inject
  SortedCache<User> userFollowers;
  @Inject
  SortedCache<User> userFriends;
  @Inject
  ConfigStore configStore;
  @Inject
  SharedPreferences sprefs;

  protected MockMainApplication app;
  protected ResponseList<Status> responseList;
  private User loginUser;
  protected Matcher<View> screenNameMatcher;

  protected MockAppComponent getComponent() {
    return (MockAppComponent) app.getAppComponent();
  }

  @Before
  public void setup() throws Exception {
    app = (MockMainApplication) InstrumentationRegistry.getTargetContext().getApplicationContext();
    getComponent().inject(this);

    initStorage();
    loginUser = UserUtil.create();
    screenNameMatcher = withText("@" + loginUser.getScreenName());
    setupConfig(loginUser);

    final int initResListCount = setupTimeline();

    getRule().launchActivity(getIntent());
    final IdlingResource idlingResource = new IdlingResource() {
      @Override
      public String getName() {
        return "launch";
      }

      @Override
      public boolean isIdleNow() {
        if (getRecyclerView() == null) {
          return false;
        }
        if (getRecyclerView().getAdapter().getItemCount() < initResListCount) {
          return false;
        }
        if (callback != null) {
          callback.onTransitionToIdle();
        }
        return true;
      }

      private ResourceCallback callback;

      @Override
      public void registerIdleTransitionCallback(ResourceCallback callback) {
        this.callback = callback;
      }
    };
    Espresso.registerIdlingResources(idlingResource);
    verifyAfterLaunch();
    Espresso.unregisterIdlingResources(idlingResource);
  }

   private void initStorage() {
    clearCache(statusCache);
    clearCache(userCache);
    clearCache(homeTLStore, "home");
    clearCache(userHomeTLStore, "user_home");
    clearCache(userFavsTLStore, "user_fabs");
    clearCache(userFollowers, "user_followers");
    clearCache(userFriends, "user_friends");
    configStore.open();
    configStore.clear();
    configStore.close();
    sprefs.edit()
        .remove(TWITTER_API_CONFIG_DATE)
        .putString("token", "validtoken")
        .putString("token_secret", "validtokensecret")
        .apply();
  }

  protected void setupConfig(User loginUser) throws Exception {
    final TwitterAPIConfiguration twitterAPIConfigMock
        = TwitterResponseMock.createTwitterAPIConfigMock();
    when(twitter.getAPIConfiguration()).thenReturn(twitterAPIConfigMock);

    final long userId = loginUser.getId();
    when(twitter.getId()).thenReturn(userId);
    when(twitter.showUser(userId)).thenReturn(loginUser);
    when(twitter.verifyCredentials()).thenReturn(loginUser);

    final IDs ignoringUserIDsMock = mock(IDs.class);
    when(ignoringUserIDsMock.getIDs()).thenReturn(new long[0]);
    when(ignoringUserIDsMock.getNextCursor()).thenReturn(0L);
    when(ignoringUserIDsMock.getPreviousCursor()).thenReturn(0L);
    when(twitter.getBlocksIDs()).thenReturn(ignoringUserIDsMock);
    when(twitter.getBlocksIDs(anyLong())).thenReturn(ignoringUserIDsMock);
    when(twitter.getMutesIDs(anyLong())).thenReturn(ignoringUserIDsMock);
  }

  protected abstract int setupTimeline() throws TwitterException;

  protected void verifyAfterLaunch() throws Exception {
    verify(twitter, times(1)).getHomeTimeline();
    final UserStreamListener userStreamListener = app.getUserStreamListener();
    assertThat(userStreamListener, is(notNullValue()));
    onView(withId(R.id.timeline)).check(matches(isDisplayed()));
    onView(withId(R.id.main_send_tweet)).check(matches(not(isDisplayed())));
  }

  @After
  public void tearDown() throws Exception {
    unregisterStreamIdlingResource();
    reset(twitter);
    final AppCompatActivity activity = getRule().getActivity();
    if (activity != null) {
      activity.finish();
      Thread.sleep(800);
    }
  }

  protected abstract ActivityTestRule<? extends AppCompatActivity> getRule();

  protected Intent getIntent() {
    return new Intent();
  }

  private static void clearCache(TypedCache cache) {
    cache.open();
    cache.clear();
    cache.close();
  }

  private static void clearCache(SortedCache cache, String name) {
    cache.open(name);
    cache.clear();
    cache.close();
  }

  private StreamIdlingResource streamIdlingResource;

  private void registerStreamIdlingResource(StreamIdlingResource streamIdlingResource) {
    if (this.streamIdlingResource != null) {
      unregisterStreamIdlingResource();
    }
    Espresso.registerIdlingResources(streamIdlingResource);
    this.streamIdlingResource = streamIdlingResource;
  }

  private void unregisterStreamIdlingResource() {
    if (this.streamIdlingResource == null) {
      return;
    }
    Espresso.unregisterIdlingResources(this.streamIdlingResource);
    this.streamIdlingResource = null;
  }

  protected void receiveDeletionNotice(final Status... target) throws Exception {
    final RecyclerView recyclerView = getRecyclerView();
    StreamIdlingResource streamIdlingResource
        = new StreamIdlingResource(recyclerView, Operation.DELETE, target.length);
    registerStreamIdlingResource(streamIdlingResource);
    TwitterResponseMock.receiveDeletionNotice(app.getUserStreamListener(), target);
  }

  protected void receiveStatuses(final Status... statuses) throws Exception {
    receiveStatuses(true, statuses);
  }

  protected void receiveStatuses(boolean isIdlingResourceUsed, final Status... statuses) throws Exception {
    if (isIdlingResourceUsed) {
      final RecyclerView recyclerView = getRecyclerView();
      final StreamIdlingResource streamIdlingResource
          = new StreamIdlingResource(recyclerView, Operation.ADD, statuses.length);
      registerStreamIdlingResource(streamIdlingResource);
    }
    TwitterResponseMock.receiveStatuses(app.getUserStreamListener(), statuses);
  }

  private RecyclerView getRecyclerView() {
    return (RecyclerView) getRule().getActivity().findViewById(R.id.timeline);
  }

  protected Status findByStatusId(long statusId) throws Exception {
    for (Status s : responseList) {
      if (s.getId() == statusId) {
        return s;
      }
    }
    throw new TwitterException("status is not found. ID: " + statusId);
  }

  protected User getLoginUser() {
    return loginUser;
  }

  protected void setupCreateFavorite(final int rtCount, final int favCount) throws TwitterException {
    when(twitter.createFavorite(anyLong())).thenAnswer(new Answer<Status>() {
      @Override
      public Status answer(InvocationOnMock invocation) throws Throwable {
        final Long id = invocation.getArgumentAt(0, Long.class);
        final Status status = findByStatusId(id);
        when(status.getFavoriteCount()).thenReturn(favCount);
        when(status.getRetweetCount()).thenReturn(rtCount);
        when(status.isFavorited()).thenReturn(true);
        return status;
      }
    });
  }

  protected void setupRetweetStatus(final long rtStatusId) throws TwitterException {
    when(twitter.retweetStatus(anyLong())).thenAnswer(new Answer<Status>() {
      @Override
      public Status answer(InvocationOnMock invocation) throws Throwable {
        final Long id = invocation.getArgumentAt(0, Long.class);
        final Status rtedStatus = findByStatusId(id);
        receiveStatuses(true, TwitterResponseMock.createRtStatus(rtedStatus, rtStatusId, false));
        return TwitterResponseMock.createRtStatus(rtedStatus, rtStatusId, true);
      }
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

  protected int setupDefaultUserInfoTimeline() throws TwitterException {
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

    final Relationship relationship = mock(Relationship.class);
    when(relationship.isSourceFollowingTarget()).thenReturn(true);
    when(relationship.isSourceMutingTarget()).thenReturn(false);
    when(twitter.showFriendship(anyLong(), anyLong())).thenReturn(relationship);
    return responseList.size();
  }
}