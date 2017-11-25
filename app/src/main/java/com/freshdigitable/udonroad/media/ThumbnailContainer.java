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

package com.freshdigitable.udonroad.media;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.freshdigitable.udonroad.R;

import twitter4j.MediaEntity;

/**
 * MediaContainer is view group contains image thumbnails.
 *
 * Created by akihit on 2016/07/10.
 */
public class ThumbnailContainer extends LinearLayout {

  private final int grid;
  private final int maxThumbCount;

  private int thumbWidth;
  private int thumbCount;

  public ThumbnailContainer(Context context) {
    this(context, null);
  }

  public ThumbnailContainer(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ThumbnailContainer(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    grid = getResources().getDimensionPixelSize(R.dimen.grid_margin);

    final TypedArray a = context.obtainStyledAttributes(
        attrs, R.styleable.ThumbnailContainer, defStyleAttr, 0);
    try {
      maxThumbCount = a.getInt(R.styleable.ThumbnailContainer_thumbCount, 0);
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

  public void bindMediaEntities(MediaEntity[] mediaEntities) {
    final int mediaCount = mediaEntities.length;
    bindMediaEntities(mediaCount);
  }

  public void bindMediaEntities(int mediaCount) {
    final int thumbCount = Math.min(maxThumbCount, mediaCount);
    if (thumbCount < 1) {
      setThumbCount(0);
      return;
    }
    setThumbCount(thumbCount);
    thumbWidth = calcThumbWidth();
    setVisibility(VISIBLE);
    for (int i = 0; i < thumbCount; i++) {
      final int num = i;
      final View mediaView = getChildAt(i);
      mediaView.setVisibility(VISIBLE);
      mediaView.setOnClickListener(view -> {
        if (mediaClickListener == null) {
          return;
        }
        mediaClickListener.onMediaClicked(view, num);
      });
    }
  }

  private int calcThumbWidth() {
    return thumbCount > 0 ? (getWidth() - grid * (thumbCount - 1)) / thumbCount : 0;
  }

  private void setThumbCount(int count) {
    this.thumbCount = count;
    final int size = getChildCount();
    if (count < size) {
      for (int i = size - 1; i >= count; i--) {
        final View v = getChildAt(i);
        v.setVisibility(GONE);
      }
      return;
    }
    for (int i = 0; i < count - size; i++) {
      final ThumbnailView thumbnailView = new ThumbnailView(getContext());
      final LayoutParams lp = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1);
      if (size + i >= 1) {
        lp.leftMargin = grid;
      }
      addView(thumbnailView, -1, lp);
    }
  }

  public void reset() {
    setVisibility(GONE);
    for (int i = 0; i < thumbCount; i++) {
      final ImageView mi = (ImageView) getChildAt(i);
      mi.setOnClickListener(null);
      mi.setVisibility(GONE);
    }
    thumbCount = 0;
  }

  private OnMediaClickListener mediaClickListener;

  public void setOnMediaClickListener(OnMediaClickListener mediaClickListener) {
    this.mediaClickListener = mediaClickListener;
  }

  public interface OnMediaClickListener {
    void onMediaClicked(View view, int index);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    thumbWidth = calcThumbWidth();
  }
}
