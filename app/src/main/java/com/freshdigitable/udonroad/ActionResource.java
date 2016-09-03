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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;

/**
 * ActionResource defines resources such as icon drawable or title string for user action.
 *
 * Created by akihit on 2016/09/02.
 */
public enum ActionResource {
  FAV(R.drawable.ic_like),
  RETWEET(R.drawable.ic_retweet),
  QUOTE(R.drawable.ic_quote),
  REPLY(R.drawable.ic_reply),
  MENU(R.drawable.ic_menu);

  final
  @DrawableRes
  int iconResId;

  ActionResource(@DrawableRes int iconResId) {
    this.iconResId = iconResId;
  }

  public Drawable createDrawable(@NonNull Context context) {
    return ContextCompat.getDrawable(context, iconResId);
  }

  public Drawable createDrawableWithColor(@NonNull Context context, @ColorInt int color) {
    final Drawable drawable = createDrawable(context);
    DrawableCompat.setTint(drawable, color);
    return drawable;
  }
}
