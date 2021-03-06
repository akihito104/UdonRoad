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

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.realm.Realm;
import io.realm.RealmModel;
import io.realm.RealmResults;

/**
 * CacheUtil is utility class for the package.
 *
 * Created by akihit on 2016/11/10.
 */

class CacheUtil {
  static Completable observeUpsertImpl(@NonNull final Realm cache,
                                       @NonNull final Realm.Transaction upsertTransaction) {
    return !cache.isClosed() ?
        Completable.create(subscriber -> cache.executeTransactionAsync(
            upsertTransaction, subscriber::onComplete, subscriber::onError))
        : Completable.complete();
  }

  static <T extends RealmModel> T findById(Realm realm, long id, Class<T> clz) {
    return realm.where(clz)
        .equalTo("id", id)
        .findFirst();
  }

  static <T extends RealmModel> Observable<T> observeById(Realm realm, long id, Class<T> clz) {
    final RealmResults<T> elem = realm.where(clz)
        .equalTo("id", id)
        .findAll();
    return EmptyRealmObjectObservable.create(elem);
  }

  private CacheUtil() {}
}
