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

/**
 * TransformAnimator is for custom reveal animation of IndicatableFFAB and BottomButtonsToolbar.
 *
 * Created by akihit on 2017/03/17.
 */

class TransformAnimator {

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  static void transToToolbar(@NonNull IndicatableFFAB ffab, @NonNull BottomButtonsToolbar bbt) {
    ffab.animate()
        .scaleX(1.2f)
        .scaleY(1.2f)
        .translationX(getCenterX(bbt) - getCenterX(ffab))
        .translationY(getCenterY(bbt) - getCenterY(ffab))
        .setDuration(100)
        .setListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            ffab.setVisibility(View.INVISIBLE);
            bbt.setVisibility(View.VISIBLE);
            showToolbar(ffab, bbt);
          }
        })
        .start();
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private static void showToolbar(IndicatableFFAB ffab, BottomButtonsToolbar bbt) {
    final Animator revealAnimator = ViewAnimationUtils.createCircularReveal(bbt,
        bbt.getWidth() / 2,
        bbt.getHeight() / 2,
        ffab.getHeight() / 2,
        (float) Math.hypot(bbt.getHeight(), bbt.getWidth()) / 2);
    revealAnimator.setDuration(200);
    revealAnimator.start();
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  static void transToFab(@NonNull IndicatableFFAB ffab, @NonNull BottomButtonsToolbar bbt) {
    final Animator revealAnimator = ViewAnimationUtils.createCircularReveal(bbt,
        bbt.getWidth() / 2,
        bbt.getHeight() / 2,
        (float) Math.hypot(bbt.getHeight(), bbt.getWidth()) / 2,
        ffab.getHeight() / 2);
    revealAnimator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        showFFAB(ffab);
        bbt.setVisibility(View.INVISIBLE);
        ffab.setVisibility(View.VISIBLE);
      }
    });
    revealAnimator.setDuration(200);
    revealAnimator.start();
  }

  private static void showFFAB(@NonNull IndicatableFFAB ffab) {
    ffab.animate()
        .scaleX(1)
        .scaleY(1)
        .translationX(0)
        .translationY(0)
        .setDuration(100)
        .start();
  }

  private static float getCenterX(View v) {
    return (v.getLeft() + v.getRight()) / 2;
  }

  private static float getCenterY(View view) {
    return (view.getTop() + view.getBottom()) / 2;
  }
}
