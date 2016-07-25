/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.inject.Inject;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import rx.Observable;
import rx.schedulers.Schedulers;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.UserStreamListener;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createResponseList;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createRtStatus;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatus;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by akihit on 2016/07/03.
 */
public abstract class MainActivityInstTestBase {
  @Inject
  TwitterApi twitterApi;
  @Inject
  Twitter twitter;

  @Rule
  public ActivityTestRule<MainActivity> rule
      = new ActivityTestRule<>(MainActivity.class, false, false);
  protected MockMainApplication app;
  protected long rtStatusId;

  @Before
  public void setup() throws Exception {
    app = (MockMainApplication) InstrumentationRegistry.getTargetContext().getApplicationContext();
    final MockAppComponent component = (MockAppComponent) app.getAppComponent();
    component.inject(this);

    Realm.deleteRealm(new RealmConfiguration.Builder(app.getApplicationContext()).name("home").build());
    Realm.deleteRealm(new RealmConfiguration.Builder(app.getApplicationContext()).name("cache").build());

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
}