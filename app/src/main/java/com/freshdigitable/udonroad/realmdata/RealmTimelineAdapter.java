/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.realmdata;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
    final RealmConfiguration config = new RealmConfiguration.Builder(context).build();
    openRealm(config);
  }

  public void openRealm(RealmConfiguration config) {
    Log.d(TAG, "openRealm: ");
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
    if (statuses.size() < 1) {
      return;
    }
    final List<StatusRealm> inserts = new ArrayList<>();
    final List<StatusRealm> updates = new ArrayList<>();
    for (Status s : statuses) {
      final StatusRealm update = timeline.where()
          .equalTo("id", s.getId())
          .findFirst();
      final StatusRealm status = new StatusRealm(s);

      if (update == null) {
        inserts.add(status);
        insertOrUpdateRetweetedStatus(status.getRetweetedStatus());
      } else {
        status.setFavorited(update.isFavorited() | s.isFavorited());
        status.setRetweeted(update.isRetweeted() | s.isRetweeted());
        updates.add(status);
      }

      if (!s.isRetweet()) {
        continue;
      }
      final Status retweetedStatus = s.getRetweetedStatus();
      final StatusRealm rtUpdate = timeline.where()
          .equalTo("id", retweetedStatus.getId())
          .findFirst();
      if (rtUpdate != null) {
        final StatusRealm rtStatus = new StatusRealm(retweetedStatus);
        rtStatus.setFavorited(rtUpdate.isFavorited() | retweetedStatus.isFavorited());
        rtStatus.setRetweeted(rtUpdate.isRetweeted() | retweetedStatus.isRetweeted());
        updates.add(rtStatus);
      }

      final RealmResults<StatusRealm> rtedUpdate = timeline.where()
          .equalTo("retweetedStatusId", retweetedStatus.getId())
          .findAll();
      updates.addAll(rtedUpdate);
    }

    if (inserts.size() > 0) {
      insertStatus(inserts);
    }
    if (updates.size() > 0) {
      updateStatus(updates);
    }
  }

  private void insertStatus(List<StatusRealm> inserts) {
    realm.beginTransaction();
    final List<StatusRealm> inserted = realm.copyToRealmOrUpdate(inserts);
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
        .subscribe(
            new Action1<RealmResults<StatusRealm>>() {
              @Override
              public void call(final RealmResults<StatusRealm> results) {
                notifyInserted(inserted, results);
              }
            });
  }

  private void notifyInserted(List<StatusRealm> inserted, RealmResults<StatusRealm> results) {
    if (inserted.size() < 1) {
      return;
    }
    final List<Integer> index = searchTimeline(inserted, results);
    if (index.size() < 1) {
      return;
    }
    Collections.sort(index);
    for (int i : index) {
      notifyItemInserted(i);
    }
  }

  private void updateStatus(List<StatusRealm> updates) {
    realm.beginTransaction();
    final List<StatusRealm> updated = realm.copyToRealmOrUpdate(updates);
    realm.commitTransaction();
    timeline.asObservable()
        .filter(new Func1<RealmResults<StatusRealm>, Boolean>() {
          @Override
          public Boolean call(RealmResults<StatusRealm> results) {
            return results.isLoaded();
          }
        })
        .first()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<RealmResults<StatusRealm>>() {
          @Override
          public void call(RealmResults<StatusRealm> results) {
            notifyChanged(updated, results);
          }
        });
  }

  private void notifyChanged(List<StatusRealm> changed, RealmResults<StatusRealm> results) {
    if (changed.size() < 1) {
      return;
    }
    final List<Integer> index = searchTimeline(changed, results);
    if (index.size() < 1) {
      return;
    }
    for (int i : index) {
      notifyItemChanged(i);
    }
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
        .subscribe(
            new Action1<RealmResults<StatusRealm>>() {
              @Override
              public void call(RealmResults<StatusRealm> statusRealms) {
                Log.d(TAG, "call: deletedStatus");
                for (int d : deleted) {
                  notifyItemRemoved(d);
                }
              }
            });
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

  private void insertOrUpdateRetweetedStatus(@Nullable final Status rtStatus) {
    if (rtStatus == null) {
      return;
    }
    realm.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        final RetweetedStatusRealm update = realm.where(RetweetedStatusRealm.class)
            .equalTo("id", rtStatus.getId())
            .findFirst();
        if (update != null) {
          update(update, rtStatus);
        } else {
          realm.copyToRealmOrUpdate(new RetweetedStatusRealm(rtStatus));
        }
      }

      private void update(RetweetedStatusRealm update, Status s) {
        if (update == null) {
          return;
        }
        update.setFavorited(update.isFavorited() | s.isFavorited());
        update.setFavoriteCount(s.getFavoriteCount());
        update.setRetweeted(update.isRetweeted() | s.isRetweeted());
        update.setRetweetCount(s.getRetweetCount());
      }
    });
  }

  @Override
  protected Status get(int position) {
    final StatusRealm res = timeline.get(position);
    if (res.isRetweet()) {
      final RetweetedStatusRealm rtStatus = realm.where(RetweetedStatusRealm.class)
          .equalTo("id", res.getRetweetedStatusId())
          .findFirst();
      res.setRetweetedStatus(rtStatus);
    }
    return res;
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
