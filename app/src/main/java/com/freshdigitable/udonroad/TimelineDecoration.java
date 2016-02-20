/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;


import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

class TimelineDecoration extends RecyclerView.ItemDecoration {
  private final Paint paint;
  private final int dividerHeight;

  TimelineDecoration() {
    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setColor(Color.GRAY);
    this.dividerHeight = 1;
  }

  @Override
  public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
    int position = ((RecyclerView.LayoutParams) view.getLayoutParams()).getViewLayoutPosition();
    int top = position == 0 ? 0 : dividerHeight;
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
          + ViewCompat.getTranslationY(child);
      final float bottom = top + dividerHeight;
      c.drawRect(left, top, right, bottom, paint);
    }
  }
}
