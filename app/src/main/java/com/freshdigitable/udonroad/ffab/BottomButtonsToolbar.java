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
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.freshdigitable.udonroad.R;

import java.util.Collections;
import java.util.List;

/**
 * BottomButtonsToolbar shows menu items that has state.
 *
 * Created by akihit on 2017/02/19.
 */
public class BottomButtonsToolbar extends LinearLayout {

  private ImageView more;

  public BottomButtonsToolbar(Context context) {
    this(context, null);
  }

  public BottomButtonsToolbar(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public BottomButtonsToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    final Resources resources = context.getResources();
    final int color = ResourcesCompat.getColor(resources, R.color.accent, context.getTheme());
    setBackgroundColor(color);

    setOrientation(LinearLayout.HORIZONTAL);
    final LayoutParams layoutParams = new LayoutParams(
        MarginLayoutParams.MATCH_PARENT, MarginLayoutParams.MATCH_PARENT);
    setLayoutParams(layoutParams);

    iconPadding = resources.getDimensionPixelSize(R.dimen.iffab_toolbar_icon_padding);

    more = new AppCompatImageView(context);
    more.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_expand_white_24dp));
    more.setLayoutParams(ICON_LAYOUT_PARAMS);
    more.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
  }

  private final int iconPadding;
  public static final LinearLayout.LayoutParams ICON_LAYOUT_PARAMS
      = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
  private IffabMenu menu;

  void setMenu(IffabMenu menu) {
    this.menu = menu;
    updateItems();
  }

  void updateItems() {
    removeView(more);
    final List<IffabMenuItem> visibleItems = this.menu.getVisibleItems();
    Collections.sort(visibleItems, (r, l) -> r.getOrder() - l.getOrder());
    for (int i = 0; i < visibleItems.size(); i++) {
      final int maxIconIndex = getChildCount() - 1;
      final IffabMenuItem item = visibleItems.get(i);
      if (i > maxIconIndex) {
        addMenuItem(item);
      } else {
        setMenuItem((ImageView) getChildAt(i), item);
      }
    }
    final int removedCount = getChildCount() - visibleItems.size();
    if (removedCount > 0) {
      removeViews(visibleItems.size(), removedCount);
    }
    addView(more);
  }

  View getMoreIcon() {
    return more;
  }

  private void addMenuItem(IffabMenuItem item) {
    final AppCompatImageView iv = new AppCompatImageView(getContext());
    iv.setLayoutParams(ICON_LAYOUT_PARAMS);
    iv.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
    setMenuItem(iv, item);
    addView(iv);
  }

  private void setMenuItem(ImageView iv, IffabMenuItem item) {
    final Drawable icon = item.getTintedIcon();
    iv.setImageDrawable(icon);
    iv.setImageState(item.parseToState(), false);
    final int itemId = item.getItemId();
    iv.setId(itemId);
    iv.setOnClickListener(v -> {
      if (item.isCheckable()) {
        menu.dispatchSelectedMenuItem(itemId);
      }
    });
  }

  void setMoreClickListener(View.OnClickListener listener) {
    more.setOnClickListener(listener);
  }

  void clear() {
    final int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      final View child = getChildAt(i);
      child.setOnClickListener(null);
    }
    removeAllViews();
  }

  static int getHeight(Context context) {
    final TypedValue tv = new TypedValue();
    return context.getTheme().resolveAttribute(
        android.support.v7.appcompat.R.attr.actionBarSize, tv, true) ?
        TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics())
        : MarginLayoutParams.WRAP_CONTENT;
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    clearAnimation();
  }
}
