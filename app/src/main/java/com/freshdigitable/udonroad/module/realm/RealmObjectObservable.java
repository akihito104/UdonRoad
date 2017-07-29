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
import io.realm.RealmObject;

/**
 * Created by akihit on 2017/07/28.
 */
class RealmObjectObservable<T extends RealmModel> extends Observable<T> {
  static <T extends RealmModel> RealmObjectObservable<T> create(@NonNull T elem) {
    if (!RealmObject.isValid(elem)) {
      throw new IllegalStateException();
    }
    return new RealmObjectObservable<>(elem);
  }

  private final T elem;

  private RealmObjectObservable(T elem) {
    this.elem = elem;
  }

  @Override
  protected void subscribeActual(Observer<? super T> observer) {
    final RealmChangeListener<T> changeListener = e -> {
      observer.onNext(e);
      if (!RealmObject.isValid(e)) {
        observer.onComplete();
      }
    };
    observer.onSubscribe(new ChangeListenerDisposable<>(elem, changeListener));
    RealmObject.addChangeListener(elem, changeListener);
  }

  private static class ChangeListenerDisposable<T extends RealmModel> implements Disposable {
    private boolean disposed = false;
    private final T elem;
    private final RealmChangeListener<T> changeListener;

    private ChangeListenerDisposable(@NonNull T elem,
                                     @NonNull RealmChangeListener<T> changeListener) {
      this.elem = elem;
      this.changeListener = changeListener;
    }

    @Override
    public void dispose() {
      if (RealmObject.isValid(elem)) {
        RealmObject.removeChangeListener(elem, changeListener);
      }
      disposed = true;
    }

    @Override
    public boolean isDisposed() {
      return disposed;
    }
  }
}
