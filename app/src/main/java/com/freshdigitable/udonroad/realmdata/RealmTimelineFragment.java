/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.realmdata;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.freshdigitable.udonroad.TimelineAdapter;
import com.freshdigitable.udonroad.TimelineFragment;

import java.util.List;

import io.realm.Realm;
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
    final RealmConfiguration rc = createRealmConfiguration();
    adapter.openRealm(rc);
    super.onStart();
  }

  @Override
  public void onStop() {
    Log.d(TAG, "onStop: ");
    adapter.closeRealm();
    super.onStop();
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "onDestroy: ");
    Realm.deleteRealm(createRealmConfiguration());
    super.onDestroy();
  }

  public abstract RealmConfiguration createRealmConfiguration();

  protected abstract void fetchTweet();

  protected abstract void fetchTweet(final Paging page);

  protected void fetchTweet(final Observable.OnSubscribe<List<Status>> onSubscribe) {
    Observable.create(onSubscribe)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            new Action1<List<Status>>() {
              @Override
              public void call(List<Status> statuses) {
                adapter.addNewStatuses(statuses);
              }
            },
            new Action1<Throwable>() {
              @Override
              public void call(Throwable throwable) {
                Log.e(TAG, "fetch: ", throwable);
              }
            });
  }

  protected static <T extends Fragment> T getInstance(T fragment, @NonNull User user) {
    final Bundle args = new Bundle();
    args.putLong("user_id", user.getId());
    fragment.setArguments(args);
    return fragment;
  }

  protected long getUserId() {
    final Bundle arguments = getArguments();
    return arguments.getLong("user_id");
  }

  @Override
  protected TimelineAdapter getTimelineAdapter() {
    return adapter;
  }
}
