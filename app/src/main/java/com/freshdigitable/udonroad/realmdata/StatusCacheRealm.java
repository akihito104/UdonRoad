/*
 * Copyright (c) 2016. Matsuda, Akihit (akihito104)
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.freshdigitable.udonroad.datastore.StatusCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import twitter4j.Status;
import twitter4j.User;
import twitter4j.UserMentionEntity;

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
  public void upsert(@Nullable Status status) {
    if (status == null) {
      return;
    }
    upsert(Collections.singletonList(status));
  }

  @Override
  public void upsert(List<Status> statuses) {
    if (statuses == null || statuses.isEmpty()) {
      return;
    }

    final List<Status> updates = splitUpsertingStatus(statuses);
    cache.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        for (Status s : updates) {
          final StatusRealm update = realm.where(StatusRealm.class)
              .equalTo(KEY_ID, s.getId())
              .findFirst();
          if (update != null) {
            update(update, s);
          } else {
            realm.copyToRealmOrUpdate(new StatusRealm(s));
          }
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

  @NonNull
  private List<Status> splitUpsertingStatus(List<Status> statuses) {
    final List<Status> updates = new ArrayList<>();
    for (Status s : statuses) {
      updates.add(s);
      final Status retweetedStatus = s.getRetweetedStatus();
      if (retweetedStatus != null) {
        updates.add(retweetedStatus);
        final Status quotedStatus = retweetedStatus.getQuotedStatus();
        if (quotedStatus != null) {
          updates.add(quotedStatus);
        }
      }
      final Status quotedStatus = s.getQuotedStatus();
      if (quotedStatus != null) {
        updates.add(quotedStatus);
      }
    }
    return updates;
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

  /**
   * to update with response of destroyFavorite or destroy Retweet
   * @param status new data for update
   */
  @Override
  public void forceUpsert(final Status status) {
    final List<Status> statuses = splitUpsertingStatus(Collections.singletonList(status));
    cache.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        for (Status s: statuses) {
          realm.copyToRealmOrUpdate(new StatusRealm(s));
        }
      }
    });
  }

  @Override
  @Nullable
  public Status findStatus(long id) {
    final StatusRealm res = getStatusInternal(id);
    if (res == null) {
      return null;
    }
    if (res.isRetweet()) {
      final StatusRealm rtStatus = getStatusInternal(res.getRetweetedStatusId());
      if (rtStatus != null && rtStatus.getQuotedStatusId() > 0) {
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

  @Override
  public Observable<Status> observeStatusById(long statusId) {
    final StatusRealm status = (StatusRealm) findStatus(statusId);
    if (status == null) {
      return null;
    }
    return Observable.create(new Observable.OnSubscribe<Status>() {
      @Override
      public void call(final Subscriber<? super Status> subscriber) {
        StatusRealm.addChangeListener(status, new RealmChangeListener<StatusRealm>() {
          @Override
          public void onChange(StatusRealm element) {
            subscriber.onNext(element);
          }
        });
        subscriber.onNext(status);
      }
    }).doOnUnsubscribe(new Action0() {
      @Override
      public void call() {
        StatusRealm.removeChangeListeners(status);
      }
    });
  }

  @Override
  public void upsertUser(final User user) {
    cache.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        realm.insert(new UserRealm(user));
      }
    });
  }

  @Override
  public void upsertUser(UserMentionEntity mentionEntity) {
    final User user = getUser(mentionEntity.getId());
    if (user == null) {
      upsertUser(new UserRealm(mentionEntity));
    }
  }

  @Override
  public User getUser(long id) {
    return cache.where(UserRealm.class)
        .equalTo("id", id)
        .findFirst();
  }

  @Override
  public Observable<User> observeUserById(final long userId) {
    final UserRealm user = cache.where(UserRealm.class)
        .equalTo("id", userId)
        .findFirst();
    return Observable.create(new Observable.OnSubscribe<User>() {
      @Override
      public void call(final Subscriber<? super User> subscriber) {
        UserRealm.addChangeListener(user, new RealmChangeListener<UserRealm>() {
          @Override
          public void onChange(UserRealm element) {
            subscriber.onNext(element);
          }
        });
        subscriber.onNext(user);
      }
    }).doOnUnsubscribe(new Action0() {
      @Override
      public void call() {
        UserRealm.removeChangeListeners(user);
      }
    });
  }

  @Nullable
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
