/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad.fab;

import android.view.MotionEvent;

/**
 * Created by akihit on 2016/03/22.
 */
public interface OnFlingListener {
  void onFling(Direction direction);

  enum Direction {
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