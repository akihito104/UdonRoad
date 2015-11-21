package com.freshdigitable.udonroad;

import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Log;

/**
 * Created by akihit on 2015/11/21.
 */
public class TimelineAnimator extends SimpleItemAnimator {
  private static final String TAG = TimelineAnimator.class.getName();

  @Override
  public boolean animateRemove(RecyclerView.ViewHolder holder) {
    return false;
  }

  @Override
  public boolean animateAdd(RecyclerView.ViewHolder holder) {
    Log.d(TAG, "animateAdd");
    ViewCompat.setTranslationY(holder.itemView, holder.itemView.getLayoutParams().height);
    return false;
  }

  @Override
  public boolean animateMove(RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
    return false;
  }

  @Override
  public boolean animateChange(RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder, int fromLeft, int fromTop, int toLeft, int toTop) {
    return false;
  }

  @Override
  public void runPendingAnimations() {

  }

  @Override
  public void endAnimation(RecyclerView.ViewHolder item) {

  }

  @Override
  public void endAnimations() {

  }

  @Override
  public boolean isRunning() {
    return false;
  }
}
