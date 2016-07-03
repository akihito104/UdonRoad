/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.fab;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.fab.OnFlingListener.Direction;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by akihit on 2016/07/03.
 */
public class FlingableFAB extends FloatingActionButton {

  private Map<Direction, ImageView> actionIcons;
  private int iconSize;

  public FlingableFAB(Context context) {
    this(context, null);
  }

  public FlingableFAB(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public FlingableFAB(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    actionIcons = new HashMap<>(Direction.values().length - 1);
    iconSize = context.getResources().getDimensionPixelSize(R.dimen.small_user_icon);
    final ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(iconSize, iconSize);
    for (Direction d : Direction.values()) {
      if (d == Direction.UNDEFINED) {
        continue;
      }
      final ImageView v = new ImageView(context);
      v.setLayoutParams(lp);
      actionIcons.put(d, v);
    }
  }

  public void setActionIcon(Direction direction, Drawable actionIcon) {
    DrawableCompat.setTint(actionIcon, Color.WHITE);
    actionIcons.get(direction).setImageDrawable(actionIcon);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    int width = (right - left) - (getPaddingLeft() + getPaddingRight());
//    int height = (bottom - top) - (getPaddingTop() + getPaddingBottom());

    int l = getPaddingLeft();
    int t = getPaddingTop();

    int radial = width / 2;
    int centerX = l + radial;
    int centerY = t + radial;
    double iconCenterDist = radial - iconSize / Math.sqrt(2);

    for (Map.Entry<Direction, ImageView> s : actionIcons.entrySet()) {
      final Direction d = s.getKey();
      final double x = iconCenterDist * Math.cos(2 * Math.PI * d.index / 8);
      final double y = iconCenterDist * Math.sin(2 * Math.PI * d.index / 8);
      s.getValue().layout((int) (centerX - x), (int) (centerY - y),
          (int) (centerX + x), (int) (centerY + y));
    }
  }
}
