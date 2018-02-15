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

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.TypedCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.realm.Realm;
import twitter4j.User;

/**
 * UserCacheRealm implements UserCache with Realm.
 *
 * Created by akihit on 2016/09/14.
 */
public class UserCacheRealm implements TypedCache<User> {
  private final PoolRealm pool;

  public UserCacheRealm(AppSettingStore appSettingStore) {
    this(new PoolRealm(appSettingStore));
  }

  UserCacheRealm(PoolRealm pool) {
    this.pool = pool;
  }

  @Override
  public void upsert(final User user) {
    if (user == null) {
      return;
    }
    upsert(Collections.singletonList(user));
  }

  @Override
  public void upsert(final Collection<User> entities) {
    if (entities == null || entities.isEmpty()) {
      return;
    }
    pool.executeTransaction(upsertTransaction(entities));
  }

  @NonNull
  Realm.Transaction upsertTransaction(final Collection<User> entities) {
    return r -> {
      final List<UserRealm> upserts = new ArrayList<>(entities.size());
      for (User u : entities) {
        final UserRealm user = CacheUtil.findById(r, u.getId(), UserRealm.class);
        if (user == null) {
          upserts.add(u instanceof UserRealm ? ((UserRealm) u) : new UserRealm(u));
        } else {
          user.merge(u, r);
        }
      }
      r.insertOrUpdate(upserts);
    };
  }

  @Override
  public void insert(final User entity) {
    pool.executeTransaction(r -> r.insertOrUpdate(new UserRealm(entity)));
  }

  @Override
  @Nullable
  public User find(long id) {
    return pool.findById(id, UserRealm.class);
  }

  @NonNull
  @Override
  public Observable<User> observeById(final long userId) {
    return pool.observeById(userId, UserRealm.class).cast(User.class);
  }

  @NonNull
  @Override
  public Observable<? extends User> observeById(User element) {
    return pool.observeById(element.getId(), UserRealm.class);
  }

  @Override
  public Completable observeUpsert(Collection<User> entities) {
    if (entities == null || entities.isEmpty()) {
      return Completable.complete();
    }
    return pool.observeUpsertImpl(upsertTransaction(entities));
  }

  @Override
  public void delete(long id) {
    //todo
  }

  @Override
  public void open() {
    pool.open();
  }

  @Override
  public void clear() {
    pool.clear();
  }

  @Override
  public void close() {
    pool.close();
  }

  @Override
  public void drop() {
    pool.drop();
  }
}
