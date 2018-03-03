/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by akihit on 2018/02/18.
 */

public class StatefulItem<T> {
  public final @Nullable T item;
  public final State state;

  private StatefulItem(@Nullable T item, State state) {
    this.item = item;
    this.state = state;
  }

  public static <T> StatefulItem<T> loading(@Nullable T item) {
    return new StatefulItem<>(item, State.LOADING);
  }
  public static <T> StatefulItem<T> loaded(@NonNull T item) {
    return new StatefulItem<>(item, State.LOADED);
  }
  public static <T> StatefulItem<T> deleted(@Nullable T item) {
    return new StatefulItem<>(item, State.DELETED);
  }

  public enum State {
    LOADING, LOADED, DELETED
  }
}
