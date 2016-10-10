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

import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.datastore.MediaCache;
import com.freshdigitable.udonroad.datastore.TypedCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import twitter4j.ExtendedMediaEntity;
import twitter4j.Status;
import twitter4j.User;

import static com.freshdigitable.udonroad.realmdata.StatusRealm.KEY_ID;

/**
 * StatusCacheRealm implements StatusCache for Realm.
 *
 * Created by akihit on 2016/07/22.
 */
public class StatusCacheRealm extends BaseCacheRealm implements TypedCache<Status>, MediaCache {
  @SuppressWarnings("unused")
  public static final String TAG = StatusCacheRealm.class.getSimpleName();
  private final ConfigStore configStore;
  private UserCacheRealm userTypedCache;

  public StatusCacheRealm(ConfigStore configStore) {
    this.configStore = configStore;
  }

  @Override
  public void open() {
    super.open();
    this.userTypedCache = new UserCacheRealm(this);
    configStore.open();
  }

  @Override
  public void close() {
    super.close();
    configStore.close();
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

    final Collection<Status> updates = splitUpsertingStatus(statuses);
    cache.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        final ArrayList<StatusRealm> inserts = new ArrayList<>(updates.size());
        for (Status s : updates) {
          final StatusRealm update = findById(realm, s.getId(), StatusRealm.class);
          if (update == null) {
            inserts.add(new StatusRealm(s));
          } else {
            update.merge(s);
          }
        }
        realm.insertOrUpdate(inserts);
      }
    });
    for (Status s : updates) {
      userTypedCache.upsert(s.getUserMentionEntities());
    }
  }

  @NonNull
  private Collection<Status> splitUpsertingStatus(Collection<Status> statuses) {
    final LinkedHashMap<Long, Status> updates = new LinkedHashMap<>();
    for (Status s : statuses) {
      if (!configStore.isIgnoredUser(s.getUser().getId())) {
        updates.put(s.getId(), s);
      }
      final Status quotedStatus = s.getQuotedStatus();
      if (quotedStatus != null
          && !configStore.isIgnoredUser(quotedStatus.getUser().getId())) {
        updates.put(quotedStatus.getId(), quotedStatus);
      }
      final Status retweetedStatus = s.getRetweetedStatus();
      if (retweetedStatus != null
          && !configStore.isIgnoredUser(retweetedStatus.getUser().getId())) {
        updates.put(retweetedStatus.getId(), retweetedStatus);
        final Status rtQuotedStatus = retweetedStatus.getQuotedStatus();
        if (rtQuotedStatus != null
            && !configStore.isIgnoredUser(rtQuotedStatus.getUser().getId())) {
          updates.put(rtQuotedStatus.getId(), rtQuotedStatus);
        }
      }
    }

    final Collection<Status> res = updates.values();
    final Collection<User> updateUsers = splitUpsertingUser(res);
    userTypedCache.upsert(new ArrayList<>(updateUsers));
    return res;
  }

  private Collection<User> splitUpsertingUser(Collection<Status> updates) {
    Map<Long, User> res = new LinkedHashMap<>(updates.size());
    for (Status s : updates) {
      final User user = s.getUser();
      res.put(user.getId(), user);
    }
    return res.values();
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
    final Collection<Status> statuses = splitUpsertingStatus(Collections.singletonList(status));
    final ArrayList<StatusRealm> entities = new ArrayList<>(statuses.size());
    for (Status s: statuses) {
      entities.add(new StatusRealm(s));
    }
    cache.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        realm.insertOrUpdate(entities);
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
    return findById(cache, mediaId, ExtendedMediaEntityRealm.class);
  }

  @Nullable
  private StatusRealm getStatusInternal(long id) {
    final StatusRealm status = findById(cache, id, StatusRealm.class);
    status.setUser(userTypedCache.find(status.getUserId()));
    return status;
  }
}
