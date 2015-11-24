package com.freshdigitable.udonroad;

import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.View;

/**
 * Created by akihit on 2015/11/21.
 */
public class TimelineAnimator extends SimpleItemAnimator {
  private static final String TAG = TimelineAnimator.class.getSimpleName();

  @Override
  public boolean animateRemove(RecyclerView.ViewHolder holder) {
    Log.d(TAG, "animateRemove");
    return false;
  }

  @Override
  public boolean animateAdd(RecyclerView.ViewHolder holder) {
    Log.d(TAG, "animateAdd");
    return false;
  }

  @Override
  public boolean animateMove(RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
    Log.d(TAG, "animateMove");
    final View v = holder.itemView;
    ViewCompat.animate(v)
        .translationY(toY - fromY - ViewCompat.getTranslationY(v))
        .start();
    return false;
  }

  @Override
  public boolean animateChange(RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder, int fromLeft, int fromTop, int toLeft, int toTop) {
    Log.d(TAG, "animateChange");
    return false;
  }

  @Override
  public void runPendingAnimations() {
    Log.d(TAG, "runPendingAnimations");
  }

  @Override
  public void endAnimation(RecyclerView.ViewHolder item) {
    Log.d(TAG, "endAnimation");
  }

  @Override
  public void endAnimations() {
    Log.d(TAG, "endAnimations");
  }

  @Override
  public boolean isRunning() {
    // Log.d(TAG, "isRunning"); called when view moved
    return false;
  }
}
