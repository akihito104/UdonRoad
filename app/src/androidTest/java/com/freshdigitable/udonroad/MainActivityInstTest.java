/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.freshdigitable.udonroad.util.TwitterResponseMock;

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
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.action.ViewActions.swipeUp;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.assertion.ViewAssertions.selectedDescendantsMatch;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.freshdigitable.udonroad.util.StatusViewAssertion.recyclerViewDescendantsMatches;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusViewAt;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createResponseList;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createRtStatus;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatus;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createText;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by akihit on 2016/06/15.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityInstTest {
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
  public void receive2ReverseStatusIdOrderTweetsAtSameTime_and_displayStatusIdOrder() throws Exception {
    receiveStatuses(createStatus(25));
    onView(ofStatusView(withText(createText(25))))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 0));
    onView(ofStatusView(withText(createText(25))))
        .check(selectedDescendantsMatch(withId(R.id.tl_fav_icon), not(isDisplayed())));

    receiveStatuses(
        createStatus(29),
        createStatus(27));
    onView(ofStatusView(withText(createText(25))))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 2));
    onView(ofStatusView(withText(createText(29))))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 0));
    onView(ofStatusView(withText(createText(27))))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 1));
  }

  @Test
  public void receiveDelayed2ReverseStatusIdOrderTweetsAtSameTime_and_displayStatusIdOrder()
      throws Exception {
    receiveStatuses(createStatus(25));
    onView(ofStatusView(withText(createText(25))))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 0));

    receiveStatuses(
        createStatus(29),
        createStatus(23));
    onView(ofStatusView(withText(createText(25))))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 1));
    onView(ofStatusView(withText(createText(29))))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 0));
    onView(ofStatusView(withText(createText(23))))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 2));
  }

  @Test
  public void fetchFav_then_favIconAndCountAreDisplayed() throws Exception {
    onView(ofStatusView(withText(createText(20)))).perform(click());
    onView(withId(R.id.fab)).check(matches(isDisplayed()));
    onView(withId(R.id.fab)).perform(swipeUp());
    onView(ofStatusView(withText(createText(20))))
        .check(selectedDescendantsMatch(withId(R.id.tl_favcount), withText("1")));
    // TODO tint color check
  }

  @Test
  public void fetchRT_then_RtIconAndCountAreDisplayed() throws Exception {
    onView(ofStatusView(withText(createText(20)))).perform(click());
    onView(withId(R.id.fab)).check(matches(isDisplayed()));
    onView(withId(R.id.fab)).perform(swipeRight());
    receiveStatuses(createRtStatus(rtStatusId, 20, false));

    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(createText(20)))));
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(selectedDescendantsMatch(withId(R.id.tl_rtcount), withText("1")));
    onView(withId(R.id.timeline)).perform(swipeDown());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(selectedDescendantsMatch(withId(R.id.tl_rtcount), withText("1")));
    // TODO tint color check
  }

  @Test
  public void receiveStatusDeletionNoticeForLatestStatus_then_removedTheTopOfTimeline()
      throws Exception {
    final Status target = createStatus(20);
    receiveDeletionNotice(target);

    onView(ofStatusView(withText(createText(20)))).check(doesNotExist());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(createText(19)))));
  }

  @Test
  public void receiveStatusDeletionNoticeForRTStatus_then_removedRTStatus()
      throws Exception {
    onView(ofStatusView(withText(createText(20)))).perform(click());
    onView(withId(R.id.fab)).perform(swipeRight());
    final Status target = createRtStatus(rtStatusId, 20, false);
    receiveStatuses(target);
    onView(withId(R.id.timeline)).perform(swipeDown());
    receiveDeletionNotice(target);

    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(createText(20)))));
  }

  @Test
  public void receiveStatusDeletionNoticeForRTingStatus_then_removedOriginalAndRTedStatuses()
      throws Exception {
    onView(ofStatusView(withText(createText(20)))).perform(click());
    onView(withId(R.id.fab)).perform(swipeRight());
    final Status targetRt = createRtStatus(rtStatusId, 20, false);
    receiveStatuses(targetRt);
    final Status target = createStatus(20);
    receiveDeletionNotice(target, targetRt);

    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(createText(19)))));
    onView(ofStatusView(withText(createText(20)))).check(doesNotExist());
  }

  @Test
  public void receiveStatusDeletionNoticeForFavedStatus_then_removedOriginalStatuses()
      throws Exception {
    onView(ofStatusView(withText(createText(20)))).perform(click());
    onView(withId(R.id.fab)).perform(swipeUp());
    final Status target = createStatus(20);
    receiveDeletionNotice(target);

    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(createText(19)))));
    onView(ofStatusView(withText(createText(20)))).check(doesNotExist());
  }

  @Test
  public void receive2StatusDeletionNoticeAtSameTime_then_removed2Statuses()
      throws Exception {
    receiveDeletionNotice(createStatus(18), createStatus(20));

    onView(ofStatusView(withText(createText(20)))).check(doesNotExist());
    onView(ofStatusView(withText(createText(18)))).check(doesNotExist());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(createText(19)))));
  }

  @Test
  public void receiveStatusDeletionNoticeForSelectedStatus_then_removedSelectedStatus()
      throws Exception {
    onView(ofStatusViewAt(R.id.timeline, 0)).perform(click());
    receiveDeletionNotice(createStatus(20));

    onView(ofStatusView(withText(createText(20)))).check(doesNotExist());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText(createText(19)))));
  }

  private void receiveDeletionNotice(Status... target) throws InterruptedException {
    TwitterResponseMock.receiveDeletionNotice(app.getUserStreamListener(), target);
  }

  private void receiveStatuses(final Status... statuses) throws InterruptedException {
    TwitterResponseMock.receiveStatuses(app.getUserStreamListener(), statuses);
  }

}