/*
 * Copyright (c) 2017. Matsuda, Akihit (akihito104)
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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

/**
 * TransformAnimator is for custom reveal animation of IndicatableFFAB and BottomButtonsToolbar.
 *
 * Created by akihit on 2017/03/17.
 */

class TransformAnimator {

  private static final float FAB_SCALE = 1.2f;
  private static final int FAB_MOVE_DURATION = 80;
  private static final int TOOLBAR_MOVE_DURATION = 120;
  private static final AccelerateInterpolator ACCELERATE_INTERPOLATOR = new AccelerateInterpolator();

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  static void transToToolbar(@NonNull IndicatableFFAB ffab, @NonNull BottomButtonsToolbar bbt) {
    ffab.animate()
        .scaleX(FAB_SCALE)
        .scaleY(FAB_SCALE)
        .translationX(getCenterX(bbt) - getCenterX(ffab))
        .translationY(getCenterY(bbt) - getCenterY(ffab))
        .setDuration(FAB_MOVE_DURATION)
        .setInterpolator(ACCELERATE_INTERPOLATOR)
        .setListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            ffab.setVisibility(View.INVISIBLE);
            showToolbar(ffab, bbt);
          }

          @Override
          public void onAnimationCancel(Animator animation) {
            ffab.setVisibility(View.INVISIBLE);
            bbt.setVisibility(View.VISIBLE);
            animation.removeListener(this);
          }
        })
        .start();
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private static void showToolbar(IndicatableFFAB ffab, BottomButtonsToolbar bbt) {
    final int centerX = (int) (getCenterX(ffab) - bbt.getLeft());
    final int centerY = (int) (getCenterY(ffab) - bbt.getTop());
    final Animator revealAnimator = ViewAnimationUtils.createCircularReveal(bbt,
        centerX, centerY,
        calcMinRevealRadius(ffab), calcMaxRevealRadius(ffab, bbt));
    revealAnimator.setDuration(TOOLBAR_MOVE_DURATION);
    revealAnimator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationStart(Animator animation) {
        bbt.setVisibility(View.VISIBLE);
      }
    });
    revealAnimator.start();
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  static void transToFab(@NonNull IndicatableFFAB ffab, @NonNull BottomButtonsToolbar bbt, final int afterVisibility) {
    final int centerX = (int) (getCenterX(ffab) - bbt.getLeft());
    final int centerY = (int) (getCenterY(ffab) - bbt.getTop());
    final Animator revealAnimator = ViewAnimationUtils.createCircularReveal(bbt,
        centerX, centerY,
        calcMaxRevealRadius(ffab, bbt), calcMinRevealRadius(ffab));
    revealAnimator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        bbt.setVisibility(View.INVISIBLE);
        showFFAB(ffab, afterVisibility);
      }

      @Override
      public void onAnimationCancel(Animator animation) {
        bbt.setVisibility(View.INVISIBLE);
        ffab.setVisibility(View.VISIBLE);
        animation.removeListener(this);
      }
    });
    revealAnimator.setDuration(TOOLBAR_MOVE_DURATION);
    revealAnimator.start();
  }

  private static final DecelerateInterpolator DECELERATE_INTERPOLATOR = new DecelerateInterpolator();

  private static void showFFAB(@NonNull IndicatableFFAB ffab, final int afterVisibility) {
    final ViewPropertyAnimator animator = ffab.animate()
        .scaleX(1)
        .scaleY(1)
        .translationX(0)
        .translationY(0)
        .setInterpolator(DECELERATE_INTERPOLATOR)
        .setDuration(FAB_MOVE_DURATION);
    animator.setListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationStart(Animator animation) {
        ffab.setVisibility(afterVisibility);
      }
    });
    animator.start();
  }

  private static float getCenterX(View v) {
    return v.getX() + v.getPivotX();
  }

  private static float getCenterY(View v) {
    return v.getY() + v.getPivotY();
  }

  private static float calcMinRevealRadius(IndicatableFFAB ffab) {
    return ffab.getScaleX() * ffab.getHeight() / 2;
  }

  private static float calcMaxRevealRadius(
      @NonNull IndicatableFFAB ffab, @NonNull BottomButtonsToolbar bbt) {
    final int centerX = (int) (getCenterX(ffab) - bbt.getLeft());
    final int centerY = (int) (getCenterY(ffab) - bbt.getTop());
    final int radiusX = Math.max(Math.abs(centerX), Math.abs(bbt.getWidth() - centerX));
    final int radiusY = Math.max(Math.abs(centerY), Math.abs(bbt.getHeight() - centerY));
    return (float) Math.hypot(radiusX, radiusY);
  }
}
