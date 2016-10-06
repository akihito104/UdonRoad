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
import android.support.test.rule.ActivityTestRule;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;

import com.freshdigitable.udonroad.datastore.SortedCache;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.util.StreamIdlingResource;
import com.freshdigitable.udonroad.util.StreamIdlingResource.Operation;
import com.freshdigitable.udonroad.util.TwitterResponseMock;
import com.freshdigitable.udonroad.util.UserUtil;

import org.junit.After;
import org.junit.Before;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import twitter4j.IDs;
import twitter4j.PagableResponseList;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterAPIConfiguration;
import twitter4j.User;
import twitter4j.UserStreamListener;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createResponseList;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createRtStatus;
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
 *
 * Created by akihit on 2016/07/03.
 */
public abstract class MainActivityInstTestBase {
  @Inject
  TwitterApi twitterApi;
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

  protected MockMainApplication app;
  protected long rtStatusId;

  @Before
  public void setup() throws Exception {
    app = (MockMainApplication) InstrumentationRegistry.getTargetContext().getApplicationContext();
    final MockAppComponent component = (MockAppComponent) app.getAppComponent();
    component.inject(this);

    clearCache(statusCache);
    clearCache(userCache);
    clearCache(homeTLStore, "home");
    clearCache(userHomeTLStore, "user_home");
    clearCache(userFavsTLStore, "user_fabs");
    clearCache(userFollowers, "user_followers");
    clearCache(userFriends, "user_friends");

    final List<Status> responseList = createResponseList();
    for (int i = 1; i <= 20; i++) {
      final Status status = createStatus(i);
      responseList.add(status);
    }
    assertThat(responseList.size(), is(20));
    when(twitterApi.getHomeTimeline())
        .thenReturn(Observable.just(responseList));
    when(twitterApi.getHomeTimeline(any(Paging.class)))
        .thenReturn(Observable.just(Collections.<Status>emptyList()));
    when(twitterApi.getUserTimeline(anyLong())).thenReturn(Observable.just(responseList));
    when(twitterApi.getUserTimeline(anyLong(), any(Paging.class)))
        .thenReturn(Observable.just(Collections.<Status>emptyList()));
    when(twitterApi.getFavorites(anyLong()))
        .thenReturn(Observable.just((List<Status>) createResponseList()));
    when(twitterApi.createFavorite(anyLong())).thenAnswer(new Answer<Observable<Status>>() {
      @Override
      public Observable<Status> answer(InvocationOnMock invocation) throws Throwable {
        final Long id = invocation.getArgumentAt(0, Long.class);
        final Status status = createStatus(id / 1000L);
        when(status.getFavoriteCount()).thenReturn(1);
        when(status.isFavorited()).thenReturn(true);
        return Observable.just(status)
            .subscribeOn(Schedulers.io());
      }
    });
    when(twitterApi.retweetStatus(anyLong())).thenAnswer(new Answer<Observable<Status>>() {
      @Override
      public Observable<Status> answer(InvocationOnMock invocation) throws Throwable {
        final Long id = invocation.getArgumentAt(0, Long.class);
        final long rtedStatusId = id / 1000L;
        rtStatusId = 100 + rtedStatusId;
        final Status rtStatus = createRtStatus(rtStatusId, rtedStatusId, true);
        return Observable.just(rtStatus)
            .subscribeOn(Schedulers.io());
      }
    });
    when(twitterApi.loadAccessToken()).thenReturn(true);
    when(twitterApi.getTwitter()).thenReturn(twitter);
    final User userMock = UserUtil.create();
    when(twitterApi.verifyCredentials()).thenReturn(Observable.just(userMock));
    final TwitterAPIConfiguration twitterAPIConfigMock = createTwitterAPIConfigMock();
    when(twitterApi.getTwitterAPIConfiguration()).thenReturn(
        Observable.create(new Observable.OnSubscribe<TwitterAPIConfiguration>() {
          @Override
          public void call(Subscriber<? super TwitterAPIConfiguration> subscriber) {
            subscriber.onNext(twitterAPIConfigMock);
            subscriber.onCompleted();
          }
        }));
    when(twitterApi.getFollowersList(anyLong(), anyLong()))
        .thenReturn(Observable.<PagableResponseList<User>>empty());
    when(twitterApi.getFriendsList(anyLong(), anyLong()))
        .thenReturn(Observable.<PagableResponseList<User>>empty());
    when(twitterApi.getId())
        .thenReturn(Observable.just(2000L));

    final IDs ignoringUserIDsMock = mock(IDs.class);
    when(ignoringUserIDsMock.getIDs())
        .thenReturn(new long[0]);
    when(ignoringUserIDsMock.getNextCursor())
        .thenReturn(0L);
    when(ignoringUserIDsMock.getPreviousCursor())
        .thenReturn(0L);
    when(twitterApi.getAllBlocksIDs())
        .thenReturn(Observable.just(ignoringUserIDsMock));
    when(twitterApi.getAllMutesIDs())
        .thenReturn(Observable.just(ignoringUserIDsMock));

    getRule().launchActivity(getIntent());
    verifyAfterLaunch();
  }

  protected void verifyAfterLaunch() {
    verify(twitterApi, times(1)).getHomeTimeline();
    final UserStreamListener userStreamListener = app.getUserStreamListener();
    assertThat(userStreamListener, is(notNullValue()));
    onView(withId(R.id.timeline)).check(matches(isDisplayed()));
    onView(withId(R.id.main_send_tweet)).check(matches(not(isDisplayed())));
  }

  @After
  public void tearDown() throws Exception {
    unregisterStreamIdlingResource();
    reset(twitter, twitterApi);
    final AppCompatActivity activity = getRule().getActivity();
    if (activity != null) {
      activity.finish();
      Thread.sleep(800);
    }
  }

  private static TwitterAPIConfiguration createTwitterAPIConfigMock() {
    final TwitterAPIConfiguration mock = mock(TwitterAPIConfiguration.class);
    when(mock.getShortURLLength()).thenReturn(23);
    when(mock.getShortURLLengthHttps()).thenReturn(23);
    return mock;
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
    final RecyclerView recyclerView = (RecyclerView) getRule().getActivity().findViewById(R.id.timeline);
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
      final RecyclerView recyclerView = (RecyclerView) getRule().getActivity().findViewById(R.id.timeline);
      final StreamIdlingResource streamIdlingResource
          = new StreamIdlingResource(recyclerView, Operation.ADD, statuses.length);
      registerStreamIdlingResource(streamIdlingResource);
    }
    TwitterResponseMock.receiveStatuses(app.getUserStreamListener(), statuses);
  }
}