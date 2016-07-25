/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.realmdata;

import java.util.List;

import rx.Observable;
import rx.Subscriber;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

/**
 * Created by akihit on 2016/06/07.
 */
public class RealmUserHomeTimelineFragment extends RealmTimelineFragment {
  @SuppressWarnings("unused")
  private static final String TAG = RealmUserHomeTimelineFragment.class.getSimpleName();

  @Override
  public String getStoreName() {
    return "user_home";
  }

  @Override
  protected void fetchTweet() {
    final long userId = getUserId();
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
    final long userId = getUserId();
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

  public static RealmUserHomeTimelineFragment getInstance(long userId) {
    return getInstance(new RealmUserHomeTimelineFragment(), userId);
  }
}
