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

package com.freshdigitable.udonroad.repository;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.DimenRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.Size;
import android.support.v7.content.res.AppCompatResources;
import android.view.View;
import android.view.ViewTreeObserver;

import io.reactivex.Single;

/**
 * Created by akihit on 2017/11/19.
 */
public class ImageQuery {
  final Uri uri;
  final Drawable placeholder;
  final int width;
  final int height;
  final boolean centerCrop;
  final long tag = System.nanoTime();

  private ImageQuery(Builder builder) {
    uri = builder.uri;
    placeholder = builder.placeholder;
    width = builder.width;
    height = builder.height;
    centerCrop = builder.centerCrop;
  }

  public static class Builder {
    private final Uri uri;
    Drawable placeholder;
    int width;
    int height;
    boolean centerCrop = false;

    public Builder(String url) {
      this(url != null ? Uri.parse(url) : null);
    }

    public Builder(Uri uri) {
      this.uri = uri;
    }

    public Builder placeholder(Drawable placeholder) {
      this.placeholder = placeholder;
      return this;
    }

    public Builder placeholder(Context context, @DrawableRes int placeholderRes) {
      return placeholder(AppCompatResources.getDrawable(context, placeholderRes));
    }

    public Builder width(@Size(min = 1) int width) {
      this.width = width;
      return this;
    }

    public Builder width(Context context, @DimenRes int widthRes) {
      return width(context.getResources().getDimensionPixelSize(widthRes));
    }

    public Builder height(@Size(min = 1) int height) {
      this.height = height;
      return this;
    }

    public Builder height(Context context, @DimenRes int heightRes) {
      return height(context.getResources().getDimensionPixelSize(heightRes));
    }

    public Builder centerCrop() {
      this.centerCrop = true;
      return this;
    }

    public ImageQuery build() {
      return new ImageQuery(this);
    }

    public Single<ImageQuery> build(View view) {
      if (view.getWidth() > 0 && view.getHeight() > 0) {
        width(view.getWidth());
        height(view.getHeight());
        return Single.just(build());
      }
      return observeOnGlobalLayout(view)
          .map(v -> {
            this.width(v.getWidth());
            this.height(v.getHeight());
            return this.build();
          });
    }
  }

  private static Single<View> observeOnGlobalLayout(View target) {
    return Single.create(e ->
        target.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            if (target.getHeight() <= 0 || target.getWidth() <= 0) {
              return;
            }
            e.onSuccess(target);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
              target.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            } else {
              target.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
          }
        }));
  }
}
