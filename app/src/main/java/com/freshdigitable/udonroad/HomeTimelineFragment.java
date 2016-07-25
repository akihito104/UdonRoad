/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import rx.Observable;
import rx.Subscriber;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

/**
 * displays Authenticated user home timeline.
 *
 * Created by akihit on 2016/06/07.
 */
public class HomeTimelineFragment extends TimelineFragment {
  @SuppressWarnings("unused")
  private static final String TAG = HomeTimelineFragment.class.getSimpleName();

  @Override
  public String getStoreName() {
    return "home";
  }

  private UserStreamUtil userStream;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    userStream = new UserStreamUtil(super.timelineStore);
    InjectionUtil.getComponent(this).inject(userStream);
    return super.onCreateView(inflater, container, savedInstanceState);
  }

  @Override
  public void onStart() {
    super.onStart();
    userStream.connect();
  }

  @Override
  public void onStop() {
    Log.d(TAG, "onStop: ");
    super.onStop();
  }

  @Override
  public void onDestroyView() {
    Log.d(TAG, "onDestroyView: ");
    super.onDestroyView();
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "onDestroy: ");
    userStream.disconnect();
    super.onDestroy();
  }

  @Override
  protected void fetchTweet() {
    final Twitter twitter = getTwitterApi().getTwitter();
    fetchTweet(new Observable.OnSubscribe<List<Status>>() {
      @Override
      public void call(Subscriber<? super List<Status>> subscriber) {
        try {
          subscriber.onNext(twitter.getHomeTimeline());
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    });
  }

  @Override
  protected void fetchTweet(final Paging page) {
    final Twitter twitter = getTwitterApi().getTwitter();
    fetchTweet(new Observable.OnSubscribe<List<Status>>() {
          @Override
          public void call(Subscriber<? super List<Status>> subscriber) {
            try {
              subscriber.onNext(twitter.getHomeTimeline(page));
              subscriber.onCompleted();
            } catch (TwitterException e) {
              subscriber.onError(e);
            }
          }
        });
  }
}
