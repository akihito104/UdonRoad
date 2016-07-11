/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import twitter4j.ExtendedMediaEntity;

/**
 * Created by akihit on 2016/07/10.
 */
public class MediaContainer extends LinearLayout {

  private final int grid;
  private final int maxThumbCount;

  private int thumbWidth;
  private int thumbCount;

  public MediaContainer(Context context) {
    this(context, null);
  }

  public MediaContainer(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public MediaContainer(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    grid = getResources().getDimensionPixelSize(R.dimen.grid_margin);

    final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MediaContainer, defStyleAttr, 0);
    try {
      maxThumbCount = a.getInt(R.styleable.MediaContainer_thumbCount, 0);
    } finally {
      a.recycle();
    }
    if (isInEditMode()) {
      setThumbCount(maxThumbCount);
    }
  }

  public int getThumbCount() {
    return thumbCount;
  }

  public int getThumbWidth() {
    return Math.max(thumbWidth, 0);
  }

  public void bindMediaEntities(ExtendedMediaEntity[] extendedMediaEntities) {
    final int thumbCount = Math.min(maxThumbCount, extendedMediaEntities.length);
    if (thumbCount < 1) {
      return;
    }
    setThumbCount(thumbCount);
    thumbWidth = (getWidth() - grid * (thumbCount - 1)) / thumbCount;
    setVisibility(VISIBLE);
    for (int i = 0; i < thumbCount; i++) {
      getChildAt(i).setVisibility(VISIBLE);
    }
  }

  private final LayoutParams lp = new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);

  private void setThumbCount(int count) {
    this.thumbCount = count;
    final int size = getChildCount();
    if (count <= size) {
      return;
    }
    for (int i = 0; i < count - size; i++) {
      final MediaImageView mediaImageView = new MediaImageView(getContext());
      if (size + i > 0) {
        lp.leftMargin = grid;
      }
      addView(mediaImageView, lp);
    }
  }

  public void reset() {
    setVisibility(GONE);
    for (int i = 0; i < thumbCount; i++) {
      final ImageView mi = (ImageView) getChildAt(i);
      mi.setImageDrawable(null);
      mi.setVisibility(GONE);
    }
    thumbCount = 0;
  }
}
