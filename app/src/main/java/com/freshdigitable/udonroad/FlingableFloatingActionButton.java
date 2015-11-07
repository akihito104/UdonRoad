package com.freshdigitable.udonroad;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by akihit on 15/11/04.
 */
public class FlingableFloatingActionButton extends FloatingActionButton {
  private static final String TAG = FlingableFloatingActionButton.class.getName();

  private final GestureDetector gestureDetector;

  public FlingableFloatingActionButton(Context context) {
    this(context, null);
  }

  public FlingableFloatingActionButton(Context context, AttributeSet attributeSet) {
    super(context, attributeSet);
    this.gestureDetector = new GestureDetector(context, this.gestureListener);
    this.setOnTouchListener(new View.OnTouchListener(){
      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {
        return gestureDetector.onTouchEvent(motionEvent);
      }
    });
  }

  private static final float SWIPE_MIN_DISTANCE = 120;
  private static final float SWIPE_THRESH_VER = 200;
  private OnFlingListener flingListener;

  public void setOnFlingListener(OnFlingListener listener) {
    this.flingListener = listener;
  }

  private final GestureDetector.SimpleOnGestureListener gestureListener
      = new GestureDetector.SimpleOnGestureListener() {
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
      if (flingListener == null) {
        return false;
      }

      if (Math.abs(velocityX) > SWIPE_THRESH_VER) {
        if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE) {
          flingListener.onFling(Direction.LEFT);
          return false;
        } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE) {
          flingListener.onFling(Direction.RIGHT);
          return false;
        }
      }
      if (Math.abs(velocityY) > SWIPE_THRESH_VER) {
        if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE) {
          flingListener.onFling(Direction.UP);
          return false;
        } else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE) {
          flingListener.onFling(Direction.DOWN);
          return false;
        }
      }
      return false;
    }
  };

  interface OnFlingListener {
    void onFling(Direction direction);
  }

  public enum Direction {
    UP, UP_RIGHT, RIGHT, DOWN_RIGHT, DOWN, DOWN_LEFT, LEFT, UP_LEFT,
  }
}
