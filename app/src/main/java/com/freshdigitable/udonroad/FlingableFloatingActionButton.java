package com.freshdigitable.udonroad;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by akihit on 15/11/04.
 */
public class FlingableFloatingActionButton {
  private static final String TAG = FlingableFloatingActionButton.class.getName();

  private final FloatingActionButton floatingActionButton;
  private final GestureDetector gestureDetector;

  public FlingableFloatingActionButton(FloatingActionButton floatingActionButton) {
    this.floatingActionButton = floatingActionButton;
    this.gestureDetector = new GestureDetector(floatingActionButton.getContext(), this.gestureListener);

    this.floatingActionButton.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {
        return gestureDetector.onTouchEvent(motionEvent);
      }
    });
    this.floatingActionButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent intent = new Intent(view.getContext(), TweetActivity_.class);
        view.getContext().startActivity(intent);
      }
    });
  }

  private static final float SWIPE_MIN_DISTANCE = 120;
  private static final float SWIPE_THRESH_VER = 200;

  private final GestureDetector.SimpleOnGestureListener gestureListener
      = new GestureDetector.SimpleOnGestureListener() {
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
      if (Math.abs(velocityX) > SWIPE_THRESH_VER) {
        if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE) {
          Log.d(TAG, "fling to left.");
          return false;
        } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE) {
          Log.d(TAG, "fling to right.");
          return false;
        }
      }
      if (Math.abs(velocityY) > SWIPE_THRESH_VER) {
        if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE) {
          Log.d(TAG, "fling to up");
          return false;
        } else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE) {
          Log.d(TAG, "fling to down");
          return false;
        }
      }
      return false;
    }
  };
}
