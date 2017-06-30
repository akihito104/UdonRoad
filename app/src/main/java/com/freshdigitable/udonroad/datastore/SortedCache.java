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

package com.freshdigitable.udonroad.datastore;

import android.support.annotation.NonNull;

import io.reactivex.Flowable;
import io.reactivex.Observable;

/**
 * SortedCache defines to access storage of sorted data specified type parameter.
 *
 * Created by akihit on 2016/09/14.
 */
public interface SortedCache<T> extends BaseOperation<T>, NamingBaseCache {
  Flowable<UpdateEvent> observeUpdateEvent();

  T get(int position);

  int getItemCount();

  long getLastPageCursor();

  int getPositionById(long id);

  @NonNull
  Observable<T> observeById(long id);
}
