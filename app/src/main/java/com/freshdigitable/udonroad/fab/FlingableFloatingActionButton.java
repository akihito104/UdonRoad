package com.freshdigitable.udonroad.fab;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.freshdigitable.udonroad.fab.OnFlingListener.Direction;

/**
 * Created by akihit on 15/11/04.
 */
public class FlingableFloatingActionButton extends FloatingActionButton {
  @SuppressWarnings("unused")
  private static final String TAG = FlingableFloatingActionButton.class.getSimpleName();

  public FlingableFloatingActionButton(Context context) {
    this(context, null);
  }

  public FlingableFloatingActionButton(Context context, AttributeSet attributeSet) {
    this(context, attributeSet, 0);
  }

  public FlingableFloatingActionButton(Context context, AttributeSet attributeSet, int defStyleAttr) {
    super(context, attributeSet, defStyleAttr);

    this.setOnTouchListener(new View.OnTouchListener() {
      private MotionEvent old;
      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {
        if (flingListener == null) {
          return false;
        }
        final int action = motionEvent.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
          old = MotionEvent.obtain(motionEvent);
        } else if (action == MotionEvent.ACTION_MOVE) {
          Log.d(TAG, "onTouch: " + Direction.getDirection(old, motionEvent));
          //TODO update UI
        } else if (action == MotionEvent.ACTION_UP) {
          flingListener.onFling(Direction.getDirection(old, motionEvent));
          old.recycle();
          return true;
        }
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
}
