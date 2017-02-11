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

package com.freshdigitable.udonroad.ffab;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.ffab.OnFlickListener.Direction;

import java.util.HashMap;
import java.util.Map;

/**
 * ActionIndicatorView shows menu icon of FlickableFAB.
 *
 * Created by akihit on 2016/07/04.
 */
class ActionIndicatorView extends FrameLayout {
  private final Map<Direction, ImageView> icons = new HashMap<>();
  private final Map<Direction, Drawable> drawables = new HashMap<>();

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
    icons.put(Direction.UP, iconUp);
    icons.put(Direction.RIGHT, iconRight);
    icons.put(Direction.DOWN, iconDown);
    icons.put(Direction.LEFT, iconLeft);
  }

  public void setDrawable(Direction direction, Drawable drawable) {
    tintIcon(drawable, this.indicatorIconTint);
    drawables.put(direction, drawable);
  }

  @Override
  protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
    super.onVisibilityChanged(changedView, visibility);
    if (changedView == this && visibility == VISIBLE) {
      for (Direction d : icons.keySet()) {
        icons.get(d).setImageDrawable(drawables.get(d));
      }
    }
  }

  public void clear() {
    drawables.clear();
    for (ImageView iv : icons.values()) {
      iv.setImageDrawable(null);
    }
  }

  public void onActionSelected(Direction direction) {
    if (direction == Direction.UNDEFINED) {
      return;
    }
    for (ImageView ic : icons.values()) {
      ic.setVisibility(INVISIBLE);
    }

    if (direction.isOnAxis()) {
      translationTo(direction, TransCoefs.ORIGIN);
    } else {
      final Drawable drawable = drawables.get(direction);
      if (drawable != null) {
        final Direction neighbor = direction.getBothNeighbor()[0];
        icons.get(neighbor).setImageDrawable(drawable);
        translationTo(neighbor, TransCoefs.ORIGIN);
        return;
      }
      if (direction == Direction.UP_RIGHT) {
        translationTo(Direction.UP, TransCoefs.SECOND_QUAD);
        translationTo(Direction.RIGHT, TransCoefs.FORTH_QUAD);
      } else if (direction == Direction.DOWN_RIGHT) {
        translationTo(Direction.RIGHT, TransCoefs.FIRST_QUAD);
        translationTo(Direction.DOWN, TransCoefs.THIRD_QUAD);
      } else if (direction == Direction.DOWN_LEFT) {
        translationTo(Direction.LEFT, TransCoefs.SECOND_QUAD);
        translationTo(Direction.DOWN, TransCoefs.FORTH_QUAD);
      } else if (direction == Direction.UP_LEFT) {
        translationTo(Direction.UP, TransCoefs.FIRST_QUAD);
        translationTo(Direction.LEFT, TransCoefs.THIRD_QUAD);
      }
    }
  }

  private void translationTo(Direction direction, TransCoefs coefs) {
    translationTo(icons.get(direction), coefs);
  }

  private void translationTo(View ic, TransCoefs coefs) {
    final float dX = getPaddingLeft() + coefs.xCoef * calcContentWidth() - calcCenterX(ic);
    final float dY = getPaddingTop() + coefs.yCoef * calcContentHeight() - calcCenterY(ic);
    setTranslation(ic, dX, dY);
    setScale(ic, coefs.scale);
    ic.setVisibility(VISIBLE);
  }

  public void onActionLeave(Direction direction) {
    if (direction == Direction.UNDEFINED) {
      return;
    }
    if (direction.isOnAxis()) {
      resetViewTransforms(direction);
    } else {
      for (Direction d : direction.getBothNeighbor()) {
        resetViewTransforms(d);
      }
    }
    for (Direction d : icons.keySet()) {
      icons.get(d).setImageDrawable(drawables.get(d));
    }
    for (ImageView ic : icons.values()) {
      ic.setVisibility(VISIBLE);
    }
  }

  private void resetViewTransforms(@NonNull Direction direction) {
    final ImageView icon = icons.get(direction);
    if (icon != null) {
      setScale(icon, 1);
      setTranslation(icon, 0, 0);
    }
  }

  private static float calcCenterY(View ic) {
    return ic.getY() + ic.getHeight() / 2;
  }

  private static float calcCenterX(View ic) {
    return ic.getX() + ic.getWidth() / 2;
  }

  private float calcContentWidth() {
    return getWidth() - getPaddingRight() - getPaddingLeft();
  }

  private float calcContentHeight() {
    return getHeight() - getPaddingTop() - getPaddingBottom();
  }

  private static void setScale(@NonNull View icon, float scale) {
    ViewCompat.setScaleX(icon, scale);
    ViewCompat.setScaleY(icon, scale);
  }

  private static void setTranslation(View ic, float dX, float dY) {
    ViewCompat.setTranslationX(ic, dX);
    ViewCompat.setTranslationY(ic, dY);
  }

  private int indicatorIconTint;

  public void setIndicatorIconTint(int indicatorIconTint) {
    this.indicatorIconTint = indicatorIconTint;
    for (Drawable d : drawables.values()) {
      tintIcon(d, this.indicatorIconTint);
    }
  }

  private static void tintIcon(Drawable drawable, @ColorInt int color) {
    if (drawable == null) {
      return;
    }
    DrawableCompat.setTint(drawable, color);
  }

  private enum TransCoefs {
    ORIGIN(0.5f, 0.5f, 2f),
    FIRST_QUAD(0.75f, 0.25f, 1.5f), SECOND_QUAD(0.25f, 0.25f, 1.5f),
    THIRD_QUAD(0.25f, 0.75f, 1.5f), FORTH_QUAD(0.75f, 0.75f, 1.5f),;

    final float xCoef;
    final float yCoef;
    final float scale;

    TransCoefs(float xCoef, float yCoef, float scale) {
      this.xCoef = xCoef;
      this.yCoef = yCoef;
      this.scale = scale;
    }
  }
}