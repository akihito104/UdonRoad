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
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;

/**
 * Created by akihit on 2017/11/09.
 */

class ImageRepositoryImpl implements ImageRepository {

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
    final RequestCreator request = client.load(query.uri)
        .tag(query.tag);
    if (query.height > 0 && query.width > 0) {
      request.resize(query.width, query.height);
    }
    if (query.placeholder != null) {
      request.placeholder(query.placeholder);
    }
    if (query.centerCrop) {
      request.centerCrop();
    }
    return ImageObservable.create(request, createDisposable(query.tag));
  }

  @NonNull
  private Disposable createDisposable(Object tag) {
    return new Disposable() {
      boolean disposed = false;

      @Override
      public void dispose() {
        if (disposed) {
          return;
        }
        client.cancelTag(tag);
        disposed = true;
      }

      @Override
      public boolean isDisposed() {
        return disposed;
      }
    };
  }

  private static class ImageObservable extends Observable<Drawable> {
    @NonNull
    static ImageObservable create(@NonNull RequestCreator request, @NonNull Disposable disposable) {
      return new ImageObservable(request, disposable);
    }

    private final RequestCreator request;
    private final Disposable disposable;
    private Target target;

    private ImageObservable(RequestCreator request, Disposable disposable) {
      this.request = request;
      this.disposable = disposable;
    }

    @Override
    protected void subscribeActual(Observer<? super Drawable> observer) {
      observer.onSubscribe(disposable);
      target = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
          observer.onNext(new BitmapDrawable(bitmap));
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
          if (errorDrawable != null) {
            observer.onNext(errorDrawable);
          }
          observer.onError(new RuntimeException());
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
          if (placeHolderDrawable != null) {
            observer.onNext(placeHolderDrawable);
          }
        }
      };
      request.into(target);
    }
  }
}
