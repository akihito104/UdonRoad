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

import com.freshdigitable.udonroad.datastore.TypedCache;

import java.util.Collection;

import io.realm.Realm;
import rx.Observable;
import rx.Subscriber;

/**
 * TypedCacheBaseRealm is basis class for implemented TypedCache interface.
 *
 * Created by akihit on 2016/11/10.
 */

abstract class TypedCacheBaseRealm<T> extends BaseCacheRealm implements TypedCache<T> {

  TypedCacheBaseRealm() {
    this(null);
  }

  TypedCacheBaseRealm(BaseCacheRealm baseCacheRealm) {
    super(baseCacheRealm);
  }

  @Override
  public Observable<Void> observeUpsert(final Collection<T> entities) {
    if (entities == null || entities.isEmpty()) {
      return Observable.empty();
    }
    return observeUpsertImpl(cache, upsertTransaction(entities));
  }

  static Observable<Void> observeUpsertImpl(@NonNull final Realm cache,
                                            @NonNull final Realm.Transaction upsertTransaction) {
    return Observable.create(new Observable.OnSubscribe<Void>() {
      @Override
      public void call(final Subscriber<? super Void> subscriber) {
        cache.executeTransactionAsync(upsertTransaction,
            new Realm.Transaction.OnSuccess() {
              @Override
              public void onSuccess() {
                subscriber.onNext(null);
                subscriber.onCompleted();
              }
            }, new Realm.Transaction.OnError() {
              @Override
              public void onError(Throwable error) {
                subscriber.onError(error);
              }
            });
      }
    });
  }

  abstract Realm.Transaction upsertTransaction(Collection<T> entities);
}
