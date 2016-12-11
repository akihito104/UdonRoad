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

package com.freshdigitable.udonroad;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.view.View;

import rx.functions.Action0;

/**
 * Created by akihit on 2016/07/14.
 */
public class SnackBarUtil {

  public static Snackbar create(@NonNull final View root, final CharSequence text) {
    return create(root, text, Snackbar.LENGTH_SHORT);
  }

  public static Snackbar create(@NonNull View root, CharSequence text, int length) {
    return Snackbar.make(root, text, length);
  }

  public static Snackbar create(@NonNull View root, @StringRes int resId) {
    return Snackbar.make(root, resId, Snackbar.LENGTH_SHORT);
  }

  public static void show(@NonNull final View root, final CharSequence text) {
    create(root, text).show();
  }

  public static void show(@NonNull final View root, final @StringRes int text) {
    create(root, text).show();
  }

  public static Action0 action(@NonNull final View root, final CharSequence text) {
    return new Action0() {
      @Override
      public void call() {
        show(root, text);
      }
    };
  }

  public static Action0 action(@NonNull final View root, final @StringRes int text) {
    return new Action0() {
      @Override
      public void call() {
        show(root, text);
      }
    };
  }
  private SnackBarUtil() {
  }
}
