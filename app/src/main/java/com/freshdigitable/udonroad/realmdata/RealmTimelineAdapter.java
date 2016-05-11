/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.realmdata;

import android.content.Context;
import android.util.Log;

import com.freshdigitable.udonroad.TimelineAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.Sort;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import twitter4j.Status;

/**
 * RecyclerView adapter for RealmResult
 *
 * Created by akihit on 2016/05/04.
 */
public class RealmTimelineAdapter extends TimelineAdapter {
  private static final String TAG = RealmTimelineAdapter.class.getSimpleName();
  private Realm realm;
  private RealmResults<StatusRealm> timeline;

  public void openRealm(Context context) {
    Log.d(TAG, "openRealm: ");
    final RealmConfiguration config = new RealmConfiguration.Builder(context).build();
    Realm.deleteRealm(config);
    realm = Realm.getInstance(config);
    timeline = realm.where(StatusRealm.class).findAllSorted("createdAt", Sort.DESCENDING);
    cleanOldStatuses();
  }

  public void closeRealm() {
    Log.d(TAG, "closeRealm: ");
    cleanOldStatuses();
    realm.close();
  }

  @Override
  public void addNewStatus(Status status) {
    addNewStatuses(Collections.singletonList(status));
  }

  @Override
  public void addNewStatuses(List<Status> statuses) {
    List<StatusRealm> sr = new ArrayList<>(statuses.size());
    for (Status s : statuses) {
      sr.add(new StatusRealm(s));
    }
    realm.beginTransaction();
    final List<StatusRealm> copied = realm.copyToRealmOrUpdate(sr);
    realm.commitTransaction();

    timeline.asObservable()
        .filter(new Func1<RealmResults<StatusRealm>, Boolean>() {
          @Override
          public Boolean call(RealmResults<StatusRealm> statusRealms) {
            return statusRealms.isLoaded();
          }
        })
        .first()
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(new Action1<RealmResults<StatusRealm>>() {
          @Override
          public void call(final RealmResults<StatusRealm> results) {
            Observable.from(copied)
                .map(new Func1<StatusRealm, Integer>() {
                  @Override
                  public Integer call(StatusRealm statusRealm) {
                    return results.indexOf(statusRealm);
                  }
                })
                .filter(new Func1<Integer, Boolean>() {
                  @Override
                  public Boolean call(Integer integer) {
                    return integer >= 0;
                  }
                })
                .toSortedList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Integer>>() {
                  @Override
                  public void call(List<Integer> integers) {
                    if (integers.size() < 1) {
                      return;
                    }
                    notifyItemRangeInserted(integers.get(0), integers.size());
                  }
                });
          }
        })
        .subscribe();
  }

  @Override
  public void addNewStatusesAtLast(List<Status> statuses) {
    addNewStatuses(statuses);
  }

  @Override
  public void deleteStatus(long statusId) {
    deleteRetweetedStatus(statusId);

    final RealmResults<StatusRealm> res = realm.where(StatusRealm.class)
        .equalTo("id", statusId)
        .findAll();
    if (res.size() <= 0) {
      return;
    }

    final List<Integer> deleted = new ArrayList<>(res.size());
    for (StatusRealm sr : res) {
      deleted.add(timeline.indexOf(sr));
    }

    realm.beginTransaction();
    res.deleteAllFromRealm();
    realm.commitTransaction();

    timeline.asObservable()
        .filter(new Func1<RealmResults<StatusRealm>, Boolean>() {
          @Override
          public Boolean call(RealmResults<StatusRealm> statusRealms) {
            return statusRealms.isLoaded();
          }
        })
        .first()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<RealmResults<StatusRealm>>() {
          @Override
          public void call(RealmResults<StatusRealm> statusRealms) {
            Log.d(TAG, "call: deletedStatus");
            for (int d : deleted) {
              notifyItemRemoved(d);
            }
            notifyDataSetChanged();
          }
        });
  }

  private void deleteRetweetedStatus(long statusId) {
    realm.beginTransaction();
    realm.where(RetweetedStatusRealm.class)
        .equalTo("id", statusId)
        .findAll()
        .deleteAllFromRealm();
    realm.commitTransaction();
  }

  @Override
  protected Status get(int position) {
    return timeline.get(position);
  }

  @Override
  public int getItemCount() {
    return timeline.size();
  }

  private void cleanOldStatuses() {
    final long now = System.currentTimeMillis();
    final Date date = new Date(now - TimeUnit.HOURS.toMillis(3));
    final RealmResults<StatusRealm> res = realm.where(StatusRealm.class)
        .lessThanOrEqualTo("createdAt", date)
        .findAll();
    Log.d(TAG, "cleanOldStatuses: clear:" + res.size());
    realm.beginTransaction();
    res.deleteAllFromRealm();
    realm.commitTransaction();
  }
}
