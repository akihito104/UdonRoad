/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.realmdata;

import android.support.annotation.NonNull;

import java.util.List;

import io.realm.RealmConfiguration;
import rx.Observable;
import rx.Subscriber;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

/**
 * Created by akihit on 2016/06/07.
 */
public class RealmUserHomeTimelineFragment extends RealmTimelineFragment {
  @SuppressWarnings("unused")
  private static final String TAG = RealmUserHomeTimelineFragment.class.getSimpleName();

  @Override
  public RealmConfiguration createRealmConfiguration() {
    return new RealmConfiguration.Builder(getContext())
        .name("user_home")
        .build();
  }

  @Override
  protected void fetchTweet() {
    final long userId = getUser().getId();
    final Twitter twitter = getTwitterApi().getTwitter();
    fetchTweet(new Observable.OnSubscribe<List<Status>>() {
      @Override
      public void call(Subscriber<? super List<Status>> subscriber) {
        try {
          subscriber.onNext(twitter.getUserTimeline(userId));
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    });
  }

  @Override
  protected void fetchTweet(final Paging page) {
    final long userId = getUser().getId();
    final Twitter twitter = getTwitterApi().getTwitter();
    fetchTweet(new Observable.OnSubscribe<List<Status>>() {
      @Override
      public void call(Subscriber<? super List<Status>> subscriber) {
        try {
          subscriber.onNext(twitter.getUserTimeline(userId, page));
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    });
  }

  @NonNull
  @Override
  protected User getUser() {
    final User user = super.getUser();
    if (user == null) {
      throw new RuntimeException();
    }
    return user;
  }

  public static RealmUserHomeTimelineFragment getInstance(User user) {
    return getInstance(new RealmUserHomeTimelineFragment(), user);
  }
}
