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

package com.freshdigitable.udonroad.subscriber;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.freshdigitable.udonroad.datastore.BaseOperation;
import com.freshdigitable.udonroad.datastore.SortedCache;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;

import rx.functions.Action0;
import rx.functions.Action1;
import rx.subjects.PublishSubject;

/**
 * RequestWorkerBase is a base class of request worker.
 *
 * Created by akihit on 2016/11/03.
 */
public abstract class RequestWorkerBase<T extends BaseOperation<?>> {
  TwitterApi twitterApi;
  T cache;
  PublishSubject<Integer> userFeedback;

  RequestWorkerBase(@NonNull TwitterApi twitterApi,
                    @NonNull T cache,
                    @NonNull PublishSubject<Integer> userFeedback) {
    this.twitterApi = twitterApi;
    this.cache = cache;
    this.userFeedback = userFeedback;
  }

  @CallSuper
  public void open() {
    if (cache instanceof SortedCache) {
      throw new IllegalArgumentException("SortedCache should be called open(String).");
    }
    cache.open();
  }

  @CallSuper
  public void open(@NonNull String name) {
    if (cache instanceof TypedCache) {
      throw new IllegalArgumentException("TypedCache should be called open()");
    }
    ((SortedCache) cache).open(name);
  }

  public T getCache() {
    return cache;
  }

  public abstract void close();

  @NonNull
  Action1<Throwable> onErrorFeedback(@StringRes final int msg) {
    return new Action1<Throwable>() {
      @Override
      public void call(Throwable throwable) {
        userFeedback.onNext(msg);
      }
    };
  }

  @NonNull
  Action0 onCompleteFeedback(@StringRes final int msg) {
    return new Action0() {
      @Override
      public void call() {
        userFeedback.onNext(msg);
      }
    };
  }
}
