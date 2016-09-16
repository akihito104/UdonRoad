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

import android.support.annotation.Nullable;

import com.freshdigitable.udonroad.datastore.TypedCache;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import twitter4j.User;
import twitter4j.UserMentionEntity;

/**
 * UserCacheRealm implements UserCache with Realm.
 *
 * Created by akihit on 2016/09/14.
 */
public class UserCacheRealm extends BaseCacheRealm implements TypedCache<User> {
  @Override
  public void upsert(final User user) {
    cache.executeTransaction(new Realm.Transaction() {
      @Override
      public void execute(Realm realm) {
        realm.insertOrUpdate(new UserRealm(user));
      }
    });
  }

  @Override
  public void upsert(List<User> entities) {

  }

  @Override
  public void forceUpsert(User entity) {

  }

  void upsert(UserMentionEntity[] mentionEntity) {
    if (mentionEntity == null || mentionEntity.length < 1) {
      return;
    }
    for (UserMentionEntity ume : mentionEntity) {
      final User user = find(ume.getId());
      if (user == null) {
        upsert(new UserRealm(ume));
      }
    }
  }

  @Override
  @Nullable
  public User find(long id) {
    return cache.where(UserRealm.class)
        .equalTo("id", id)
        .findFirst();
  }

  @Override
  public Observable<User> observeById(final long userId) {
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

  @Override
  public void delete(long id) {

  }
}