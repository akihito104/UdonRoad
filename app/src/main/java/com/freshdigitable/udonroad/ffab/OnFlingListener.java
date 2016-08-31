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

package com.freshdigitable.udonroad.ffab;

import android.view.MotionEvent;

/**
 * OnFlingListener defines callback method of fling event of FlingableFAB.
 *
 * Created by akihit on 2016/03/22.
 */
public interface OnFlingListener {
  void onStart();

  void onMoving(Direction direction);

  void onFling(Direction direction);


  enum Direction {
    UP(6), UP_RIGHT(7), RIGHT(0), DOWN_RIGHT(1), DOWN(2), DOWN_LEFT(3), LEFT(4), UP_LEFT(5), UNDEFINED(-1);
    public final int index;

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

    public Direction[] getBothNeighbor() {
      if (this == UNDEFINED) {
        return new Direction[0];
      }
      return new Direction[]{
          getWithIndex((index + 1) % 8),
          getWithIndex((index - 1) < 0 ? 7 : index - 1)
      };
    }

    private static Direction getWithIndex(int i) {
      for (Direction d : values()) {
        if (d.index == i) {
          return d;
        }
      }
      return UNDEFINED;
    }

    public boolean isOnAxis() {
      return index >= 0 && index % 2 == 0;
    }
  }
}
