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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

class TimelineDecoration extends RecyclerView.ItemDecoration {
  private final Paint paint;
  private final int dividerHeight;

  TimelineDecoration(Context context) {
    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final int dividerColor = ContextCompat.getColor(context, R.color.divider);
    paint.setColor(dividerColor);
    this.dividerHeight = 1;
  }

  @Override
  public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
    int position = ((RecyclerView.LayoutParams) view.getLayoutParams()).getViewLayoutPosition();
    int top = position == 0 ? dividerHeight : 0;
    outRect.set(0, top, 0, 0);
  }

  @Override
  public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
    super.onDraw(c, parent, state);
    final float left = parent.getPaddingLeft();
    final float right = parent.getWidth() - parent.getPaddingRight();
    final int childCount = parent.getChildCount();

    final RecyclerView.LayoutManager manager = parent.getLayoutManager();
    for (int i = 0; i < childCount; i++) {
      final View child = parent.getChildAt(i);
      final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
      if (params.getViewLayoutPosition() == 0) {
        continue;
      }
      final float top = manager.getDecoratedTop(child) - params.topMargin
          + Math.round(ViewCompat.getTranslationY(child));
      final float bottom = top + dividerHeight;
      c.drawRect(left, top, right, bottom, paint);
    }
  }
}
