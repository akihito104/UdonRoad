package com.freshdigitable.udonroad;

import android.support.v4.animation.AnimatorCompatHelper;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * customized animation for RecyclerView as a Twitter Timeline
 * Created by akihit on 2015/11/21.
 */
public class TimelineAnimator extends SimpleItemAnimator {
  private static final String TAG = TimelineAnimator.class.getSimpleName();

  private final List<ViewHolder> pendingRemoves = new ArrayList<>();
  private final List<ViewHolder> removeAnimations = new ArrayList<>();

  @Override
  public boolean animateRemove(final ViewHolder holder) {
//    Log.d(TAG, "animateRemove: ");
    resetAnimation(holder);
    pendingRemoves.add(holder);
    return true;
  }

  private void animateRemoveImpl(final ViewHolder holder) {
//    Log.d(TAG, "animateRemoveImpl: ");
    removeAnimations.add(holder);
    ViewCompat.animate(holder.itemView)
        .translationY(-holder.itemView.getHeight())
        .alpha(0)
        .setDuration(getRemoveDuration())
        .setListener(new ViewPropertyAnimatorListenerAdapter() {
          @Override
          public void onAnimationStart(View view) {
            dispatchRemoveStarting(holder);
          }

          @Override
          public void onAnimationEnd(View view) {
            clearAllAnimationSettings(view);
            dispatchRemoveFinished(holder);
            removeAnimations.remove(holder);
            dispatchFinishedWhenDone();
          }

          @Override
          public void onAnimationCancel(View view) {
            clearAllAnimationSettings(view);
          }
        })
        .start();
  }

  private final List<Move> pendingMoves = new ArrayList<>();
  private final List<ViewHolder> moveAnimations = new ArrayList<>();

  private static class Move {
    private final ViewHolder holder;
    private final int fromX;
    private final int fromY;
    private final int toX;
    private final int toY;

    private Move(ViewHolder holder,
                 int fromX, int fromY, int toX, int toY) {
      this.holder = holder;
      this.fromX = fromX;
      this.fromY = fromY;
      this.toX = toX;
      this.toY = toY;
    }

    private int deltaX() {
      return toX - fromX;
    }

    private int deltaY() {
      return toY - fromY;
    }
  }

  @Override
  public boolean animateMove(final ViewHolder holder,
                             int fromX, int fromY, int toX, int toY) {
//    Log.d(TAG, "animateMove: " + debugString(holder));
    fromX += ViewCompat.getTranslationX(holder.itemView);
    fromY += ViewCompat.getTranslationY(holder.itemView);
    resetAnimation(holder);
    final int dX = toX - fromX;
    final int dY = toY - fromY;
    if (dX == 0 && dY == 0) {
      dispatchMoveFinished(holder);
      return false;
    }

    if (dX != 0) {
      ViewCompat.setTranslationX(holder.itemView, -dX);
    }
    if (dY != 0) {
      ViewCompat.setTranslationY(holder.itemView, -dY);
    }
    pendingMoves.add(new Move(holder, fromX, fromY, toX, toY));
    return true;
  }

  private void animateMoveImpl(final Move move) {
//    Log.d(TAG, "animateMoveImpl: " + debugString(move.holder));
    moveAnimations.add(move.holder);
    final ViewPropertyAnimatorCompat animate = ViewCompat.animate(move.holder.itemView);
    if (move.deltaX() != 0) {
      animate.translationX(0);
    }
    if (move.deltaY() != 0) {
      animate.translationY(0);
    }
    animate.setDuration(getMoveDuration())
        .setListener(new ViewPropertyAnimatorListenerAdapter() {
          @Override
          public void onAnimationStart(View view) {
            dispatchMoveStarting(move.holder);
          }

          @Override
          public void onAnimationEnd(View view) {
            animate.setListener(null);
            dispatchMoveFinished(move.holder);
            moveAnimations.remove(move.holder);
            dispatchFinishedWhenDone();
          }

          @Override
          public void onAnimationCancel(View view) {
            if (move.deltaX() != 0) {
              ViewCompat.setTranslationX(view, 0);
            }
            if (move.deltaY() != 0) {
              ViewCompat.setTranslationY(view, 0);
            }
          }
        })
        .start();
  }

  private final List<ViewHolder> pendingAdd = new ArrayList<>();
  private final List<ViewHolder> addAnimations = new ArrayList<>();

  @Override
  public boolean animateAdd(ViewHolder holder) {
//    Log.d(TAG, "animateAdd: " + debugString(holder));
    resetAnimation(holder);
    ViewCompat.setTranslationY(holder.itemView, 0);
    pendingAdd.add(holder);
    return true;
  }

  private void animateAddImpl(final ViewHolder holder) {
//    Log.d(TAG, "animateAddImpl: " + debugString(holder));
    addAnimations.add(holder);
    ViewCompat.animate(holder.itemView)
        .setDuration(0)
        .setListener(new ViewPropertyAnimatorListenerAdapter() {
          @Override
          public void onAnimationStart(View view) {
            dispatchAddStarting(holder);
          }

          @Override
          public void onAnimationEnd(View view) {
            clearAllAnimationSettings(view);
            dispatchAddFinished(holder);
            addAnimations.remove(holder);
          }

          @Override
          public void onAnimationCancel(View view) {
            clearAllAnimationSettings(view);
          }
        })
        .start();
  }

  @Override
  public boolean animateChange(ViewHolder oldHolder,
                               ViewHolder newHolder,
                               int fromLeft, int fromTop, int toLeft, int toTop) {
//    Log.d(TAG, "animateChange: ");
    return false; // nothing to do
  }

  private final List<List<Move>> moveAnimList = new ArrayList<>();
  private final List<List<ViewHolder>> addAnimList = new ArrayList<>();

  @Override
  public void runPendingAnimations() {
//    Log.d(TAG, "runPendingAnimations: ");
    final boolean isPendingRemove = !pendingRemoves.isEmpty();
    final boolean isPendingMove = !pendingMoves.isEmpty();

    // remove
    for (ViewHolder pendingRemove : pendingRemoves) {
      animateRemoveImpl(pendingRemove);
    }
    pendingRemoves.clear();
    // move
    if (isPendingMove) {
      final List<Move> moves = new ArrayList<>(pendingMoves.size());
      moves.addAll(pendingMoves);
      moveAnimList.add(moves);
      pendingMoves.clear();
      final Runnable mover = new Runnable() {
        @Override
        public void run() {
          for (Move move : moves) {
            animateMoveImpl(move);
          }
          moves.clear();
          moveAnimList.remove(moves);
        }
      };
      if (isPendingRemove) {
        ViewCompat.postOnAnimationDelayed(moves.get(0).holder.itemView, mover, getRemoveDuration());
      } else {
        mover.run();
      }
    }
    // add
    if (!pendingAdd.isEmpty()) {
      final List<ViewHolder> adds = new ArrayList<>(pendingAdd.size());
      adds.addAll(pendingAdd);
      addAnimList.add(adds);
      pendingAdd.clear();
      final Runnable adder = new Runnable() {
        @Override
        public void run() {
          for (ViewHolder add : adds) {
            animateAddImpl(add);
          }
          adds.clear();
          addAnimList.remove(adds);
        }
      };
      if (isPendingRemove || isPendingMove) {
        long removeDuration = isPendingRemove ? getRemoveDuration() : 0;
        long moveDuration = isPendingMove ? getMoveDuration() : 0;
        ViewCompat.postOnAnimationDelayed(adds.get(0).itemView, adder, removeDuration + moveDuration);
      } else {
        adder.run();
      }
    }
  }

  @Override
  public void endAnimation(ViewHolder item) {
//    Log.d(TAG, "endAnimation: ");
    ViewCompat.animate(item.itemView).cancel();

    cancelMoveAnimationFromList(pendingMoves, item);
    if (pendingRemoves.remove(item)) {
      clearAllAnimationSettings(item.itemView);
      dispatchRemoveFinished(item);
    }
    cancelAddAnimationFromList(pendingAdd, item);

    for (int i = moveAnimList.size() - 1; i >= 0; i--) {
      final List<Move> moves = moveAnimList.get(i);
      cancelMoveAnimationFromList(moves, item);
      if (moves.isEmpty()) {
        moveAnimList.remove(i);
      }
    }
    for (int i = addAnimList.size() - 1; i >= 0; i--) {
      final List<ViewHolder> adds = addAnimList.get(i);
      cancelAddAnimationFromList(adds, item);
      if (adds.isEmpty()) {
        addAnimList.remove(i);
      }
    }

    if (removeAnimations.remove(item)) {
      throw new IllegalStateException("after animation is canceled, item should not be in removeAnimations.");
    }
    if (addAnimations.remove(item)) {
      throw new IllegalStateException("after animation is canceled, item should not be in addAnimations.");
    }
    if (moveAnimations.remove(item)) {
      throw new IllegalStateException("after animation is canceled, item should not be in moveAnimations.");
    }
    dispatchFinishedWhenDone();
  }

  private void cancelMoveAnimationFromList(List<Move> moves, ViewHolder item) {
    for (int i = moves.size() - 1; i >= 0; i--) {
      final Move move = moves.get(i);
      if (move.holder == item) {
        cancelMoveAnimation(item);
        moves.remove(i);
      }
    }
  }

  private void cancelMoveAnimation(ViewHolder item) {
    ViewCompat.setTranslationY(item.itemView, 0);
    ViewCompat.setTranslationX(item.itemView, 0);
    dispatchMoveFinished(item);
  }

  private void cancelAddAnimationFromList(List<ViewHolder> adds, ViewHolder item) {
    if (adds.remove(item)) {
      clearAllAnimationSettings(item.itemView);
      dispatchAddFinished(item);
    }
  }

  @Override
  public void endAnimations() {
//    Log.d(TAG, "endAnimations: ");
    for (int i = pendingMoves.size() - 1; i >= 0; i--) {
      Move move = pendingMoves.get(i);
      cancelMoveAnimation(move.holder);
      pendingMoves.remove(i);
    }
    for (int i = pendingRemoves.size() - 1; i >= 0; i--) {
      final ViewHolder remove = pendingRemoves.get(i);
      dispatchRemoveFinished(remove);
      pendingRemoves.remove(i);
    }
    for (int i = pendingAdd.size() - 1; i >= 0; i--) {
      final ViewHolder add = pendingAdd.get(i);
      dispatchAddFinished(add);
      pendingRemoves.remove(i);
    }

    for (int i = moveAnimList.size() - 1; i >= 0; i--) {
      final List<Move> moves = moveAnimList.get(i);
      for (int j = moves.size() - 1; j >= 0; j--) {
        final Move move = moves.get(j);
        cancelMoveAnimation(move.holder);
        moves.remove(j);
      }
      moveAnimList.remove(i);
    }
    for (int i = addAnimList.size() - 1; i >= 0; i--) {
      final List<ViewHolder> adds = addAnimList.get(i);
      for (int j = adds.size() - 1; j >= 0; j--) {
        dispatchAddFinished(adds.get(j));
        adds.remove(j);
      }
      addAnimList.remove(i);
    }

    cancelAll(removeAnimations);
    cancelAll(moveAnimations);
    cancelAll(addAnimations);

    dispatchAnimationsFinished();
  }

  private void cancelAll(List<ViewHolder> viewHolders) {
    for (ViewHolder vh : viewHolders) {
      ViewCompat.animate(vh.itemView).cancel();
    }
    viewHolders.clear();
  }

  @Override
  public boolean isRunning() {
//    Log.d(TAG, "isRunning: ");
    return !addAnimations.isEmpty() || !addAnimList.isEmpty()
        || !moveAnimations.isEmpty() || !moveAnimList.isEmpty()
        || !removeAnimations.isEmpty();
  }

  private void resetAnimation(ViewHolder holder) {
    AnimatorCompatHelper.clearInterpolator(holder.itemView);
    endAnimation(holder);
  }

  private void dispatchFinishedWhenDone() {
    if (!isRunning()) {
      dispatchAnimationsFinished();
    }
  }

  private void clearAllAnimationSettings(View v) {
    ViewCompat.setAlpha(v, 1);
    ViewCompat.setScaleX(v, 1);
    ViewCompat.setScaleY(v, 1);
    ViewCompat.setTranslationX(v, 0);
    ViewCompat.setTranslationY(v, 0);
    ViewCompat.setRotationX(v, 0);
    ViewCompat.setRotationY(v, 0);
    ViewCompat.setPivotX(v, v.getMeasuredWidth() / 2);
    ViewCompat.setPivotY(v, v.getMeasuredHeight() / 2);
    ViewCompat.animate(v).setInterpolator(null).setStartDelay(0);
  }

  private String debugString(ViewHolder item) {
    return "view: " + item.itemView.toString();
  }
}
