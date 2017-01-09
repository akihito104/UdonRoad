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
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.ffab.OnFlingListener.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * IndicatableFFAB is a view group of FlingableFAB and ActionIndicatorView.
 *
 * Created by akihit on 2016/09/05.
 */
@CoordinatorLayout.DefaultBehavior(IndicatableFFAB.Behavior.class)
public class IndicatableFFAB extends FrameLayout {
  private final ActionIndicatorView indicator;
  private final FlingableFAB ffab;
  private final List<Direction> enableDirections = new ArrayList<>();
  private int indicatorMargin;

  public IndicatableFFAB(Context context) {
    this(context, null);
  }

  public IndicatableFFAB(Context context, AttributeSet attrs) {
    this(context, attrs, R.attr.iffabStyle);
  }

  public IndicatableFFAB(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    final View v = View.inflate(context, R.layout.view_indicatable_ffab, this);
    indicator = (ActionIndicatorView) v.findViewById(R.id.iffab_indicator);
    ffab = (FlingableFAB) v.findViewById(R.id.iffab_ffab);
    ViewCompat.setElevation(indicator, ffab.getCompatElevation());

    final TypedArray a = context.obtainStyledAttributes(attrs,
        R.styleable.IndicatableFFAB, defStyleAttr, R.style.Widget_FFAB_IndicatableFFAB);
    try {
      final Drawable fabIcon = a.getDrawable(R.styleable.IndicatableFFAB_fabIcon);
      ffab.setImageDrawable(fabIcon);
      final int fabTint = a.getColor(R.styleable.IndicatableFFAB_fabTint, NO_ID);
      if (fabTint != NO_ID) {
        ViewCompat.setBackgroundTintList(ffab, ColorStateList.valueOf(fabTint));
      }
      final int indicatorTint = a.getColor(R.styleable.IndicatableFFAB_indicatorTint, 0);
      indicator.setBackgroundColor(indicatorTint);
      indicatorMargin = a.getDimensionPixelSize(R.styleable.IndicatableFFAB_marginFabToIndicator, 0);
    } finally {
      a.recycle();
    }

    ffab.setOnTouchListener(new OnTouchListener() {
      private MotionEvent old;

      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {
        final int action = motionEvent.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
          old = MotionEvent.obtain(motionEvent);
          onStart();
          return false;
        }
        final Direction direction = Direction.getDirection(old, motionEvent);
        if (action == MotionEvent.ACTION_MOVE) {
          onMoving(direction);
        } else if (action == MotionEvent.ACTION_UP) {
          old.recycle();
          onFling(view.getHandler());
        }
        return false;
      }

      private Direction prevSelected = Direction.UNDEFINED;

      public void onStart() {
        indicator.onActionLeave(prevSelected);
        prevSelected = Direction.UNDEFINED;
        indicator.setVisibility(View.VISIBLE);
      }

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

      public void onFling(Handler handler) {
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {
            indicator.setVisibility(View.INVISIBLE);
          }
        }, 200);
      }
    });

    if (isInEditMode()) {
      indicator.setVisibility(VISIBLE);
    }
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    setIndicatorMargin();
  }

  private void setIndicatorMargin() {
    if (indicator.getVisibility() != VISIBLE) {
      return;
    }
    final ViewGroup.MarginLayoutParams layoutParams = (MarginLayoutParams) indicator.getLayoutParams();
    final MarginLayoutParams ffabLp = (MarginLayoutParams) ffab.getLayoutParams();
    layoutParams.bottomMargin = indicatorMargin + ffab.getHeight() + ffabLp.bottomMargin;
  }

  public void setIndicatorIcon(@NonNull Direction direction, @Nullable Drawable drawable) {
    if (drawable != null) {
      indicator.setDrawable(direction, drawable);
    }
    addEnableDirection(direction);
  }

  public void addEnableDirection(Direction direction) {
    enableDirections.add(direction);
  }

  public void removeEnableDirection(Direction direction) {
    enableDirections.remove(direction);
  }

  public void hide() {
    ffab.hide();
    setVisibility(INVISIBLE);
  }

  public void show() {
    setVisibility(VISIBLE);
    ffab.show();
  }

  public void setOnFlingListener(OnFlingListener flingListener) {
    ffab.setOnFlingListener(flingListener);
  }

  public void clear() {
    indicator.clear();
    enableDirections.clear();
    setOnFlingListener(null);
  }

  public static class Behavior extends CoordinatorLayout.Behavior<IndicatableFFAB> {
    public Behavior() {
      super();
    }

    public Behavior(Context context, AttributeSet attrs) {
      super(context, attrs);
    }

    @Override
    public void onAttachedToLayoutParams(@NonNull CoordinatorLayout.LayoutParams params) {
      params.dodgeInsetEdges |= Gravity.BOTTOM;
    }
  }
}
