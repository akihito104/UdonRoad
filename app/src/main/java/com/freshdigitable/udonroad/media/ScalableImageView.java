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

package com.freshdigitable.udonroad.media;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/**
 * Created by akihit on 2017/09/02.
 */
public class ScalableImageView extends AppCompatImageView {
  private final GestureDetectorCompat gestureDetector;
  private final ScaleGestureDetector scaleGestureDetector;

  public ScalableImageView(Context context) {
    this(context, null);
  }

  public ScalableImageView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ScalableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
          scale = detector.getScaleFactor();
          focusX = detector.getFocusX();
          focusY = detector.getFocusY();
          return true;
        }
      });
    gestureDetector = new GestureDetectorCompat(getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
          if (e2.getEventTime() - e1.getEventTime() < 200
              && shouldGoNextPage(distanceX)) {
            return false;
          }
          transX = -distanceX;
          transY = -distanceY;
          return true;
        }

        private boolean shouldGoNextPage(final float distX) {
          if (Math.abs(distX) < 0.1) {
            return false;
          }
          if (getDrawable() == null) {
            return true;
          }
          if (getHeight() == getDrawable().getIntrinsicHeight() * imageMat[Matrix.MSCALE_Y]) {
            return true;
          }
          if (distX > 0) {
            final float maxTransX = getWidth() - getDrawable().getIntrinsicWidth() * imageMat[Matrix.MSCALE_X];
            return maxTransX <= 0
                && Math.abs(maxTransX - imageMat[Matrix.MTRANS_X]) < 0.01;
          } else {
            return Math.abs(imageMat[Matrix.MTRANS_X]) < 0.01;
          }
        }
      });
    gestureDetector.setIsLongpressEnabled(false);
    setScaleType(ScaleType.MATRIX);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    scaleGestureDetector.onTouchEvent(event);
    final boolean scaling = scaleGestureDetector.isInProgress();
    if (scaling) {
      transformMatrix.postScale(scale, scale, focusX, focusY);
    }
    final boolean scrolled = gestureDetector.onTouchEvent(event);
    if (scrolled) {
      transformMatrix.postTranslate(transX, transY);
    }
    final boolean invalidated = scaling || scrolled;
    if (invalidated) {
      getParent().requestDisallowInterceptTouchEvent(true);
      invalidate();
    }
    return invalidated || super.onTouchEvent(event);
  }

  private final float[] imageMat = new float[9];

  @Override
  protected void onDraw(Canvas canvas) {
    if (getDrawable() != null && !transformMatrix.isIdentity()) {
      final Matrix matrix = getImageMatrix();
      matrix.postConcat(transformMatrix);
      matrix.getValues(imageMat);
      imageMat[Matrix.MSCALE_X] = Math.max(imageMat[Matrix.MSCALE_X], matToFit[Matrix.MSCALE_X]);
      imageMat[Matrix.MSCALE_Y] = Math.max(imageMat[Matrix.MSCALE_Y], matToFit[Matrix.MSCALE_Y]);

      final float maxTransX = getWidth() - getDrawable().getIntrinsicWidth() * imageMat[Matrix.MSCALE_X];
      if (Math.abs(maxTransX) < Math.abs(imageMat[Matrix.MTRANS_X])) {
        imageMat[Matrix.MTRANS_X] = maxTransX;
      } else if (Math.signum(maxTransX) * imageMat[Matrix.MTRANS_X] < 0) {
        imageMat[Matrix.MTRANS_X] = 0;
      }

      final float maxTransY = getHeight() - getDrawable().getIntrinsicHeight() * imageMat[Matrix.MSCALE_Y];
      if (Math.abs(maxTransY) < Math.abs(imageMat[Matrix.MTRANS_Y])) {
        imageMat[Matrix.MTRANS_Y] = maxTransY;
      } else if (Math.signum(maxTransY) * imageMat[Matrix.MTRANS_Y] < 0) {
        imageMat[Matrix.MTRANS_Y] = 0;
      }

      matrix.setValues(imageMat);
      setImageMatrix(matrix);
      transformMatrix.reset();
    }
    super.onDraw(canvas);
  }

  private Matrix transformMatrix = new Matrix();
  private float scale = 1;
  private float focusX;
  private float focusY;
  private float transX;
  private float transY;

  private final RectF viewRect = new RectF();
  private final RectF drawableRect = new RectF();
  private final Matrix matrixToFit = new Matrix();
  private final float[] matToFit = new float[9];

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    viewRect.set(0, 0, w, h);
    updateMatrixToFit();
  }

  @Override
  public void setImageDrawable(@Nullable Drawable drawable) {
    super.setImageDrawable(drawable);
    if (drawable == null) {
      drawableRect.setEmpty();
    } else {
      drawableRect.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
    }
    updateMatrixToFit();
  }

  private void updateMatrixToFit() {
    if (drawableRect.isEmpty() || viewRect.isEmpty()) {
      matrixToFit.reset();
      transformMatrix.reset();
    } else {
      matrixToFit.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER);
      setImageMatrix(matrixToFit);
      invalidate();
    }
    matrixToFit.getValues(matToFit);
  }

  @Override
  public void setImageMatrix(Matrix matrix) {
    super.setImageMatrix(matrix);
    matrix.getValues(imageMat);
  }
}
