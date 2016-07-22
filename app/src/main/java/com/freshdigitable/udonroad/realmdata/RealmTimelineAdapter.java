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
  private RealmResults<StatusIDs> timeline;
  private StatusCache statusCache;

  public void openRealm(Context context) {
    final RealmConfiguration config = new RealmConfiguration.Builder(context).build();
    openRealm(context, config);
  }

  public void openRealm(Context context, RealmConfiguration config) {
    Log.d(TAG, "openRealm: ");
    statusCache = new StatusCache(context);
    realm = Realm.getInstance(config);
    defaultTimeline();
  }

  public void closeRealm() {
    Log.d(TAG, "closeRealm: ");
    clearSelectedTweet();
    realm.close();
    statusCache.close();
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
    final List<StatusIDs> inserts = new ArrayList<>();
    final List<StatusIDs> updates = new ArrayList<>();
    for (Status s : statuses) {
      final StatusIDs update = findTimeline(s);
      if (update == null) {
        inserts.add(new StatusIDs(s));
      } else {
        updates.add(new StatusIDs(s));
      }
      statusCache.upsertStatus(s);

      final RealmResults<StatusIDs> u = findReferringStatus(s.getId());
      if (u.size() > 0) {
        statusCache.upsertStatus(s);
        updates.addAll(u);
      }

      final long quotedStatusId = s.getQuotedStatusId();
      if (quotedStatusId > 0) {
        final Status quotedStatus = s.getQuotedStatus();
        statusCache.upsertStatus(quotedStatus);

        final StatusIDs q = findTimeline(quotedStatus);
        if (q != null) {
          updates.add(q);
        }

        final RealmResults<StatusIDs> updatedQuotedStatus = findReferringStatus(quotedStatusId);
        if (updatedQuotedStatus.size() > 0) {
          updates.addAll(updatedQuotedStatus);
        }
      }

      if (!s.isRetweet()) {
        continue;
      }
      final Status retweetedStatus = s.getRetweetedStatus();
      statusCache.upsertStatus(retweetedStatus);
      statusCache.upsertStatus(retweetedStatus.getQuotedStatus());

      final StatusIDs rs = findTimeline(retweetedStatus);
      if (rs != null) {
        updates.add(rs);
      }
      final RealmResults<StatusIDs> rtedUpdate = findReferringStatus(retweetedStatus.getId());
      if (rtedUpdate.size() > 0) {
        updates.addAll(rtedUpdate);
      }

      final long rtQuotedStatusId = retweetedStatus.getQuotedStatusId();
      if (rtQuotedStatusId > 0) {
        final RealmResults<StatusIDs> rtUpdatedQuotedStatus = findReferringStatus(rtQuotedStatusId);
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
  private RealmResults<StatusIDs> findReferringStatus(long id) {
    return timeline.where()
              .beginGroup()
              .equalTo(KEY_QUOTAD_STATUS_ID, id)
              .or()
              .equalTo(KEY_RETWEETED_STATUS_ID, id)
              .endGroup()
              .findAll();
  }

  @Nullable
  private StatusIDs findTimeline(Status newer) {
    if (newer == null) {
      return null;
    }
    return timeline.where()
        .equalTo(KEY_ID, newer.getId())
        .findFirst();
  }

  private void insertStatus(List<StatusIDs> inserts) {
    realm.beginTransaction();
    final List<StatusIDs> inserted = realm.copyToRealmOrUpdate(inserts);
    realm.commitTransaction();
    timeline.asObservable()
        .filter(new Func1<RealmResults<StatusIDs>, Boolean>() {
          @Override
          public Boolean call(RealmResults<StatusIDs> statusRealms) {
            return statusRealms.isLoaded();
          }
        })
        .first()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            new Action1<RealmResults<StatusIDs>>() {
              @Override
              public void call(final RealmResults<StatusIDs> results) {
                notifyInserted(inserted, results);
              }
            });
  }

  private void notifyInserted(List<StatusIDs> inserted, RealmResults<StatusIDs> results) {
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

  private void updateStatus(List<StatusIDs> updates) {
    realm.beginTransaction();
    final List<StatusIDs> updated = realm.copyToRealmOrUpdate(updates);
    realm.commitTransaction();
    timeline.asObservable()
        .filter(new Func1<RealmResults<StatusIDs>, Boolean>() {
          @Override
          public Boolean call(RealmResults<StatusIDs> results) {
            return results.isLoaded();
          }
        })
        .first()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<RealmResults<StatusIDs>>() {
          @Override
          public void call(RealmResults<StatusIDs> results) {
            notifyChanged(updated, results);
          }
        });
  }

  private void notifyChanged(List<StatusIDs> changed, RealmResults<StatusIDs> results) {
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

    final RealmResults<StatusIDs> res = realm.where(StatusIDs.class)
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
      final long selectedTweetId = getSelectedTweetId();
      for (StatusIDs r : res) {
        if (selectedTweetId == r.getId()) {
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
        .filter(new Func1<RealmResults<StatusIDs>, Boolean>() {
          @Override
          public Boolean call(RealmResults<StatusIDs> statusRealms) {
            return statusRealms.isLoaded();
          }
        })
        .first()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            new Action1<RealmResults<StatusIDs>>() {
              @Override
              public void call(RealmResults<StatusIDs> statusRealms) {
                Log.d(TAG, "call: deletedStatus");
                for (int d : deleted) {
                  notifyItemRemoved(d);
                  statusCache.deleteStatus(d);
                }
              }
            });
  }

  @NonNull
  private List<Integer> searchTimeline(List<StatusIDs> items) {
    return searchTimeline(items, timeline);
  }

  @NonNull
  private List<Integer> searchTimeline(List<StatusIDs> items, RealmResults<StatusIDs> timeline) {
    final List<Integer> res = new ArrayList<>(items.size());
    for (StatusIDs sr : items) {
      final int index = timeline.indexOf(sr);
      if (index >= 0) {
        res.add(index);
      }
    }
    return res;
  }

  @Override
  protected Status get(int position) {
    final StatusIDs ids = timeline.get(position);
    return statusCache.getStatus(ids.getId());
  }

  @Override
  public int getItemCount() {
    return timeline.size();
  }

  public void defaultTimeline() {
    timeline = realm
        .where(StatusIDs.class)
        .findAllSorted(KEY_ID, Sort.DESCENDING);
    notifyDataSetChanged();
  }
}
