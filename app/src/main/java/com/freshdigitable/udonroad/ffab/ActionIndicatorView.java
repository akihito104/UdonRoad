/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.ffab;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.ffab.OnFlingListener.Direction;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by akihit on 2016/07/04.
 */
public class ActionIndicatorView extends RelativeLayout {
  private Map<Direction, ImageView> icons;

  public ActionIndicatorView(Context context) {
    this(context, null);
  }

  public ActionIndicatorView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ActionIndicatorView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    final int padding = getResources().getDimensionPixelSize(R.dimen.grid_margin);
    setPadding(padding, padding, padding, padding);

    final View v = View.inflate(context, R.layout.view_action_indicator, this);
    final ImageView iconUp = (ImageView) v.findViewById(R.id.indicator_up);
    final ImageView iconRight = (ImageView) v.findViewById(R.id.indicator_right);
    final ImageView iconDown = (ImageView) v.findViewById(R.id.indicator_down);
    final ImageView iconLeft = (ImageView) v.findViewById(R.id.indicator_left);
    icons = new HashMap<>();
    icons.put(Direction.UP, iconUp);
    icons.put(Direction.RIGHT, iconRight);
    icons.put(Direction.DOWN, iconDown);
    icons.put(Direction.LEFT, iconLeft);
  }

  public void setDrawable(Direction direction, Drawable drawable) {
    icons.get(direction).setImageDrawable(drawable);
  }

  public void onActionSelected(Direction direction) {
    if (direction == Direction.UNDEFINED) {
      return;
    }
    for (ImageView ic : icons.values()) {
      ic.setVisibility(INVISIBLE);
    }

    if (direction.index % 2 == 0) {
      final ImageView icon = icons.get(direction);
      toOrigin(icon);
      scaleUp(direction, 2.0f);
      icon.setVisibility(VISIBLE);
    } else {
      if (direction == Direction.UP_RIGHT) {
        toSecondQuadrant(icons.get(Direction.UP));
        toForthQuadrant(icons.get(Direction.RIGHT));
      } else if (direction == Direction.DOWN_RIGHT) {
        toFirstQuadrant(icons.get(Direction.RIGHT));
        toThirdQuadrant(icons.get(Direction.DOWN));
      } else if (direction == Direction.DOWN_LEFT) {
        toSecondQuadrant(icons.get(Direction.LEFT));
        toForthQuadrant(icons.get(Direction.DOWN));
      } else if (direction == Direction.UP_LEFT) {
        toFirstQuadrant(icons.get(Direction.UP));
        toThirdQuadrant(icons.get(Direction.LEFT));
      }
      for (Direction d : direction.getBothNeighbor()) {
        scaleUp(d, 1.5f);
        icons.get(d).setVisibility(VISIBLE);
      }
    }
  }

  private void toOrigin(View ic) {
    final float dX = getPaddingLeft() + calcContentWidth() / 2 - calcCenterX(ic);
    final float dY = getPaddingTop() + calcContentHeight() / 2 - calcCenterY(ic);
    setTranslation(ic, dX, dY);
  }

  private void toFirstQuadrant(View ic) {
    final float dX = getPaddingLeft() + 3 * calcContentWidth() / 4 - calcCenterX(ic);
    final float dY = getPaddingTop() + calcContentHeight() / 4 - calcCenterY(ic);
    setTranslation(ic, dX, dY);
  }

  private void toSecondQuadrant(View ic) {
    final float dX = getPaddingLeft() + calcContentWidth() / 4 - calcCenterX(ic);
    final float dY = getPaddingTop() + calcContentHeight() / 4 - calcCenterY(ic);
    setTranslation(ic, dX, dY);
  }

  private void toThirdQuadrant(View ic) {
    final float dX = getPaddingLeft() + calcContentWidth() / 4 - calcCenterX(ic);
    final float dY = getPaddingTop() + 3 * calcContentHeight() / 4 - calcCenterY(ic);
    setTranslation(ic, dX, dY);
  }

  private void toForthQuadrant(View ic) {
    final float dX = getPaddingLeft() + 3 * calcContentWidth() / 4 - calcCenterX(ic);
    final float dY = getPaddingTop() + 3 * calcContentHeight() / 4 - calcCenterY(ic);
    setTranslation(ic, dX, dY);
  }

  public void onActionLeave(Direction direction) {
    if (direction == Direction.UNDEFINED) {
      return;
    }
    if (direction.index % 2 == 0) {
      resetViewTransforms(direction);
    } else {
      for (Direction d : direction.getBothNeighbor()) {
        resetViewTransforms(d);
      }
    }
    for (ImageView ic : icons.values()) {
      ic.setVisibility(VISIBLE);
    }
  }

  private void scaleUp(@NonNull Direction direction, float scale) {
    final ImageView icon = icons.get(direction);
    if (icon != null) {
      setScale(icon, scale);
    }
  }

  private void resetViewTransforms(@NonNull Direction direction) {
    final ImageView icon = icons.get(direction);
    if (icon != null) {
      setScale(icon, 1);
      setTranslation(icon, 0, 0);
    }
  }

  private float calcCenterY(View ic) {
    return ic.getY() + ic.getHeight() / 2;
  }

  private float calcCenterX(View ic) {
    return ic.getX() + ic.getWidth() / 2;
  }

  private float calcContentWidth() {
    return getWidth() - getPaddingRight() - getPaddingLeft();
  }

  private float calcContentHeight() {
    return getHeight() - getPaddingTop() - getPaddingBottom();
  }

  private void setScale(@NonNull ImageView icon, float scale) {
    ViewCompat.setScaleX(icon, scale);
    ViewCompat.setScaleY(icon, scale);
  }

  private void setTranslation(View ic, float dX, float dY) {
    ViewCompat.setTranslationX(ic, dX);
    ViewCompat.setTranslationY(ic, dY);
  }
}