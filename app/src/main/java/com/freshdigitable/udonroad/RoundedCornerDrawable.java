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

package com.freshdigitable.udonroad;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import static android.os.Build.VERSION.*;
import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * RoundedCornerDrawable is wrapper class to make the corners of drawable rounded.
 *
 * Created by akihit on 2016/12/19.
 */

class RoundedCornerDrawable extends Drawable {
  private final Paint masker;
  private final Paint copier;
  private final Drawable maskDrawable;
  private final Drawable drawable;

  RoundedCornerDrawable(@NonNull Drawable maskerDrawable, @NonNull Drawable drawable) {
    this.maskDrawable = maskerDrawable;
    this.drawable = drawable;
    masker = new Paint();
    masker.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
    copier = new Paint();
  }

  private RectF boundF;

  @Override
  protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);
    boundF = new RectF(bounds);
  }

  @Override
  public void draw(@NonNull Canvas canvas) {
    final int saved = SDK_INT >= LOLLIPOP ?
        canvas.saveLayer(boundF, copier)
        : canvas.saveLayer(boundF, copier, Canvas.ALL_SAVE_FLAG);
    maskDrawable.draw(canvas);
    canvas.saveLayer(boundF, masker, 0);
    drawable.draw(canvas);
    canvas.restoreToCount(saved);
  }

  @Override
  public void setBounds(int left, int top, int right, int bottom) {
    super.setBounds(left, top, right, bottom);
    maskDrawable.setBounds(left, top, right, bottom);
    drawable.setBounds(left, top, right, bottom);
  }

  @Override
  public void setBounds(@NonNull Rect bounds) {
    setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
  }

  @Override
  public void setAlpha(@IntRange(from = 0, to = 255) int i) {
    drawable.setAlpha(i);
  }

  @Override
  public void setColorFilter(@Nullable ColorFilter colorFilter) {
    drawable.setColorFilter(colorFilter);
  }

  @Override
  public int getOpacity() {
    return drawable.getOpacity();
  }
}
