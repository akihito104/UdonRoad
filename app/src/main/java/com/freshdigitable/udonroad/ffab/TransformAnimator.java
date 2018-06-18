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

import com.freshdigitable.udonroad.R;

/**
 * TransformAnimator is for custom reveal animation of IndicatableFFAB and BottomButtonsToolbar.
 *
 * Created by akihit on 2017/03/17.
 */

class TransformAnimator {

  protected final IndicatableFFAB ffab;
  protected final View bottomSheet;

  static TransformAnimator create(IndicatableFFAB ffab, View bottomSheet) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      return new TransformAnimatorImplV21(ffab, bottomSheet);
    } else {
      return new TransformAnimator(ffab, bottomSheet);
    }
  }

  private TransformAnimator(IndicatableFFAB ffab, View bottomSheet) {
    this.ffab = ffab;
    this.bottomSheet = bottomSheet;
  }

  void transToToolbar() {
    ffab.hide();
    bottomSheet.setVisibility(View.VISIBLE);
  }

  void transToFab(final int afterVisibility) {
    bottomSheet.setVisibility(View.INVISIBLE);
    ffab.show();
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private static class TransformAnimatorImplV21 extends TransformAnimator {
    private static final float FAB_SCALE = 1.2f;
    private static final int FAB_MOVE_DURATION = 80;
    private static final int TOOLBAR_MOVE_DURATION = 120;
    private static final AccelerateInterpolator ACCELERATE_INTERPOLATOR = new AccelerateInterpolator();

    private final BottomButtonsToolbar bbt;

    private TransformAnimatorImplV21(IndicatableFFAB ffab, View bottomSheet) {
      super(ffab, bottomSheet);
      this.bbt = bottomSheet.findViewById(R.id.iffabSheet_button);
    }

    @Override
    void transToToolbar() {
      final float toolbarX = getToolbarX();
      final float toolbarY = getToolbarY();
      ffab.animate()
          .scaleX(FAB_SCALE)
          .scaleY(FAB_SCALE)
          .translationX(getToolbarCenterX() - getCenterX(ffab))
          .translationY(getToolbarCenterY() - getCenterY(ffab))
          .setDuration(FAB_MOVE_DURATION)
          .setInterpolator(ACCELERATE_INTERPOLATOR)
          .setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
              ffab.setVisibility(View.INVISIBLE);
              showToolbar(toolbarX, toolbarY);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
              ffab.setVisibility(View.INVISIBLE);
              bottomSheet.setVisibility(View.VISIBLE);
              animation.removeListener(this);
            }
          })
          .start();
    }

    private void showToolbar(float toolbarX, float toolbarY) { // XXX
      final int centerX = (int) (getCenterX(ffab) - toolbarX);
      final int centerY = (int) (getCenterY(ffab) - toolbarY);
      final Animator revealAnimator = ViewAnimationUtils.createCircularReveal(bbt,
          centerX, centerY,
          calcMinRevealRadius(ffab), calcMaxRevealRadius(centerX, centerY));
      revealAnimator.setDuration(TOOLBAR_MOVE_DURATION);
      revealAnimator.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationStart(Animator animation) {
          bottomSheet.setVisibility(View.VISIBLE);
        }
      });
      revealAnimator.start();
    }

    @Override
    void transToFab(final int afterVisibility) {
      final int centerX = (int) (getCenterX(ffab) - bottomSheet.getLeft());
      final int centerY = (int) (getCenterY(ffab) - bottomSheet.getTop());
      final Animator revealAnimator = ViewAnimationUtils.createCircularReveal(bbt,
          centerX, centerY,
          calcMaxRevealRadius(centerX, centerY), calcMinRevealRadius(ffab));
      revealAnimator.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          bottomSheet.setVisibility(View.INVISIBLE);
          showFFAB(ffab, afterVisibility);
        }

        @Override
        public void onAnimationCancel(Animator animation) {
          bottomSheet.setVisibility(View.INVISIBLE);
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

    private float getToolbarCenterX() {
      return bottomSheet.getX() + getCenterX(bbt);
    }

    private float getToolbarCenterY() {
      return bottomSheet.getY() + getCenterY(bbt);
    }

    private float getToolbarX() {
      return bottomSheet.getX() + bbt.getX();
    }

    private float getToolbarY() {
      return bottomSheet.getY() + bbt.getY();
    }

    private static float calcMinRevealRadius(IndicatableFFAB ffab) {
      return ffab.getScaleX() * ffab.getHeight() / 2;
    }

    private float calcMaxRevealRadius(int centerX, int centerY) {
      final int radiusX = Math.max(Math.abs(centerX), Math.abs(bbt.getWidth() - centerX));
      final int radiusY = Math.max(Math.abs(centerY), Math.abs(bbt.getHeight() - centerY));
      return (float) Math.hypot(radiusX, radiusY);
    }
  }
}
