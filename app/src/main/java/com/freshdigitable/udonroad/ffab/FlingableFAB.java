package com.freshdigitable.udonroad.ffab;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.freshdigitable.udonroad.ffab.OnFlingListener.Direction;

/**
 * FlingibleFloatingActionButton accepts only fling action.<br>
 * It indicates action icon on succeeding user's fling action.
 * <p/>
 * Created by akihit on 15/11/04.
 */
public class FlingableFAB extends FloatingActionButton {
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
    setOnTouchListener(new View.OnTouchListener() {
      private MotionEvent old;

      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {
        if (flingListener == null) {
          return false;
        }
        final int action = motionEvent.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
          old = MotionEvent.obtain(motionEvent);
          flingListener.onStart();
          if (actionIndicatorHelper != null) {
            actionIndicatorHelper.onStart();
          }
          return false;
        }
        final Direction direction = Direction.getDirection(old, motionEvent);
        if (action == MotionEvent.ACTION_MOVE) {
//          Log.d(TAG, "onTouch: " + direction);
          flingListener.onMoving(direction);
          if (actionIndicatorHelper != null) {
            actionIndicatorHelper.onMoving(direction);
          }
          return false;
        } else if (action == MotionEvent.ACTION_UP) {
          flingListener.onFling(direction);
          old.recycle();
          if (actionIndicatorHelper != null) {
            actionIndicatorHelper.onFling(direction);
          }
          return true;
        }
        return false;
      }
    });
    setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
      }
    });
  }

  private OnFlingListener flingListener;

  public void setOnFlingListener(OnFlingListener listener) {
    this.flingListener = listener;
  }

  private OnFlingListener actionIndicatorHelper;

  public void setActionIndicatorHelper(OnFlingListener actionIndicatorHelper) {
    this.actionIndicatorHelper = actionIndicatorHelper;
  }
}