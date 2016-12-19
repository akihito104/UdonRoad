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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * RoundedCornerImageView is acceptable custom shape.
 *
 * Created by akihit on 2016/09/22.
 */

public class RoundedCornerImageView extends AppCompatImageView {
  private final Paint masker;
  private final Paint copier;
  private final Drawable maskDrawable;

  public RoundedCornerImageView(Context context) {
    this(context, null);
  }

  public RoundedCornerImageView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public RoundedCornerImageView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    masker = new Paint();
    masker.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
    copier = new Paint();
    final TypedArray a = context.obtainStyledAttributes(attrs,
        R.styleable.RoundedCornerImageView,
        defStyleAttr, R.style.Widget_RoundedCornerImageView);
    try {
      maskDrawable = a.getDrawable(R.styleable.RoundedCornerImageView_maskerShape);
    } finally {
      a.recycle();
    }
  }

  private Rect bound;
  private RectF boundF;

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    bound = new Rect(getPaddingLeft(), getPaddingTop(),
        w - getPaddingRight(), h - getPaddingBottom());
    boundF = new RectF(bound);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    final int saved = canvas.saveLayer(boundF, copier,
        Canvas.HAS_ALPHA_LAYER_SAVE_FLAG | Canvas.FULL_COLOR_LAYER_SAVE_FLAG);
    maskDrawable.setBounds(bound);
    maskDrawable.draw(canvas);
    canvas.saveLayer(boundF, masker, 0);
    super.onDraw(canvas);
    canvas.restoreToCount(saved);
  }
}
