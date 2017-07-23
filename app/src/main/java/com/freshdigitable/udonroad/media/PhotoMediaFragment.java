/*
 * Copyright (c) 2016. Matsuda, Akihit (akihito104)
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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

/**
 * PhotoMediaFragment shows general image file such a photo.
 *
 * Created by akihit on 2016/07/17.
 */
public class PhotoMediaFragment extends MediaViewActivity.MediaFragment {
  @SuppressWarnings("unused")
  private static final String TAG = PhotoMediaFragment.class.getSimpleName();
  private ImageView imageView;
  private String loadingTag;
  private Matrix imageMatrix;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    imageView = new ScalableImageView(getContext());
    return imageView;
  }

  @Override
  public void onStart() {
    super.onStart();
    imageView.setOnClickListener(super.getOnClickListener());
    imageView.setOnTouchListener(super.getTouchListener());
    loadingTag = "media:" + mediaEntity.getId();
    Picasso.with(getContext())
        .load(mediaEntity.getMediaURLHttps() + ":medium")
        .tag(loadingTag)
        .into(imageView, new Callback() {
          @Override
          public void onSuccess() {
            imageView.setScaleType(ImageView.ScaleType.MATRIX);
            if (imageMatrix != null) {
              imageView.setImageMatrix(imageMatrix);
            }
          }

          @Override
          public void onError() {}
        });
  }

  @Override
  public void onStop() {
    super.onStop();
    Picasso.with(getContext()).cancelTag(loadingTag);
    imageMatrix = imageView.getImageMatrix();
    imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
    imageView.setOnClickListener(null);
    imageView.setOnTouchListener(null);
    imageView.setImageDrawable(null);
  }

  private static class ScalableImageView extends AppCompatImageView {
    private final GestureDetectorCompat gestureDetector;
    private final ScaleGestureDetector scaleGestureDetector;

    public ScalableImageView(Context context) {
      super(context, null, 0);
      scaleGestureDetector = new ScaleGestureDetector(getContext(), scaleListener);
      gestureDetector = new GestureDetectorCompat(getContext(), scrollListener);
      gestureDetector.setIsLongpressEnabled(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
      scaleGestureDetector.onTouchEvent(event);
      final boolean scaling = scaleGestureDetector.isInProgress();
      if (scaling) {
        currentMat.postScale(scale, scale, focusX, focusY);
      }
      final boolean scrolled = gestureDetector.onTouchEvent(event);
      if (scrolled) {
        currentMat.postTranslate(transX, transY);
      }
      final boolean invalidated = scaling || scrolled;
      if (invalidated) {
        invalidate();
      } else {
        Log.d(TAG, "onTouchEvent: not invalidated");
      }
      return invalidated || super.onTouchEvent(event);
    }

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
      matrixToFit.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER);
      matrixToFit.getValues(matToFit);
      Log.d(TAG, "updateMatrixToFit: " + matrixToFit.toShortString());
    }

    private final float[] mat = new float[9];
    @Override
    protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);
      if (!oldMat.equals(currentMat)) {
        final Matrix matrix = getImageMatrix();
        matrix.postConcat(currentMat);
        matrix.getValues(mat);
        mat[Matrix.MSCALE_X] = Math.max(mat[Matrix.MSCALE_X], matToFit[Matrix.MSCALE_X]);
        mat[Matrix.MSCALE_Y] = Math.max(mat[Matrix.MSCALE_Y], matToFit[Matrix.MSCALE_Y]);
        matrix.setValues(mat);
        Log.d(TAG, "onDraw: " + matrix.toShortString());
        setImageMatrix(matrix);
        oldMat.set(currentMat);
        currentMat.reset();
      }
    }

    private Matrix oldMat = new Matrix();
    private Matrix currentMat = new Matrix();
    private float scale = 1;
    private float focusX;
    private float focusY;
    private float transX;
    private float transY;

    private final SimpleOnGestureListener scrollListener = new SimpleOnGestureListener() {
      @Override
      public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (matrixToFit.equals(getImageMatrix())) {
          return false;
        }
        Log.d(TAG, "onScroll: dX>" + distanceX + ", dY>" + distanceY);
        transX = -distanceX;
        transY = -distanceY;
        return true;
      }
    };

    private final OnScaleGestureListener scaleListener = new SimpleOnScaleGestureListener() {
      @Override
      public boolean onScale(ScaleGestureDetector detector) {
        Log.d(TAG, "onScale: dt>" + detector.getTimeDelta() + ", sf>" + detector.getScaleFactor() + ", ps>" + detector.getPreviousSpan() + ", cs>" + detector.getCurrentSpan());
        scale = detector.getScaleFactor();
        focusX = detector.getFocusX();
        focusY = detector.getFocusY();
        return true;
      }
    };
  }
}
