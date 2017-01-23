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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.realm.Realm;
import rx.Observable;
import twitter4j.User;
import twitter4j.UserMentionEntity;

/**
 * UserCacheRealm implements UserCache with Realm.
 *
 * Created by akihit on 2016/09/14.
 */
public class UserCacheRealm extends TypedCacheBaseRealm<User> {
  public UserCacheRealm() {
    this(null);
  }

  UserCacheRealm(BaseCacheRealm baseCacheRealm) {
    super(baseCacheRealm);
  }

  @Override
  public void upsert(final User user) {
    if (user == null) {
      return;
    }
    upsert(Collections.singletonList(user));
  }

  @Override
  public void upsert(final List<User> entities) {
    if (entities == null || entities.isEmpty()) {
      return;
    }
    cache.executeTransaction(upsertTransaction(entities));
  }

  @NonNull
  @Override
  public Realm.Transaction upsertTransaction(final Collection<User> entities) {
    return realm -> {
      final List<UserRealm> upserts = new ArrayList<>(entities.size());
      for (User u : entities) {
        final UserRealm user = findById(realm, u.getId(), UserRealm.class);
        if (user == null) {
          upserts.add(new UserRealm(u));
        } else {
          user.merge(u, realm);
        }
      }
      realm.insertOrUpdate(upserts);
    };
  }

  @Override
  public void insert(final User entity) {
    cache.executeTransaction(realm -> realm.insertOrUpdate(new UserRealm(entity)));
  }

  void upsert(UserMentionEntity[] mentionEntity) {
    if (mentionEntity == null || mentionEntity.length < 1) {
      return;
    }
    final List<UserRealm> upserts = new ArrayList<>(mentionEntity.length);
    for (UserMentionEntity ume : mentionEntity) {
      final User user = find(ume.getId());
      if (user == null) {
        upserts.add(new UserRealm(ume));
      }
    }
    if (!upserts.isEmpty()) {
      cache.executeTransaction(realm -> realm.insertOrUpdate(upserts));
    }
  }

  @Override
  @Nullable
  public User find(long id) {
    return findById(cache, id, UserRealm.class);
  }

  @NonNull
  @Override
  public Observable<User> observeById(final long userId) {
    final UserRealm user = findById(cache, userId, UserRealm.class);
    if (user == null) {
      return Observable.empty();
    }
    return Observable.create(
        (Observable.OnSubscribe<User>) subscriber -> UserRealm.addChangeListener(user, subscriber::onNext))
        .doOnUnsubscribe(() -> UserRealm.removeChangeListeners(user));
  }

  @Override
  public void delete(long id) {
    //todo
  }
}
