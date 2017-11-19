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
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.v7.content.res.AppCompatResources;
import android.view.View;
import android.view.ViewTreeObserver;

import com.freshdigitable.udonroad.R;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import twitter4j.MediaEntity;
import twitter4j.User;

/**
 * Created by akihit on 2017/11/09.
 */

class ImageRepositoryImpl implements ImageRepository {

  private final Picasso client;
  private final Context context;
  private final ColorDrawable defaultPlaceholder = new ColorDrawable(Color.LTGRAY);

  ImageRepositoryImpl(Context appContext) {
    client = Picasso.with(appContext);
    context = appContext;
  }

  @Override
  public Observable<Drawable> queryUserIcon(User user, Object tag) {
    return queryUserIcon(user, R.dimen.tweet_user_icon, tag);
  }

  @Override
  public Observable<Drawable> queryUserIcon(User user, int sizeRes, Object tag) {
    return queryUserIcon(user, sizeRes, true, tag);
  }

  @Override
  public Observable<Drawable> queryUserIcon(User user, int sizeRes, boolean placeholder, Object tag) {
    final RequestCreator request = getRequestCreator(user.getProfileImageURLHttps(), sizeRes, tag);
    if (placeholder) {
      final Drawable drawable = AppCompatResources.getDrawable(context, R.drawable.ic_person_outline_black);
      if (drawable != null) {
        request.placeholder(drawable);
      }
    }
    return queryImage(request, tag);
  }

  @Override
  public Observable<Drawable> querySmallUserIcon(User user, Object tag) {
    final RequestCreator request = getRequestCreator(user.getMiniProfileImageURLHttps(), R.dimen.small_user_icon, tag);
    final Drawable drawable = AppCompatResources.getDrawable(context, R.drawable.ic_person_outline_black);
    if (drawable != null) {
      request.placeholder(drawable);
    }
    return queryImage(request, tag);
  }

  @Override
  public Observable<Drawable> querySquareImage(String url, @DimenRes int sizeRes, Object tag) {
    final RequestCreator request = getRequestCreator(url, sizeRes, tag)
        .centerCrop();
    return queryImage(request, tag);
  }

  private RequestCreator getRequestCreator(String url, @DimenRes int sizeRes, Object tag) {
    return client.load(url)
        .tag(tag)
        .resizeDimen(sizeRes, sizeRes);
  }

  @Override
  public Observable<Drawable> queryMediaThumbnail(MediaEntity entity, View target, Object tag) {
    return observeOnGlobalLayout(target)
        .toObservable()
        .flatMap(v -> queryMediaThumbnail(entity, v.getHeight(), v.getWidth(), tag));
  }

  private Single<View> observeOnGlobalLayout(View target) {
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

  @Override
  public Observable<Drawable> queryMediaThumbnail(MediaEntity entity, int height, int width, Object tag) {
    final RequestCreator request = client.load(entity.getMediaURLHttps() + ":thumb")
        .tag(tag)
        .resize(width, height)
        .placeholder(defaultPlaceholder)
        .centerCrop();
    return queryImage(request, tag);
  }

  @Override
  public Observable<Drawable> queryPhotoMedia(String url, Object tag) {
    return queryImage(client.load(url).tag(tag), tag);
  }

  @Override
  public Observable<Drawable> queryToFit(Uri uri, View target, boolean centerCrop, Object tag) {
    final RequestCreator request = client.load(uri)
        .placeholder(defaultPlaceholder)
        .tag(tag);
    if (centerCrop) {
      request.centerCrop();
    }
    if (target.getHeight() > 0 && target.getWidth() > 0) {
      request.resize(target.getWidth(), target.getHeight());
      return queryImage(request, tag);
    } else {
      return observeOnGlobalLayout(target)
          .map(v -> request.resize(v.getWidth(), v.getHeight()))
          .toObservable()
          .flatMap(r -> queryImage(r, tag));
    }
  }

  @Override
  public Observable<Drawable> queryToFit(String uri, View target, boolean centerCrop, Object tag) {
    return queryToFit(Uri.parse(uri), target, centerCrop, tag);
  }

  private Observable<Drawable> queryImage(RequestCreator request, Object tag) {
    return ImageObservable.create(request, createDisposable(tag));
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
