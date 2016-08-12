/*
 * Copyright (c) 2016. Akihito Matsuda (akihito104)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.freshdigitable.udonroad.realmdata;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.freshdigitable.udonroad.datastore.StatusCache;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import twitter4j.Status;
import twitter4j.User;

import static com.freshdigitable.udonroad.realmdata.StatusRealm.KEY_ID;

/**
 * Created by akihit on 2016/07/22.
 */
public class StatusCacheRealm implements StatusCache {
  @SuppressWarnings("unused")
  public static final String TAG = StatusCacheRealm.class.getSimpleName();
  private Realm cache;

  @Override
  public void open(Context context) {
    Log.d(TAG, "StatusCacheRealm: open");
    final RealmConfiguration config = new RealmConfiguration.Builder(context)
        .name("cache")
        .deleteRealmIfMigrationNeeded()
        .build();
    cache = Realm.getInstance(config);
  }

  @Override
  public void upsert(Status status) {
    upsertStatus(status);
  }

  @Override
  public void upsert(List<Status> statuses) {
    for (Status s : statuses) {
      upsertStatus(s);
    }
  }

  private void upsertStatus(@Nullable final Status rtStatus) {
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

  @Override
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

  @Override
  public Status getStatus(long id) {
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

  public void upsertUser(final User user) {
    cache.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        realm.insert(new UserRealm(user));
      }
    });
  }

  @Override
  public User getUser(long id) {
    return cache.where(UserRealm.class)
        .equalTo("id", id)
        .findFirst();
  }

  private StatusRealm getStatusInternal(long id) {
    return cache.where(StatusRealm.class)
        .equalTo(KEY_ID, id)
        .findFirst();
  }

  @Override
  public void clear() {
    cache.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        cache.deleteAll();
      }
    });
  }

  @Override
  public void close() {
    if (cache == null || cache.isClosed()) {
      return;
    }
    Log.d(TAG, "close: " + cache.getConfiguration().getRealmFileName());
    cache.close();
  }
}
