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

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.freshdigitable.udonroad.ffab.OnFlingListener.Direction;

/**
 * FlingibleFloatingActionButton accepts only fling action.<br>
 * It indicates action icon on succeeding user's fling action.
 *
 * Created by akihit on 15/11/04.
 */
class FlingableFAB extends FloatingActionButton {
  @SuppressWarnings("unused")
  private static final String TAG = FlingableFAB.class.getSimpleName();

  public FlingableFAB(Context context) {
    this(context, null);
  }

  public FlingableFAB(Context context, AttributeSet attributeSet) {
    this(context, attributeSet, 0);
  }

  public FlingableFAB(Context context, AttributeSet attributeSet, int defStyleAttr) {
    super(context, attributeSet, defStyleAttr);
  }

  private MotionEvent old;

  @Override
  public boolean onTouchEvent(MotionEvent motionEvent) {
    if (flingListener == null) {
      return super.onTouchEvent(motionEvent);
    }
    final int action = motionEvent.getAction();
    if (action == MotionEvent.ACTION_DOWN) {
      old = MotionEvent.obtain(motionEvent);
      flingListener.onStart();
      return true;
    }
    final Direction direction = Direction.getDirection(old, motionEvent);
    if (action == MotionEvent.ACTION_MOVE) {
//          Log.d(TAG, "onTouch: " + direction);
      flingListener.onMoving(direction);
      return true;
    } else if (action == MotionEvent.ACTION_UP) {
      flingListener.onFling(direction);
      old.recycle();
      return true;
    }
    return super.onTouchEvent(motionEvent);
  }

  private OnFlingListener flingListener;

  public void setOnFlingListener(OnFlingListener listener) {
    this.flingListener = listener;
  }
}