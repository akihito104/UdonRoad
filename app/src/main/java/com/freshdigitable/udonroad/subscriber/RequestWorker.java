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

package com.freshdigitable.udonroad.subscriber;

import com.freshdigitable.udonroad.datastore.BaseCache;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;

/**
 * Created by akihit on 2017/06/26.
 */

public interface RequestWorker {
  OnIffabItemSelectedListener getOnIffabItemSelectedListener(long selectedId);

  final class Util {
    static <T, S extends BaseCache> void fetchToStore(
        Single<T> fetchTask,
        S store, BiConsumer<S, T> storeTask,
        Consumer<T> successFeedback, Consumer<Throwable> failedFeedback) {
      fetchTask.observeOn(AndroidSchedulers.mainThread()).subscribe(
          t -> {
            store.open();
            storeTask.accept(store, t);
            store.close();
            successFeedback.accept(t);
          },
          failedFeedback);

    }
  }
}
