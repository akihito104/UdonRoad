/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.fab;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.freshdigitable.udonroad.fab.OnFlingListener.Direction;

public class SimpleFlingListenerHelper implements OnTouchListener {
  private final GestureDetector gestureDetector;

  public SimpleFlingListenerHelper(
      @NonNull Context context,
      @NonNull final OnFlingListener flingListener) {
    this.gestureDetector = new GestureDetector(context,
        new GestureDetector.SimpleOnGestureListener() {
          @Override
          public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            flingListener.onFling(Direction.getDirection(e1, e2, velocityX, velocityY));
            return true;
          }
        });
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    return gestureDetector.onTouchEvent(event);
  }
}
