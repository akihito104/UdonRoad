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

import static com.freshdigitable.udonroad.realmdata.StatusRealm.KEY_ID;
import static com.freshdigitable.udonroad.realmdata.StatusRealm.KEY_QUOTAD_STATUS_ID;
import static com.freshdigitable.udonroad.realmdata.StatusRealm.KEY_RETWEETED_STATUS_ID;

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
    realm = Realm.getInstance(config);
    defaultTimeline();
  }

  public void closeRealm() {
    Log.d(TAG, "closeRealm: ");
    clearSelectedTweet();
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
      final StatusRealm update = createIfUpdateTimeline(s);
      if (update == null) {
        inserts.add(new StatusRealm(s));
      } else {
        updates.add(margeStatus(update, s));
      }

      final RealmResults<StatusRealm> u = findReferringStatus(s.getId());
      if (u.size() > 0) {
        insertOrUpdateReferredStatus(s);
        updates.addAll(u);
      }

      final long quotedStatusId = s.getQuotedStatusId();
      if (quotedStatusId > 0) {
        final Status quotedStatus = s.getQuotedStatus();
        insertOrUpdateReferredStatus(quotedStatus);

        final StatusRealm q = createIfUpdateTimeline(quotedStatus);
        if (q != null) {
          updates.add(q);
        }

        final RealmResults<StatusRealm> updatedQuotedStatus = findReferringStatus(quotedStatusId);
        if (updatedQuotedStatus.size() > 0) {
          updates.addAll(updatedQuotedStatus);
        }
      }

      if (!s.isRetweet()) {
        continue;
      }
      final Status retweetedStatus = s.getRetweetedStatus();
      insertOrUpdateReferredStatus(retweetedStatus);
      insertOrUpdateReferredStatus(retweetedStatus.getQuotedStatus());

      final StatusRealm rs = createIfUpdateTimeline(retweetedStatus);
      if (rs != null) {
        updates.add(rs);
      }
      final RealmResults<StatusRealm> rtedUpdate = findReferringStatus(retweetedStatus.getId());
      if (rtedUpdate.size() > 0) {
        updates.addAll(rtedUpdate);
      }

      final long rtQuotedStatusId = retweetedStatus.getQuotedStatusId();
      if (rtQuotedStatusId > 0) {
        final RealmResults<StatusRealm> rtUpdatedQuotedStatus = findReferringStatus(rtQuotedStatusId);
        if (rtUpdatedQuotedStatus.size() > 0) {
          updates.addAll(rtUpdatedQuotedStatus);
        }
      }
    }

    if (inserts.size() > 0) {
      insertStatus(inserts);
    }
    if (updates.size() > 0) {
      updateStatus(updates);
    }
  }

  @NonNull
  private RealmResults<StatusRealm> findReferringStatus(long id) {
    return timeline.where()
              .beginGroup()
              .equalTo(KEY_QUOTAD_STATUS_ID, id)
              .or()
              .equalTo(KEY_RETWEETED_STATUS_ID, id)
              .endGroup()
              .findAll();
  }

  @Nullable
  private StatusRealm createIfUpdateTimeline(Status newer) {
    if (newer == null) {
      return null;
    }
    final StatusRealm older = timeline.where()
        .equalTo(KEY_ID, newer.getId())
        .findFirst();
    if (older == null) {
      return null;
    }
    return margeStatus(older, newer);
  }

  private StatusRealm margeStatus(Status older, Status newer) {
    final StatusRealm res = new StatusRealm(newer);
    res.setRetweeted(newer.isRetweeted() || older.isRetweeted());
    res.setFavorited(newer.isFavorited() || older.isFavorited());
    return res;
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
        .beginGroup()
        .equalTo(KEY_ID, statusId)
        .or()
        .equalTo(KEY_RETWEETED_STATUS_ID, statusId)
        .endGroup()
        .findAll();
    if (res.size() < 1) {
      return;
    }

    final List<Integer> deleted = searchTimeline(res);
    if (isStatusViewSelected()) {
      for (Status r : res) {
        if (getSelectedTweetId() == r.getId()) {
          clearSelectedTweet();
        }
      }
    }

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
    realm.where(ReferredStatusRealm.class)
        .equalTo(KEY_ID, statusId)
        .findAll()
        .deleteAllFromRealm();
    realm.commitTransaction();
  }

  private void insertOrUpdateReferredStatus(@Nullable final Status rtStatus) {
    if (rtStatus == null) {
      return;
    }
    realm.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        final ReferredStatusRealm update = realm.where(ReferredStatusRealm.class)
            .equalTo(KEY_ID, rtStatus.getId())
            .findFirst();
        if (update != null) {
          update(update, rtStatus);
        } else {
          realm.copyToRealmOrUpdate(new ReferredStatusRealm(rtStatus));
        }
      }

      private void update(ReferredStatusRealm update, Status s) {
        if (update == null) {
          return;
        }
        update.setFavorited(update.isFavorited() || s.isFavorited());
        final int favoriteCount = s.getFavoriteCount();
        if (favoriteCount > 0) {
          update.setFavoriteCount(favoriteCount);
        }
        update.setRetweeted(update.isRetweeted() || s.isRetweeted());
        final int retweetCount = s.getRetweetCount();
        if (retweetCount > 0) {
          update.setRetweetCount(retweetCount);
        }
      }
    });
  }

  @Override
  protected Status get(int position) {
    final StatusRealm res = timeline.get(position);
    if (res.isRetweet()) {
      final ReferredStatusRealm rtStatus = getReferredStatus(res.getRetweetedStatusId());
      if (rtStatus.getQuotedStatusId() > 0) {
        rtStatus.setQuotedStatus(getReferredStatus(rtStatus.getQuotedStatusId()));
      }
      res.setRetweetedStatus(rtStatus);
      return res;
    }
    if (res.getQuotedStatusId() > 0) {
      res.setQuotedStatus(getReferredStatus(res.getQuotedStatusId()));
    }
    return res;
  }

  private ReferredStatusRealm getReferredStatus(long id) {
    return realm.where(ReferredStatusRealm.class)
        .equalTo(KEY_ID, id)
        .findFirst();
  }

  @Override
  public int getItemCount() {
    return timeline.size();
  }

  public void defaultTimeline() {
    timeline = realm
        .where(StatusRealm.class)
        .findAllSorted(KEY_ID, Sort.DESCENDING);
    notifyDataSetChanged();
  }
}
