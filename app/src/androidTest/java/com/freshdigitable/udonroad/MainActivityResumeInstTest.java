/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.inject.Inject;

import rx.Observable;
import rx.schedulers.Schedulers;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.UserStreamListener;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusViewAt;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createResponseList;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createRtStatus;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatus;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createText;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.receiveStatuses;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by akihit on 2016/07/01.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityResumeInstTest {
  @Inject
  TwitterApi twitterApi;
  @Inject
  Twitter twitter;

  @Rule
  public ActivityTestRule<MainActivity> rule
      = new ActivityTestRule<>(MainActivity.class, false, false);
  private MockMainApplication app;
  private long rtStatusId;

  @Before
  public void setup() throws Exception {
    app = (MockMainApplication) InstrumentationRegistry.getTargetContext().getApplicationContext();
    final MockTwitterApiComponent component = (MockTwitterApiComponent) app.getTwitterApiComponent();
    component.inject(this);

    final ResponseList<Status> responseList = createResponseList();
    for (int i = 1; i <= 20; i++) {
      final Status status = createStatus(i);
      responseList.add(status);
    }
    assertThat(responseList.size(), is(20));
    when(twitter.getHomeTimeline()).thenReturn(responseList);
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

    rule.launchActivity(new Intent());
    verify(twitter, times(1)).getHomeTimeline();
    final UserStreamListener userStreamListener = app.getUserStreamListener();
    assertThat(userStreamListener, is(notNullValue()));
    onView(withId(R.id.timeline)).check(matches(isDisplayed()));
  }

  @After
  public void tearDown() throws Exception {
    reset(twitter, twitterApi);
    final MainActivity activity = rule.getActivity();
    if (activity != null) {
      activity.finish();
      Thread.sleep(1000);
    }
  }

  @Test
  public void heading_then_latestTweetAppears() throws Exception {
    onView(ofStatusViewAt(R.id.timeline, 0)).perform(click());
    receiveStatuses(app.getUserStreamListener(),
        createStatus(21), createStatus(22));
    onView(withId(R.id.action_heading)).perform(click());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(createText(22)))));
    onView(withId(R.id.fab)).check(matches(not(isDisplayed())));
  }

  @Test
  public void headingAfterRelaunch_then_latestTweetAppears() throws Exception {
    onView(ofStatusViewAt(R.id.timeline, 0)).perform(click());
    receiveStatuses(app.getUserStreamListener(),
        createStatus(21), createStatus(22));
    onView(ofStatusViewAt(R.id.timeline, 0)).perform(click());

    Intent home = new Intent();
    home.setAction(Intent.ACTION_MAIN);
    home.addCategory(Intent.CATEGORY_HOME);
    home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    Intent relaunch = new Intent(rule.getActivity(), rule.getActivity().getClass());
    relaunch.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

    InstrumentationRegistry.getTargetContext().startActivity(home);
    Thread.sleep(500);
    rule.getActivity().startActivity(relaunch);

    onView(withId(R.id.action_heading)).perform(click());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(createText(22)))));
    onView(withId(R.id.fab)).check(matches(not(isDisplayed())));
  }

}
