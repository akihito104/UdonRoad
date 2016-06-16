/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.realmdata;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.freshdigitable.udonroad.MainApplication;
import com.freshdigitable.udonroad.UserStreamUtil;

import java.util.List;

import io.realm.RealmConfiguration;
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
public class RealmHomeTimelineFragment extends RealmTimelineFragment {
  @SuppressWarnings("unused")
  private static final String TAG = RealmHomeTimelineFragment.class.getSimpleName();

  @Override
  public RealmConfiguration createRealmConfiguration() {
    return new RealmConfiguration.Builder(getContext())
        .name("home")
        .build();
  }

  private UserStreamUtil userStream;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final MainApplication application = (MainApplication) getActivity().getApplication();
    userStream = new UserStreamUtil(application, adapter);
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
    userStream.disconnect();
    super.onStop();
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
