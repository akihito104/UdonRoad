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
  static final int ALIGN_CENTER = 10;
  RefinedImageSpan(Context context, Bitmap b) {
    super(context, b);
  }

  RefinedImageSpan(Context context, Bitmap b, int verticalAlignment) {
    super(context, b, verticalAlignment);
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
      final int verticalAlignment = getVerticalAlignment();
      if (verticalAlignment == ALIGN_BASELINE) {
        fm.ascent = -bounds.bottom;
      } else if (verticalAlignment == ALIGN_BOTTOM) {
        fm.ascent = bounds.bottom * fm.top / (-fm.top + fm.bottom);
      } else if (verticalAlignment == ALIGN_CENTER) {
        fm.ascent = fm.top - (bounds.bottom - (fm.bottom - fm.top)) / 2;
      }
      fm.descent = Math.max(bounds.bottom + fm.ascent, 0);
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
    final int drawableHeight = cachedDrawable.getBounds().bottom;

    canvas.save();
    final int transY;
    final int verticalAlignment = getVerticalAlignment();
    if (verticalAlignment == ALIGN_BASELINE) {
      transY = baseline - drawableHeight;
    } else if (verticalAlignment == ALIGN_BOTTOM) {
      transY = bottom - drawableHeight - paint.getFontMetricsInt().leading;
    } else if (verticalAlignment == ALIGN_CENTER) {
      final float center = baseline + (paint.getFontMetrics().bottom + paint.getFontMetrics().top) / 2;
      transY = (int) (center - drawableHeight / 2);
    } else {
      transY = top;
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
