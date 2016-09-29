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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.freshdigitable.udonroad.datastore.MediaCache;
import com.freshdigitable.udonroad.datastore.TypedCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import twitter4j.ExtendedMediaEntity;
import twitter4j.Status;

import static com.freshdigitable.udonroad.realmdata.StatusRealm.KEY_ID;

/**
 * StatusCacheRealm implements StatusCache for Realm.
 *
 * Created by akihit on 2016/07/22.
 */
public class StatusCacheRealm extends BaseCacheRealm implements TypedCache<Status>, MediaCache {
  @SuppressWarnings("unused")
  public static final String TAG = StatusCacheRealm.class.getSimpleName();
  private UserCacheRealm userTypedCache;

  public StatusCacheRealm() {
  }

  @Override
  public void open() {
    super.open();
    userTypedCache = new UserCacheRealm(this);
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
            realm.insertOrUpdate(new StatusRealm(s));
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
    for (Status s : updates) {
      userTypedCache.upsert(s.getUserMentionEntities());
    }
  }

  @NonNull
  private List<Status> splitUpsertingStatus(List<Status> statuses) {
    final List<Status> updates = new ArrayList<>();
    for (Status s : statuses) {
      updates.add(s);
      final Status quotedStatus = s.getQuotedStatus();
      if (quotedStatus != null) {
        updates.add(quotedStatus);
      }
      final Status retweetedStatus = s.getRetweetedStatus();
      if (retweetedStatus != null) {
        updates.add(retweetedStatus);
        final Status rtQuotedStatus = retweetedStatus.getQuotedStatus();
        if (rtQuotedStatus != null) {
          updates.add(rtQuotedStatus);
        }
      }
    }
    return updates;
  }

  @Override
  public void delete(final long statusId) {
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
          realm.insertOrUpdate(new StatusRealm(s));
        }
      }
    });
  }

  @Override
  @Nullable
  public Status find(long id) {
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
  public Observable<Status> observeById(long statusId) {
    final StatusRealm status = (StatusRealm) find(statusId);
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
  public ExtendedMediaEntity getMediaEntity(long mediaId) {
    return cache.where(ExtendedMediaEntityRealm.class)
        .equalTo("id", mediaId)
        .findFirst();
  }

  @Nullable
  private StatusRealm getStatusInternal(long id) {
    return cache.where(StatusRealm.class)
        .equalTo(KEY_ID, id)
        .findFirst();
  }
}
