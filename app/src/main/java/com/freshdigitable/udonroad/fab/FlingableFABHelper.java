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
import android.support.v4.view.ViewCompat;
import android.view.View;

import com.freshdigitable.udonroad.R;

import java.util.ArrayList;
import java.util.List;

public class FlingableFABHelper implements OnFlingListener {
  private final ActionIndicatorView indicator;
  private final FlingableFAB fab;
  private List<Direction> enableDirections = new ArrayList<>();

  public FlingableFABHelper(@NonNull ActionIndicatorView indicator,
                            @NonNull FlingableFAB fab) {
    this.indicator = indicator;
    this.fab = fab;
    fab.setActionIndicatorHelper(this);
    ViewCompat.setElevation(this.indicator, this.fab.getCompatElevation());

    setIndicatorIcon(Direction.UP, R.drawable.ic_like);
    setIndicatorIcon(Direction.RIGHT, R.drawable.ic_retweet);
    addEnableDirection(Direction.UP_RIGHT);
  }

  private void setIndicatorIcon(Direction direction, @DrawableRes int drawableId) {
    final Drawable d = createIndicatorIcon(drawableId);
    indicator.setDrawable(direction, d);
    addEnableDirection(direction);
  }

  public void addEnableDirection(Direction direction) {
    enableDirections.add(direction);
  }

  public void removeEnableDirection(Direction direction) {
    enableDirections.remove(direction);
  }

  @NonNull
  private Drawable createIndicatorIcon(@DrawableRes int drawable) {
    final Drawable d = ContextCompat.getDrawable(indicator.getContext(), drawable);
    DrawableCompat.setTint(d, Color.WHITE);
    return d;
  }

  @Override
  public void onStart() {
    indicator.onActionLeave(prevSelected);
    prevSelected = Direction.UNDEFINED;
    indicator.setVisibility(View.VISIBLE);
  }

  private Direction prevSelected = Direction.UNDEFINED;

  @Override
  public void onMoving(Direction direction) {
    if (prevSelected == direction) {
      return;
    }
    indicator.onActionLeave(prevSelected);
    if (isDirectionEnabled(direction)) {
      indicator.onActionSelected(direction);
    }
    prevSelected = direction;
  }

  private boolean isDirectionEnabled(Direction direction) {
    for (Direction d : enableDirections) {
      if (d == direction) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void onFling(Direction direction) {
    indicator.getHandler().postDelayed(new Runnable() {
      @Override
      public void run() {
        indicator.setVisibility(View.INVISIBLE);
      }
    }, 200);
  }

  public FlingableFAB getFab() {
    return fab;
  }

}