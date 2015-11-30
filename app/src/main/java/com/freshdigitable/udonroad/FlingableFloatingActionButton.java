package com.freshdigitable.udonroad;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by akihit on 15/11/04.
 */
public class FlingableFloatingActionButton extends FloatingActionButton {
  private static final String TAG = FlingableFloatingActionButton.class.getSimpleName();

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
      flingListener.onFling(Direction.getDirection(e1, e2, velocityX, velocityY));
      return false;
    }
  };

  interface OnFlingListener {
    void onFling(Direction direction);
  }

  public enum Direction {
    UP(6), UP_RIGHT(7), RIGHT(0), DOWN_RIGHT(1), DOWN(2), DOWN_LEFT(3), LEFT(4), UP_LEFT(5), UNDEFINED(-1);
    final int index;

    Direction(int i) {
      index = i;
    }

    private static final float SWIPE_MIN_DISTANCE = 120;
    private static final float SWIPE_THRESH_VER = 200;
    private static final double ANGLE_DIVIDE = 2 * Math.PI / 8;
    static Direction getDirection(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
      double speed = Math.sqrt(velocityX * velocityX + velocityY * velocityY);
      double distX = e2.getX() - e1.getX();
      double distY = e2.getY() - e1.getY();
      double dist = Math.sqrt(distX * distX + distY * distY);
      if (speed < SWIPE_THRESH_VER || dist < SWIPE_MIN_DISTANCE) {
        return UNDEFINED;
      }
      double angle = atan3(velocityY, velocityX);
      for (Direction d : Direction.values()) {
        if (UNDEFINED.equals(d)) {
          continue;
        }
        if (RIGHT.equals(d)) {
          if (angle < ANGLE_DIVIDE / 2 || angle > 2 * Math.PI - ANGLE_DIVIDE / 2) {
            return d;
          }
        }
        double lowerThresh = d.index * ANGLE_DIVIDE - ANGLE_DIVIDE / 2;
        double upperThresh = d.index * ANGLE_DIVIDE + ANGLE_DIVIDE / 2;
        if (angle > lowerThresh && angle < upperThresh) {
          return d;
        }
      }
      return UNDEFINED;
    }

    private static double atan3(float y, float x) {
      double angle = Math.atan2(y, x);
      if (angle < 0) {
        return 2 * Math.PI + angle;
      }
      return angle;
    }
  }
}
