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
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

/**
 * IconAttachedTextView shows text with icon at first.
 *
 * Created by akihit on 2016/12/15.
 */

public class IconAttachedTextView extends AppCompatTextView {

  public IconAttachedTextView(Context context) {
    this(context, null);
  }

  public IconAttachedTextView(Context context, AttributeSet attrs) {
    this(context, attrs, R.attr.iconAttachedTextViewStyle);
  }

  public IconAttachedTextView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    final TypedArray a = context.obtainStyledAttributes(attrs,
        R.styleable.IconAttachedTextView, defStyleAttr, R.style.Widget_IconAttachedTextView);
    try {
      int iconRes = a.getResourceId(R.styleable.IconAttachedTextView_icon, -1);
      final Drawable icon = iconRes > 0 ? AppCompatResources.getDrawable(context, iconRes)
          : a.getDrawable(R.styleable.IconAttachedTextView_icon);
      final int iconColor = a.getColor(R.styleable.IconAttachedTextView_tintIcon, NO_ID);
      setIcon(icon, iconColor);
    } finally {
      a.recycle();
    }
  }

  public void setIcon(@DrawableRes int icon) {
    setIcon(AppCompatResources.getDrawable(getContext(), icon), NO_ID);
  }

  public void setIcon(Drawable icon, int iconColor) {
    if (icon != null) {
      icon = icon.mutate();
      final int width
          = icon.getIntrinsicWidth() * getLineHeight() / icon.getIntrinsicHeight();
      icon.setBounds(0, 0, width, getLineHeight());
      if (iconColor != NO_ID) {
        DrawableCompat.setTint(icon, iconColor);
      }
      setCompoundDrawables(icon, null, null, null);
    }
  }

  public void tintIcon(@ColorRes int colorRes) {
    for (Drawable d : getCompoundDrawables()) {
      if (d == null) {
        continue;
      }
      DrawableCompat.setTint(d, ContextCompat.getColor(getContext(), colorRes));
    }
  }
}
