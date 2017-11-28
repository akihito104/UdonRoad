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
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;

/**
 * Created by akihit on 2017/11/09.
 */

class ImageRepositoryImpl implements ImageRepository {
  @SuppressWarnings("unused")
  private static final String TAG = ImageRepositoryImpl.class.getSimpleName();
  private final Picasso client;

  ImageRepositoryImpl(Context appContext) {
    client = Picasso.with(appContext);
  }

  @Override
  public Observable<Drawable> queryImage(Single<ImageQuery> query) {
    return query.toObservable().flatMap(this::queryImage);
  }

  @Override
  public Observable<Drawable> queryImage(ImageQuery query) {
    final RequestCreator request = client.load(query.uri);
    if (query.height > 0 && query.width > 0) {
      request.resize(query.width, query.height);
    }
    if (query.placeholder != null) {
      request.placeholder(query.placeholder);
    }
    if (query.centerCrop) {
      request.centerCrop();
    }
    return create(client, request);
  }

  @NonNull
  private static Observable<Drawable> create(Picasso client, @NonNull RequestCreator request) {
    return Observable.create(e -> {
      final Target target = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
          e.onNext(new BitmapDrawable(bitmap));
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
          if (errorDrawable != null) {
            e.onNext(errorDrawable);
          }
          e.onError(new RuntimeException());
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
          if (placeHolderDrawable != null) {
            e.onNext(placeHolderDrawable);
          }
        }
      };
      e.setDisposable(new Disposable() {
        private boolean disposed = false;

        @Override
        public void dispose() {
          if(!disposed) {
            client.cancelRequest(target);
            disposed = true;
          }
        }

        @Override
        public boolean isDisposed() {
          return disposed;
        }
      });
      request.into(target);
    });
  }
}
