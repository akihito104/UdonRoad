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

package com.freshdigitable.udonroad.ffab;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.freshdigitable.udonroad.R;

/**
 * BottomButtonsToolbar shows menu items that has state.
 *
 * Created by akihit on 2017/02/19.
 */
class BottomButtonsToolbar extends Toolbar {
  private LinearLayout menuContainer;

  public BottomButtonsToolbar(Context context) {
    this(context, null);
  }

  public BottomButtonsToolbar(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public BottomButtonsToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    final int color = ResourcesCompat.getColor(getResources(), R.color.accent, context.getTheme());
    setBackgroundColor(color);

    menuContainer = new LinearLayout(context, attrs, defStyleAttr);
    menuContainer.setOrientation(LinearLayout.HORIZONTAL);
    final LayoutParams layoutParams = new LayoutParams(MarginLayoutParams.MATCH_PARENT, MarginLayoutParams.MATCH_PARENT);
    addView(menuContainer, layoutParams);
  }

  @Override
  protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
    return super.generateLayoutParams(p);
  }

  void setDrawable(int order, Drawable drawable) {
    final AppCompatImageView iv = new AppCompatImageView(getContext());
    iv.setImageDrawable(drawable);
    menuContainer.addView(iv);
  }

  static int getHeight(Context context) {
    final TypedValue tv = new TypedValue();
    return context.getTheme().resolveAttribute(android.support.v7.appcompat.R.attr.actionBarSize, tv, true)
        ? TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics())
        : MarginLayoutParams.WRAP_CONTENT;
  }
}
