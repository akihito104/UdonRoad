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

package com.freshdigitable.udonroad.util;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.espresso.IdlingResource;
import android.support.v7.widget.RecyclerView;


/**
 * StreamIdlingResource is to use waiting for updated RecyclerView by twitter stream.
 *
 * Created by akihit on 2016/10/01.
 */
public class StreamIdlingResource implements IdlingResource {
  private final int expectedCount;
  private final RecyclerView.Adapter adapter;

  public StreamIdlingResource(@NonNull RecyclerView recyclerView,
                              @NonNull Operation op,
                              @IntRange(from = 1) int count) {
    adapter = recyclerView.getAdapter();
    expectedCount = op.apply(adapter.getItemCount(), count);
  }

  @Override
  public boolean isIdleNow() {
    final int itemCount = adapter.getItemCount();
    final boolean isIdleNow = itemCount == expectedCount;
    if (isIdleNow && callback != null) {
      callback.onTransitionToIdle();
    }
    return isIdleNow;
  }

  @Nullable
  private ResourceCallback callback;

  @Override
  public void registerIdleTransitionCallback(ResourceCallback callback) {
    this.callback = callback;
  }

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  public enum Operation {
    ADD {
      @Override
      int apply(int o1, int o2) {
        return o1 + o2;
      }
    }, DELETE {
      @Override
      int apply(int o1, int o2) {
        return o1 - o2;
      }
    },;

    abstract int apply(int o1, int o2);
  }
}
