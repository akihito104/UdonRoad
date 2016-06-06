package com.freshdigitable.udonroad.fab;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.fab.OnFlingListener.Direction;

/**
 * FlingibleFloatingActionButton accepts only fling action.<br>
 * It indicates action icon on succeeding user's fling action.
 *
 * Created by akihit on 15/11/04.
 */
public class FlingableFloatingActionButton extends LinearLayout {
  @SuppressWarnings("unused")
  private static final String TAG = FlingableFloatingActionButton.class.getSimpleName();
  private FloatingActionButton fab;

  public FlingableFloatingActionButton(Context context) {
    this(context, null);
  }

  public FlingableFloatingActionButton(Context context, AttributeSet attributeSet) {
    this(context, attributeSet, 0);
  }

  public FlingableFloatingActionButton(Context context, AttributeSet attributeSet, int defStyleAttr) {
    super(context, attributeSet, defStyleAttr);
    final View v = View.inflate(context, R.layout.view_fling_fab, this);
    actionIndicator = (ImageView) v.findViewById(R.id.fab_indicator);
    this.actionIndicatorHelper = new ActionIndicatorHelper(actionIndicator);

    fab = (FloatingActionButton) v.findViewById(R.id.fab);
    fab.hide();
    fab.setOnTouchListener(new View.OnTouchListener() {
      private MotionEvent old;

      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {
        if (flingListener == null) {
          return false;
        }
        final int action = motionEvent.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
          old = MotionEvent.obtain(motionEvent);
          if (actionIndicator != null) {
            actionIndicator.setVisibility(VISIBLE);
          }
          return false;
        }
        final Direction direction = Direction.getDirection(old, motionEvent);
        if (action == MotionEvent.ACTION_MOVE) {
          Log.d(TAG, "onTouch: " + direction);
          if (actionIndicatorHelper != null) {
            actionIndicatorHelper.onFling(direction);
          }
          return false;
        } else if (action == MotionEvent.ACTION_UP) {
          flingListener.onFling(direction);
          old.recycle();
          if (actionIndicator != null) {
            actionIndicator.setVisibility(INVISIBLE);
          }
          return true;
        }
        return false;
      }
    });
    fab.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
      }
    });
  }

  private OnFlingListener flingListener;

  public void setOnFlingListener(OnFlingListener listener) {
    this.flingListener = listener;
  }

  private ImageView actionIndicator;

  private OnFlingListener actionIndicatorHelper;

  public FloatingActionButton getFab() {
    return fab;
  }
}
