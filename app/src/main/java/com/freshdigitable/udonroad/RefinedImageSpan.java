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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.style.ImageSpan;

import java.lang.ref.WeakReference;

/**
 * RefinedImageSpan makes settling image fine position in line.
 *
 * Created by akihit on 2016/12/17.
 */

class RefinedImageSpan extends ImageSpan {
  RefinedImageSpan(Context context, Bitmap b) {
    super(context, b);
  }

  RefinedImageSpan(Drawable d) {
    super(d);
  }

  RefinedImageSpan(Drawable d, int verticalAlignment) {
    super(d, verticalAlignment);
  }

  @Override
  public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
    final Rect bounds = getCachedDrawable().getBounds();
    if (fm != null) {
      if (getVerticalAlignment() == ALIGN_BASELINE) {
        fm.ascent = -bounds.bottom;
        fm.descent = 0;
      } else if (getVerticalAlignment() == ALIGN_BOTTOM) {
        fm.ascent = (int) (bounds.bottom * (float) fm.top / (-fm.top + fm.bottom));
        fm.descent = bounds.bottom + fm.ascent;
      }
      fm.top = fm.ascent;
      fm.bottom = fm.descent;
    }
    return bounds.right;
  }

  @Override
  public void draw(Canvas canvas,
                   CharSequence text, int start, int end,
                   float x, int top, int baseline, int bottom,
                   Paint paint) {
    final Drawable cachedDrawable = getCachedDrawable();
    canvas.save();
    int transY = 0;
    if (getVerticalAlignment() == ALIGN_BASELINE) {
      transY = baseline - cachedDrawable.getBounds().bottom;
    } else if (getVerticalAlignment() == ALIGN_BOTTOM) {
      transY = bottom - cachedDrawable.getBounds().bottom;
    }
    canvas.translate(x, transY);
    cachedDrawable.draw(canvas);
    canvas.restore();
  }

  private WeakReference<Drawable> cachedDrawable;

  @NonNull
  private Drawable getCachedDrawable() {
    final WeakReference<Drawable> cd = this.cachedDrawable;
    Drawable drawable = null;
    if (cd != null) {
      drawable = cd.get();
    }
    if (drawable == null) {
      drawable = getDrawable();
      cachedDrawable = new WeakReference<>(drawable);
    }
    return drawable;
  }
}
