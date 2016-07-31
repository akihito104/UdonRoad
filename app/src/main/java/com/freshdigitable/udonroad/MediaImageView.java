/*
 * Copyright (c) 2016. Akihito Matsuda (akihito104)
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

package com.freshdigitable.udonroad;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * Created by akihit on 2016/07/10.
 */
public class MediaImageView extends AppCompatImageView {
  public static final String TAG = MediaImageView.class.getSimpleName();
  private Bitmap playIcon;

  public MediaImageView(Context context) {
    this(context, null);
  }

  public MediaImageView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public MediaImageView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    setContentDescription(getResources().getString(R.string.tweet_media_descs));
    setVisibility(GONE);

    final Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.ld_play_icon);
    playIcon = Bitmap.createBitmap(
        drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(playIcon);
    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    drawable.draw(canvas);
  }

  private boolean showIcon = false;

  public void setShowIcon(boolean showIcon) {
    this.showIcon = showIcon;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (showIcon) {
      int left = (getWidth() - playIcon.getWidth()) / 2;
      int top = (getHeight() - playIcon.getHeight()) / 2;
      canvas.drawBitmap(playIcon, left, top, null);
    }
  }
}
