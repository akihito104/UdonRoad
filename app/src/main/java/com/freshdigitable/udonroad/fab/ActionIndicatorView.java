/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.fab;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.fab.OnFlingListener.Direction;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by akihit on 2016/07/04.
 */
public class ActionIndicatorView extends RelativeLayout {

  private ImageView iconSelected;
  private Map<Direction, ImageView> icons;

  public ActionIndicatorView(Context context) {
    this(context, null);
  }

  public ActionIndicatorView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ActionIndicatorView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    final View v = View.inflate(context, R.layout.view_action_indicator, this);
    final ImageView iconUp = (ImageView) v.findViewById(R.id.indicator_up);
    final ImageView iconRight = (ImageView) v.findViewById(R.id.indicator_right);
    final ImageView iconDown = (ImageView) v.findViewById(R.id.indicator_down);
    final ImageView iconLeft = (ImageView) v.findViewById(R.id.indicator_left);
    iconSelected = (ImageView) v.findViewById(R.id.indicator_selected);
    icons = new HashMap<>();
    icons.put(Direction.UP, iconUp);
    icons.put(Direction.RIGHT, iconRight);
    icons.put(Direction.DOWN, iconDown);
    icons.put(Direction.LEFT, iconLeft);
  }

  public void setSelectedIconVisible() {
    iconSelected.setVisibility(VISIBLE);
    for (ImageView v : icons.values()) {
      v.setVisibility(GONE);
    }
  }

  public void setActionIconVisible() {
    iconSelected.setVisibility(GONE);
    for (ImageView v : icons.values()) {
      v.setVisibility(VISIBLE);
    }
  }

  public void setDrawable(Direction direction, Drawable drawable) {
    icons.get(direction).setImageDrawable(drawable);
  }

  public ImageView getSelectedIcon() {
    return iconSelected;
  }
}