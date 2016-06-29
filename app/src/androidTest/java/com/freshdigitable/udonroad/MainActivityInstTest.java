/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.content.Intent;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.core.deps.guava.base.Predicate;
import android.support.test.espresso.core.deps.guava.collect.Iterables;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.annotation.Nullable;
import javax.inject.Inject;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import twitter4j.ExtendedMediaEntity;
import twitter4j.RateLimitStatus;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.Twitter;
import twitter4j.URLEntity;
import twitter4j.User;
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
import static android.support.test.espresso.util.TreeIterables.breadthFirstViewTraversal;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
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
    onView(ofStatusView(withText("tweet body 25")))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 0));
    onView(ofStatusView(withText("tweet body 25")))
        .check(selectedDescendantsMatch(withId(R.id.tl_fav_icon), not(isDisplayed())));

    receiveStatuses(
        createStatus(29),
        createStatus(27));
    onView(ofStatusView(withText("tweet body 25")))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 2));
    onView(ofStatusView(withText("tweet body 29")))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 0));
    onView(ofStatusView(withText("tweet body 27")))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 1));
  }

  @Test
  public void receiveDelayed2ReverseStatusIdOrderTweetsAtSameTime_and_displayStatusIdOrder()
      throws Exception {
    receiveStatuses(createStatus(25));
    onView(ofStatusView(withText("tweet body 25")))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 0));

    receiveStatuses(
        createStatus(29),
        createStatus(23));
    onView(ofStatusView(withText("tweet body 25")))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 1));
    onView(ofStatusView(withText("tweet body 29")))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 0));
    onView(ofStatusView(withText("tweet body 23")))
        .check(recyclerViewDescendantsMatches(R.id.timeline, 2));
  }

  @Test
  public void fetchFav_then_favIconAndCountAreDisplayed() throws Exception {
    onView(ofStatusView(withText("tweet body 20"))).perform(click());
    onView(withId(R.id.fab)).check(matches(isDisplayed()));
    onView(withId(R.id.fab)).perform(swipeUp());
    onView(ofStatusView(withText("tweet body 20")))
        .check(selectedDescendantsMatch(withId(R.id.tl_favcount), withText("1")));
    // TODO tint color check
  }

  @Test
  public void fetchRT_then_RtIconAndCountAreDisplayed() throws Exception {
    onView(ofStatusView(withText("tweet body 20"))).perform(click());
    onView(withId(R.id.fab)).check(matches(isDisplayed()));
    onView(withId(R.id.fab)).perform(swipeRight());
    receiveStatuses(createRtStatus(rtStatusId, 20, false));

    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText("tweet body 20"))));
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(selectedDescendantsMatch(withId(R.id.tl_rtcount), withText("1")));
    onView(withId(R.id.timeline)).perform(swipeDown());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(selectedDescendantsMatch(withId(R.id.tl_rtcount), withText("1")));
    // TODO tint color check
  }

  @Test
  public void receiveDeletedStatusEventForLatestStatus_then_removedTheTopOfTimeline()
      throws Exception {
    final Status target = createStatus(20);
    receiveDeletionNotice(target);

    onView(ofStatusView(withText("tweet body 20"))).check(doesNotExist());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText("tweet body 19"))));
  }

  @Test
  public void receiveDeletedStatusEventForRTStatus_then_removedRTStatus()
      throws Exception {
    onView(ofStatusView(withText("tweet body 20"))).perform(click());
    onView(withId(R.id.fab)).perform(swipeRight());
    final Status target = createRtStatus(rtStatusId, 20, false);
    receiveStatuses(target);
    onView(withId(R.id.timeline)).perform(swipeDown());
    receiveDeletionNotice(target);

    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText("tweet body 20"))));
  }

  @Test
  public void receiveDeletedStatusEventForRTingStatus_then_removedOriginalAndRTedStatuses()
      throws Exception {
    onView(ofStatusView(withText("tweet body 20"))).perform(click());
    onView(withId(R.id.fab)).perform(swipeRight());
    final Status targetRt = createRtStatus(rtStatusId, 20, false);
    receiveStatuses(targetRt);
    final Status target = createStatus(20);
    receiveDeletionNotice(target, targetRt);

    onView(ofStatusView(withText("tweet body 20"))).check(doesNotExist());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText("tweet body 19"))));
  }

  @Test
  public void receiveDeletedStatusEventForFavedStatus_then_removedOriginalStatuses()
      throws Exception {
    onView(ofStatusView(withText("tweet body 20"))).perform(click());
    onView(withId(R.id.fab)).perform(swipeUp());
    final Status target = createStatus(20);
    receiveDeletionNotice(target);

    onView(ofStatusView(withText("tweet body 20"))).check(doesNotExist());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText("tweet body 19"))));
  }

  @Test
  public void receive2DeletedStatusEventsAtSameTime_then_removed2Statuses()
      throws Exception {
    receiveDeletionNotice(createStatus(19), createStatus(20));

    onView(ofStatusView(withText("tweet body 20"))).check(doesNotExist());
    onView(ofStatusView(withText("tweet body 19"))).check(doesNotExist());
    onView(ofStatusViewAt(R.id.timeline, 0))
        .check(matches(ofStatusView(withText("tweet body 18"))));
  }

  protected void receiveDeletionNotice(Status... target) throws InterruptedException {
    Observable.just(Arrays.asList(target))
        .flatMapIterable(new Func1<List<Status>, Iterable<Status>>() {
          @Override
          public Iterable<Status> call(List<Status> statuses) {
            return statuses;
          }
        })
        .map(new Func1<Status, StatusDeletionNotice>() {
          @Override
          public StatusDeletionNotice call(Status status) {
            return createDeletionNotice(status);
          }
        })
        .observeOn(Schedulers.io())
        .subscribe(new Action1<StatusDeletionNotice>() {
          @Override
          public void call(StatusDeletionNotice statusDeletionNotice) {
            app.getUserStreamListener().onDeletionNotice(statusDeletionNotice);
          }
        });
    Thread.sleep(500);
  }

  @NonNull
  protected StatusDeletionNotice createDeletionNotice(final Status target) {
    return new StatusDeletionNotice() {
      @Override
      public long getStatusId() {
        return target.getId();
      }

      @Override
      public long getUserId() {
        return target.getUser().getId();
      }

      @Override
      public int compareTo(@NonNull StatusDeletionNotice statusDeletionNotice) {
        return 0;
      }
    };
  }

  private void receiveStatuses(final Status... statuses) throws InterruptedException {
    Observable.just(Arrays.asList(statuses))
        .flatMapIterable(new Func1<List<Status>, Iterable<Status>>() {
          @Override
          public Iterable<Status> call(List<Status> statuses) {
            return statuses;
          }
        })
        .observeOn(Schedulers.io())
        .subscribe(new Action1<Status>() {
          @Override
          public void call(Status status) {
            app.getUserStreamListener().onStatus(status);
          }
        });
    Thread.sleep(600); // buffering tweets in 500ms
  }

  @NonNull
  private Matcher<View> ofStatusView(final Matcher<View> viewMatcher) {
    return new BoundedMatcher<View, StatusView>(StatusView.class) {
      @Override
      protected boolean matchesSafely(StatusView item) {
        final Iterable<View> it = Iterables.filter(breadthFirstViewTraversal(item),
            new Predicate<View>() {
              @Override
              public boolean apply(@Nullable View view) {
                return view != null
                    && viewMatcher.matches(view);
              }
            });
        return it.iterator().hasNext();
      }

      @Override
      public void describeTo(Description description) {
        viewMatcher.describeTo(description);
      }
    };
  }

  private Matcher<View> ofStatusViewAt(@IdRes final int recyclerViewId, final int position) {
    final Matcher<View> recyclerViewMatcher = withId(recyclerViewId);
    return new BoundedMatcher<View, StatusView>(StatusView.class) {
      @Override
      public void describeTo(Description description) {
      }

      @Override
      protected boolean matchesSafely(StatusView item) {
        final RecyclerView recyclerView = (RecyclerView) item.getParent();
        if (!recyclerViewMatcher.matches(recyclerView)) {
          return false;
        }
        final View target = recyclerView.getChildAt(position);
        return target == item;
      }
    };
  }

  private ViewAssertion recyclerViewDescendantsMatches(
      @IdRes final int recyclerViewId, final int position) {
    return new ViewAssertion() {
      @Override
      public void check(View view, NoMatchingViewException noViewFoundException) {
        if (!(view instanceof StatusView)) {
          throw noViewFoundException;
        }
        final RecyclerView recyclerView = (RecyclerView) view.getParent();
        if (recyclerView == null
            || recyclerView.getId() != recyclerViewId) {
          throw noViewFoundException;
        }
        final View actual = recyclerView.getChildAt(position);
        if (view != actual) {
          throw noViewFoundException;
        }
      }
    };
  }

  private static Status createStatus(long id) {
    final Status status = mock(Status.class);
    when(status.getId()).thenReturn(id * 1000L);
    when(status.getCreatedAt()).thenReturn(new Date());
    when(status.getText()).thenReturn("tweet body " + id);
    when(status.isRetweet()).thenReturn(false);
    when(status.getSource())
        .thenReturn("<a href=\"https://twitter.com/akihito104\">Udonroad</a>");
    when(status.getURLEntities()).thenReturn(new URLEntity[0]);
    when(status.getExtendedMediaEntities()).thenReturn(new ExtendedMediaEntity[0]);
    final User user = mock(User.class);
    when(user.getId()).thenReturn(2000L);
    when(user.getName()).thenReturn("akihito matsuda");
    when(user.getScreenName()).thenReturn("akihito104");
    when(status.getUser()).thenReturn(user);
    return status;
  }

  private static Status createRtStatus(long newStatusId, long rtedStatusId, boolean isFromApi) {
    final Status rtStatus = createStatus(rtedStatusId);
    when(rtStatus.isRetweeted()).thenReturn(isFromApi);
    final int retweetCount = rtStatus.getRetweetCount();
    when(rtStatus.getRetweetCount()).thenReturn(retweetCount + 1);

    final Status status = createStatus(newStatusId);
    final String rtText = rtStatus.getText();
    when(status.getText()).thenReturn(rtText);
    when(status.isRetweet()).thenReturn(true);
    when(status.isRetweeted()).thenReturn(isFromApi);
    when(status.getRetweetedStatus()).thenReturn(rtStatus);
    return status;
  }

  @NonNull
  private ResponseList<Status> createResponseList() {
    return new ResponseList<Status>() {
      List<Status> list = new ArrayList<>();

      @Override
      public RateLimitStatus getRateLimitStatus() {
        return null;
      }

      @Override
      public void add(int location, Status object) {
        list.add(location, object);
      }

      @Override
      public boolean add(Status object) {
        return list.add(object);
      }

      @Override
      public boolean addAll(int location, @NonNull Collection<? extends Status> collection) {
        return list.addAll(location, collection);
      }

      @Override
      public boolean addAll(@NonNull Collection<? extends Status> collection) {
        return list.addAll(collection);
      }

      @Override
      public void clear() {
        list.clear();
      }

      @Override
      public boolean contains(Object object) {
        return list.contains(object);
      }

      @Override
      public boolean containsAll(@NonNull Collection<?> collection) {
        return list.containsAll(collection);
      }

      @Override
      public Status get(int location) {
        return list.get(location);
      }

      @Override
      public int indexOf(Object object) {
        return list.indexOf(object);
      }

      @Override
      public boolean isEmpty() {
        return list.isEmpty();
      }

      @NonNull
      @Override
      public Iterator<Status> iterator() {
        return list.iterator();
      }

      @Override
      public int lastIndexOf(Object object) {
        return list.lastIndexOf(object);
      }

      @Override
      public ListIterator<Status> listIterator() {
        return list.listIterator();
      }

      @NonNull
      @Override
      public ListIterator<Status> listIterator(int location) {
        return list.listIterator(location);
      }

      @Override
      public Status remove(int location) {
        return list.remove(location);
      }

      @Override
      public boolean remove(Object object) {
        return list.remove(object);
      }

      @Override
      public boolean removeAll(@NonNull Collection<?> collection) {
        return list.removeAll(collection);
      }

      @Override
      public boolean retainAll(@NonNull Collection<?> collection) {
        return list.retainAll(collection);
      }

      @Override
      public Status set(int location, Status object) {
        return list.set(location, object);
      }

      @Override
      public int size() {
        return list.size();
      }

      @NonNull
      @Override
      public List<Status> subList(int start, int end) {
        return list.subList(start, end);
      }

      @NonNull
      @Override
      public Object[] toArray() {
        return list.toArray();
      }

      @NonNull
      @Override
      public <T> T[] toArray(@NonNull T[] array) {
        return list.toArray(array);
      }

      @Override
      public int getAccessLevel() {
        return 0;
      }
    };
  }
}