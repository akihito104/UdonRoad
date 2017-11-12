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

import android.content.res.ColorStateList;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.widget.TextView;

import io.reactivex.disposables.Disposable;
import twitter4j.Status;

/**
 * Utils is utility method class.
 *
 * Created by akihit on 2016/12/09.
 */

public class Utils {
  @SuppressWarnings("unchecked")
  public static <T extends Status> T getBindingStatus(T status) {
    return status.isRetweet() ? (T) status.getRetweetedStatus() : status;
  }

  public static void colorStateLinkify(TextView textView) {
    final ColorStateList linkStateList
        = ContextCompat.getColorStateList(textView.getContext(), R.color.selector_link_text);
    textView.setMovementMethod(ColorStateLinkMovementMethod.getInstance(linkStateList));
  }

  public static void maybeDispose(@Nullable Disposable disposable) {
    if (disposable != null && !disposable.isDisposed()) {
      disposable.dispose();
    }
  }

  public static boolean isSubscribed(@Nullable Disposable disposable) {
    return disposable != null && !disposable.isDisposed();
  }

  private Utils() {}
}
