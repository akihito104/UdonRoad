/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.inject.Inject;

import twitter4j.RateLimitStatus;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.User;
import twitter4j.UserStreamListener;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
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
    when(twitterApi.getTwitter()).thenReturn(twitter);
  }

  @Test
  public void tests() throws Exception {
    rule.launchActivity(new Intent());
    verify(twitter, times(1)).getHomeTimeline();
    final UserStreamListener userStreamListener = app.getUserStreamListener();
    assertThat(userStreamListener, is(notNullValue()));
    userStreamListener.onStatus(createStatus(25));
    Thread.sleep(500);
    userStreamListener.onStatus(createStatus(29));
    userStreamListener.onStatus(createStatus(27));
    Thread.sleep(500); // buffering tweets in 500ms
    onView(withId(R.id.timeline)).check(matches(isDisplayed()));
    // TODO position of each view
    onView(withText("tweet body 25")).check(matches(isDisplayed()));
    onView(withText("tweet body 29")).check(matches(isDisplayed()));
    onView(withText("tweet body 27")).check(matches(isDisplayed()));
  }

  private static Status createStatus(long id) {
    final Status status = mock(Status.class);
    when(status.getId()).thenReturn(id * 1000L);
    when(status.getCreatedAt()).thenReturn(new Date());
    when(status.getText()).thenReturn("tweet body " + id);
    when(status.isRetweet()).thenReturn(false);
    when(status.getSource()).thenReturn("Udonroad"); // not set
    final User user = mock(User.class);
    when(user.getId()).thenReturn(2000L);
    when(user.getScreenName()).thenReturn("akihito matsuda");
    when(user.getName()).thenReturn("akihito104");
    when(status.getUser()).thenReturn(user);
    return status;
  }

  @NonNull
  protected ResponseList<Status> createResponseList() {
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