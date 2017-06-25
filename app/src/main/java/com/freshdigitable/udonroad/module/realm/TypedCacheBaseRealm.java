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

import io.reactivex.Completable;
import io.realm.Realm;

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
  public Completable observeUpsert(final Collection<T> entities) {
    if (entities == null || entities.isEmpty()) {
      return Completable.complete();
    }
    return observeUpsertImpl(cache, upsertTransaction(entities));
  }

  static Completable observeUpsertImpl(@NonNull final Realm cache,
                                       @NonNull final Realm.Transaction upsertTransaction) {
    return Completable.create(subscriber ->
        cache.executeTransactionAsync(
            upsertTransaction, subscriber::onComplete, subscriber::onError));
  }

  abstract Realm.Transaction upsertTransaction(Collection<T> entities);
}
