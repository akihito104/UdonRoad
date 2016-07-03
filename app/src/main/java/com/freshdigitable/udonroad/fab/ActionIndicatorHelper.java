/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.fab;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;
import android.widget.ImageView;

import com.freshdigitable.udonroad.R;

import java.util.HashMap;
import java.util.Map;

public class ActionIndicatorHelper implements OnFlingListener {
  private final ImageView indicator;

  public ActionIndicatorHelper(@NonNull ImageView indicator) {
    this.indicator = indicator;

    setIndicatorIcon(Direction.UP, R.drawable.ic_like);
    setIndicatorIcon(Direction.RIGHT, R.drawable.ic_retweet);

    defaultIndicatorIcon = createIndicatorIcon(R.drawable.ic_add_white_36dp);
  }

  private final Map<Direction, Drawable> indicatorIcon = new HashMap<>();
  private final Drawable defaultIndicatorIcon;

  private void setIndicatorIcon(Direction direction, @DrawableRes int drawable) {
    final Drawable d = createIndicatorIcon(drawable);
    indicatorIcon.put(direction, d);
  }

  @NonNull
  private Drawable createIndicatorIcon(@DrawableRes int drawable) {
    final Drawable d = ContextCompat.getDrawable(indicator.getContext(), drawable);
    DrawableCompat.setTint(d, Color.WHITE);
    return d;
  }

  @Override
  public void onStart() {
    indicator.setVisibility(View.VISIBLE);
  }

  @Override
  public void onFling(Direction direction) {
    indicator.setVisibility(View.INVISIBLE);
  }

  private Drawable old;

  @Override
  public void onMoving(Direction direction) {
    final Drawable icon = indicatorIcon.get(direction);
    if (old == icon) {
      return;
    }
    old = icon;
    if (icon != null) {
      indicator.setImageDrawable(icon);
    } else {
      indicator.setImageDrawable(defaultIndicatorIcon);
    }
  }
}
