/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.realmdata;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.freshdigitable.udonroad.TimelineAdapter;
import com.freshdigitable.udonroad.TimelineFragment;

import java.util.List;

import io.realm.RealmConfiguration;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.User;

/**
 * Created by akihit on 2016/06/06.
 */
public abstract class RealmTimelineFragment extends TimelineFragment {
  private static final String TAG = RealmTimelineFragment.class.getSimpleName();

  protected RealmTimelineAdapter adapter = new RealmTimelineAdapter();

  @Override
  public void onStart() {
    super.onStart();
    final RealmConfiguration rc = createRealmConfiguration();
    adapter.openRealm(rc);
  }

  @Override
  public void onStop() {
    super.onStop();
    adapter.closeRealm();
  }

  public abstract RealmConfiguration createRealmConfiguration();

  protected abstract void fetchTweet();

  protected abstract void fetchTweet(final Paging page);

  protected void fetchTweet(final Observable.OnSubscribe<List<Status>> onSubscribe) {
    Observable.create(onSubscribe)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(new Action1<List<Status>>() {
          @Override
          public void call(List<Status> statuses) {
            adapter.addNewStatuses(statuses);
          }
        })
        .doOnError(new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            Log.e(TAG, "fetch: ", throwable);
          }
        })
        .subscribe();
  }

  protected static <T extends Fragment> T getInstance(T fragment, @NonNull User user) {
    final Bundle args = new Bundle();
    args.putSerializable("USER", user);
    fragment.setArguments(args);
    return fragment;
  }

  @Nullable
  protected User getUser() {
    final Bundle arguments = getArguments();
    return (User) arguments.get("USER");

  }

  @Override
  protected TimelineAdapter getTimelineAdapter() {
    return adapter;
  }
}
