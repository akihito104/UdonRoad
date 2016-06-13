/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.realmdata;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.freshdigitable.udonroad.TimelineAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.Sort;
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
    openRealm(config);
  }

  public void openRealm(RealmConfiguration config) {
    Realm.deleteRealm(config);
    realm = Realm.getInstance(config);
    defaultTimeline();
  }

  public void closeRealm() {
    Log.d(TAG, "closeRealm: ");
    realm.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        realm.deleteAll();
      }
    });
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

    final List<StatusRealm> updates = new ArrayList<>();
    for (Status s : statuses) {
      final StatusRealm update = timeline.where().equalTo("id", s.getId())
          .findFirst();
      if (update != null) {
        updates.add(update);
      }
    }

    realm.beginTransaction();
    final List<StatusRealm> inserts = realm.copyToRealmOrUpdate(sr);
    realm.commitTransaction();

    for (StatusRealm u : updates) {
      for (int i = inserts.size() - 1; i >= 0; i--) {
        StatusRealm c = inserts.get(i);
        if (u.getId() == c.getId()) {
          inserts.remove(c);
          break;
        }
      }
    }

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
            notifyInserted(inserts, results);
          }
        })
        .doOnError(new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            Log.e(TAG, "addNewStatuses: ", throwable);
          }
        })
        .subscribe();
  }

  private void notifyInserted(List<StatusRealm> copied, RealmResults<StatusRealm> results) {
    Log.d(TAG, "notifyInserted:");
    final List<Integer> res = searchTimeline(copied, results);
    if (res.size() < 1) {
      return;
    }
    Collections.sort(res);
    Log.d(TAG, "notifyInserted> index: " + res.get(0) + ", size: " + res.size());
    notifyItemRangeInserted(res.get(0), res.size());
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
    if (res.size() < 1) {
      return;
    }

    final List<Integer> deleted = searchTimeline(res);

    realm.beginTransaction();
    res.deleteAllFromRealm();
    realm.commitTransaction();

    if (deleted.size() <= 0) {
      return;
    }
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
          public void call(RealmResults<StatusRealm> statusRealms) {
            Log.d(TAG, "call: deletedStatus");
            for (int d : deleted) {
              notifyItemRemoved(d);
            }
          }
        })
        .doOnError(new Action1<Throwable>(){
          @Override
          public void call(Throwable throwable) {
            Log.e(TAG, "deleteStatus: ", throwable);
          }
        })
        .subscribe();

  }

  @NonNull
  private List<Integer> searchTimeline(List<StatusRealm> items) {
    return searchTimeline(items, timeline);
  }

  @NonNull
  private List<Integer> searchTimeline(List<StatusRealm> items, RealmResults<StatusRealm> timeline) {
    final List<Integer> res = new ArrayList<>(items.size());
    for (StatusRealm sr : items) {
      final int index = timeline.indexOf(sr);
      if (index >= 0) {
        res.add(index);
      }
    }
    return res;
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

  public void defaultTimeline() {
    timeline = realm
        .where(StatusRealm.class)
        .findAllSorted("id", Sort.DESCENDING);
    notifyDataSetChanged();
  }
}
