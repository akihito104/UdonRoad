/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import twitter4j.ExtendedMediaEntity;

/**
 * Created by akihit on 2016/07/10.
 */
public class MediaContainer extends LinearLayout {

  private int grid;

  public MediaContainer(Context context) {
    this(context, null);
  }

  public MediaContainer(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public MediaContainer(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    grid = getResources().getDimensionPixelSize(R.dimen.grid_margin);
  }

  private int thumbCount;
  private List<MediaImageView> thumbs = new ArrayList<>(4);

  public void setThumbCount(int count) {
    this.thumbCount = count;
    final int size = thumbs.size();
    if (count <= size) {
      return;
    }
    for (int i=0; i<size-count; i++) {
      thumbs.add(new MediaImageView(getContext()));
    }
  }

  public int getThumbCount() {
    return thumbCount;
  }

  private int mediaWidth;

  public int getMediaWidth() {
    return Math.max(mediaWidth, 0);
  }

  public void bindMediaEntities(ExtendedMediaEntity[] extendedMediaEntities) {
    final int mediaCount = Math.min(thumbs.size(), extendedMediaEntities.length);
    if (mediaCount < 1) {
      return;
    }
    mediaWidth = (getWidth() - grid * (mediaCount - 1)) / mediaCount;
    setVisibility(VISIBLE);
    for (int i = 0; i < mediaCount; i++) {
      thumbs.get(i).setVisibility(VISIBLE);
    }
  }
}
