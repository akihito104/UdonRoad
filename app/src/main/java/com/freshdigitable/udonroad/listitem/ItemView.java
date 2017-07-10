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

package com.freshdigitable.udonroad.listitem;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.CallSuper;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.freshdigitable.udonroad.CombinedScreenNameTextView;
import com.freshdigitable.udonroad.R;

/**
 * StatusViewBase defines how to bind Status and StatusView.
 *
 * Created by akihit on 2016/06/28.
 */
public abstract class ItemView extends RelativeLayout {
  ImageView icon;
  CombinedScreenNameTextView names;
  TextView tweet;
  ReactionContainer reactionContainer;
  final int grid;
  final int selectedColor;

  public ItemView(Context context) {
    this(context, null);
  }

  public ItemView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ItemView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    grid = getGrid();
    setPadding(grid, grid, grid, grid);
    setBackgroundColor(Color.TRANSPARENT);
    selectedColor = ContextCompat.getColor(context, R.color.twitter_action_normal_transparent);
  }

  protected int getGrid() {
    return getResources().getDimensionPixelSize(R.dimen.grid_margin);
  }

  @CallSuper
  public void bind(ListItem item) {
    if (item == null) {
      return;
    }
    names.setNames(item.getUser());
    tweet.setText(item.getText());
    reactionContainer.update(item.getStats());
  }

  @CallSuper
  public void reset() {
    setBackgroundColor(Color.TRANSPARENT);

    icon.setImageDrawable(null);
    icon.setImageResource(android.R.color.transparent);
    icon.setOnClickListener(null);
    setOnClickListener(null);
  }

  String formatString(@StringRes int id, Object... items) {
    final String format = getResources().getString(id);
    return String.format(format, items);
  }

  public ImageView getIcon() {
    return icon;
  }

  public abstract void setSelectedColor();

  public abstract void setUnselectedColor();

  public TextView getUserName() {
    return names;
  }
}