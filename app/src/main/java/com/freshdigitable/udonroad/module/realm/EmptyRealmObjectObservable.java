/*
 * Copyright (c) 2017. Matsuda, Akihit (akihito104)
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

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.realm.RealmChangeListener;
import io.realm.RealmModel;
import io.realm.RealmResults;

/**
 * Created by akihit on 2017/08/01.
 */

class EmptyRealmObjectObservable<T extends RealmModel> extends Observable<T> {
  static <T extends RealmModel> Observable<T> create(@NonNull RealmResults<T> realmResult) {
    return new EmptyRealmObjectObservable<>(realmResult);
  }
  private final RealmResults<T> realmResult;

  private EmptyRealmObjectObservable(RealmResults<T> realmResult) {
    this.realmResult = realmResult;
  }

  @Override
  protected void subscribeActual(Observer<? super T> observer) {
    final RealmChangeListener<RealmResults<T>> changeListener = ts -> {
      if (!ts.isEmpty()) {
        observer.onNext(ts.first());
      }
    };
    observer.onSubscribe(new ChangeListenerDisposable<>(realmResult, changeListener));
    realmResult.addChangeListener(changeListener);
  }

  private static class ChangeListenerDisposable<T extends RealmModel> implements Disposable {
    private boolean disposed = false;
    private final RealmResults<T> elem;
    private final RealmChangeListener<RealmResults<T>> changeListener;

    private ChangeListenerDisposable(@NonNull RealmResults<T> elem,
                                     @NonNull RealmChangeListener<RealmResults<T>> changeListener) {
      this.elem = elem;
      this.changeListener = changeListener;
    }

    @Override
    public void dispose() {
      if (!elem.isValid()) {
        disposed = true;
        return;
      }
      elem.removeChangeListener(changeListener);
      disposed = true;
    }

    @Override
    public boolean isDisposed() {
      return disposed;
    }
  }
}
