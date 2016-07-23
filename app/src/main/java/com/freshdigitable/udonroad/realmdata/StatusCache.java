/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.realmdata;

import android.content.Context;
import android.support.annotation.Nullable;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import twitter4j.Status;
import twitter4j.User;

import static com.freshdigitable.udonroad.realmdata.StatusRealm.KEY_ID;

/**
 * Created by akihit on 2016/07/22.
 */
public class StatusCache {

  private final Realm cache;

  public StatusCache(Context context) {
    final RealmConfiguration config = new RealmConfiguration.Builder(context)
        .name("cache")
        .deleteRealmIfMigrationNeeded()
        .build();
    cache = Realm.getInstance(config);
  }

  public void upsertStatus(@Nullable final Status rtStatus) {
    if (rtStatus == null) {
      return;
    }
    cache.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        final StatusRealm update = realm.where(StatusRealm.class)
            .equalTo(KEY_ID, rtStatus.getId())
            .findFirst();
        if (update != null) {
          update(update, rtStatus);
        } else {
          realm.copyToRealmOrUpdate(new StatusRealm(rtStatus));
        }
      }

      private void update(StatusRealm update, Status s) {
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

  public void deleteStatus(final long statusId) {
    cache.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        realm.where(StatusRealm.class)
            .equalTo(KEY_ID, statusId)
            .findAll()
            .deleteAllFromRealm();
      }
    });
  }

  public StatusRealm getStatus(long id) {
    final StatusRealm res = getStatusInternal(id);
    if (res.isRetweet()) {
    final StatusRealm rtStatus = getStatusInternal(res.getRetweetedStatusId());
    if (rtStatus.getQuotedStatusId() > 0) {
      rtStatus.setQuotedStatus(getStatusInternal(rtStatus.getQuotedStatusId()));
    }
    res.setRetweetedStatus(rtStatus);
    return res;
  }
    if (res.getQuotedStatusId() > 0) {
      res.setQuotedStatus(getStatusInternal(res.getQuotedStatusId()));
    }
    return res;
  }

  public User getUser(long id) {
    return cache.where(UserRealm.class)
        .equalTo("id", id)
        .findFirst();
  }

  private StatusRealm getStatusInternal(long id){
    return cache.where(StatusRealm.class)
        .equalTo(KEY_ID, id)
        .findFirst();
  }

  public void clear() {
    cache.deleteAll();
  }

  public void close() {
    cache.close();
  }
}
