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

package com.freshdigitable.udonroad.module.realm;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.datastore.StatusReaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import io.realm.RealmObject;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import twitter4j.User;

import static com.freshdigitable.udonroad.module.realm.BaseCacheRealm.findById;
import static com.freshdigitable.udonroad.module.realm.StatusRealm.KEY_ID;

/**
 * ConfigStoreRealm is data store for user-oriented configurations
 * such as IgnorableUser and StatusReaction.
 *
 * Created by akihit on 2016/07/30.
 */
public class ConfigStoreRealm implements ConfigStore {
  @SuppressWarnings("unused")
  private static final String TAG = ConfigStoreRealm.class.getSimpleName();
  private Realm realm;
  private final RealmConfiguration config;

  public ConfigStoreRealm() {
    config = new RealmConfiguration.Builder()
        .name("config")
        .deleteRealmIfMigrationNeeded()
        .build();
  }

  @Override
  public void open() {
    realm = Realm.getInstance(config);
  }

  @Override
  public void close() {
    realm.close();
  }

  @Override
  public void clear() {
    realm.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        realm.deleteAll();
      }
    });
  }

  @Override
  public void replaceIgnoringUsers(final Collection<Long> iDs) {
    realm.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        realm.delete(IgnoringUser.class);
        List<IgnoringUser> ignoringUsers = new ArrayList<>(iDs.size());
        for (Long id : iDs) {
          final IgnoringUser iUser = new IgnoringUser(id);
          ignoringUsers.add(iUser);
        }
        realm.insertOrUpdate(ignoringUsers);
      }
    });
  }

  @Override
  public boolean isIgnoredUser(long userId) {
    return realm.where(IgnoringUser.class)
        .equalTo("id", userId)
        .findFirst() != null;
  }

  @Override
  public void addIgnoringUser(User user) {
    final long userId = user.getId();
    realm.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        realm.insertOrUpdate(new IgnoringUser(userId));
      }
    });
  }

  @Override
  public void removeIgnoringUser(User user) {
    final long userId = user.getId();
    realm.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        realm.where(IgnoringUser.class)
            .equalTo("id", userId)
            .findAll()
            .deleteAllFromRealm();
      }
    });
  }

  @Override
  public void upsert(StatusReaction entity) {
    if (entity == null) {
      return;
    }
    upsert(Collections.singletonList(entity));
  }

  @Override
  public void upsert(final List<StatusReaction> entities) {
    if (entities == null || entities.isEmpty()) {
      return;
    }
    realm.executeTransaction(upsertTransaction(entities));
  }

  @Override
  public Observable<Void> observeUpsert(final Collection<StatusReaction> entities) {
    if (entities == null || entities.isEmpty()) {
      return Observable.empty();
    }
    return TypedCacheBaseRealm.observeUpsertImpl(realm, upsertTransaction(entities));
  }

  @NonNull
  private static Realm.Transaction upsertTransaction(final Collection<StatusReaction> entities) {
    return new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        final ArrayList<StatusReactionRealm> insertReactions = new ArrayList<>(entities.size());
        for (StatusReaction s : entities) {
          final StatusReactionRealm r = findById(realm, s.getId(), StatusReactionRealm.class);
          if (r == null) {
            insertReactions.add(new StatusReactionRealm(s));
          } else {
            r.merge(s);
          }
        }
        realm.insertOrUpdate(insertReactions);
      }
    };
  }

  @Override
  public void insert(final StatusReaction entity) {
    realm.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        realm.insertOrUpdate(new StatusReactionRealm(entity));
      }
    });
  }

  @Nullable
  @Override
  public StatusReactionRealm find(long id) {
    return findById(realm, id, StatusReactionRealm.class);
  }

  @NonNull
  @Override
  public Observable<StatusReaction> observeById(long id) {
    final StatusReactionRealm statusReaction = find(id);
    if (statusReaction == null) {
      return Observable.empty();
    }
    return Observable.create(new Observable.OnSubscribe<StatusReaction>() {
      @Override
      public void call(final Subscriber<? super StatusReaction> subscriber) {
        RealmObject.addChangeListener(statusReaction, new RealmChangeListener<StatusReactionRealm>() {
          private boolean prevFaved = statusReaction.isFavorited();
          private boolean prevRTed = statusReaction.isRetweeted();

          @Override
          public void onChange(StatusReactionRealm element) {
            if (isIgnorableChange(element)) {
              return;
            }
            subscriber.onNext(element);
            prevFaved = element.isFavorited();
            prevRTed = element.isRetweeted();
          }

          private boolean isIgnorableChange(StatusReaction l) {
            return l.isFavorited() == prevFaved
                && l.isRetweeted() == prevRTed;
          }
        });
      }
    }).doOnUnsubscribe(new Action0() {
      @Override
      public void call() {
        RealmObject.removeChangeListeners(statusReaction);
      }
    });
  }

  @Override
  public void delete(final long id) {
    realm.executeTransactionAsync(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        realm.where(StatusReactionRealm.class)
            .equalTo(KEY_ID, id)
            .findAll()
            .deleteAllFromRealm();
      }
    });
  }
}
