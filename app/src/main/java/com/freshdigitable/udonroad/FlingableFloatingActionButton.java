package com.freshdigitable.udonroad;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by akihit on 15/11/04.
 */
public class FlingableFloatingActionButton extends FloatingActionButton {
  @SuppressWarnings("unused")
  private static final String TAG = FlingableFloatingActionButton.class.getSimpleName();

//  private final GestureDetector gestureDetector;

  public FlingableFloatingActionButton(Context context) {
    this(context, null);
  }

  public FlingableFloatingActionButton(Context context, AttributeSet attributeSet) {
    this(context, attributeSet, 0);
  }

  public FlingableFloatingActionButton(Context context, AttributeSet attributeSet, int defStyleAttr) {
    super(context, attributeSet, defStyleAttr);

//    GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
//      @Override
//      public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//        if (flingListener != null) {
//          flingListener.onFling(Direction.getDirection(e1, e2, velocityX, velocityY));
//          return true;
//        }
//        return false;
//      }
//    };
//    this.gestureDetector = new GestureDetector(context, gestureListener);
    this.setOnTouchListener(new View.OnTouchListener() {
      private MotionEvent old = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, getX(), getY(), 0);
      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {
        final int action = motionEvent.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
          old = MotionEvent.obtain(motionEvent);
        } else if (action == MotionEvent.ACTION_MOVE) {
          Log.d(TAG, "onTouch: " + Direction.getDirection(old, motionEvent));
          //TODO update UI
        } else if (action == MotionEvent.ACTION_UP) {
          if (flingListener != null) {
            flingListener.onFling(Direction.getDirection(old, motionEvent));
          }
          old.recycle();
          return true;
        }
//        return gestureDetector.onTouchEvent(motionEvent);
        return false;
      }
    });
    this.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
      }
    });
  }

  private OnFlingListener flingListener;

  public void setOnFlingListener(OnFlingListener listener) {
    this.flingListener = listener;
  }

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
    private static final double ANGLE_THRESHOLD = ANGLE_DIVIDE / 2;

    static Direction getDirection(MotionEvent e1, MotionEvent e2) {
      float distX = e2.getX() - e1.getX();
      float distY = e2.getY() - e1.getY();
      double dist = Math.sqrt(distX * distX + distY * distY);
      if (dist < SWIPE_MIN_DISTANCE) {
        return UNDEFINED;
      }
      double angle = atan3(distY, distX);
      for (Direction d : Direction.values()) {
        if (UNDEFINED.equals(d)) {
          continue;
        }
        if (RIGHT.equals(d)) {
          if (angle < ANGLE_THRESHOLD || angle > 2 * Math.PI - ANGLE_THRESHOLD) {
            return d;
          }
        }
        double lowerThresh = d.index * ANGLE_DIVIDE - ANGLE_THRESHOLD;
        double upperThresh = d.index * ANGLE_DIVIDE + ANGLE_THRESHOLD;
        if (angle > lowerThresh && angle < upperThresh) {
          return d;
        }
      }
      return UNDEFINED;
    }

    static Direction getDirection(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
      double speed = Math.sqrt(velocityX * velocityX + velocityY * velocityY);
      if (speed < SWIPE_THRESH_VER) {
        return UNDEFINED;
      }
      return getDirection(e1, e2);
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
