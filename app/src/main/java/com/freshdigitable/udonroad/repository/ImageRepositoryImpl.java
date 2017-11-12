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
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.v7.content.res.AppCompatResources;

import com.freshdigitable.udonroad.R;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import twitter4j.MediaEntity;
import twitter4j.User;

/**
 * Created by akihit on 2017/11/09.
 */

class ImageRepositoryImpl implements ImageRepository {

  private final Picasso client;
  private final Context context;

  @Inject
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
    final RequestCreator request = getRequestCreator(user.getProfileImageURLHttps(), sizeRes, tag)
        .placeholder(AppCompatResources.getDrawable(context, R.drawable.ic_person_outline_black));
    return ImageObservable.create(request, createDisposable(tag));
  }

  @Override
  public Observable<Drawable> querySmallUserIcon(User user, Object tag) {
    final RequestCreator request = getRequestCreator(user.getMiniProfileImageURLHttps(), R.dimen.small_user_icon, tag)
        .placeholder(AppCompatResources.getDrawable(context, R.drawable.ic_person_outline_black));
    return ImageObservable.create(request, createDisposable(tag));
  }

  @Override
  public Observable<Drawable> querySquareImage(String url, @DimenRes int sizeRes, Object tag) {
    final RequestCreator request = getRequestCreator(url, sizeRes, tag)
        .centerCrop();
    return ImageObservable.create(request, createDisposable(tag));
  }

  private RequestCreator getRequestCreator(String url, @DimenRes int sizeRes, Object tag) {
    return client.load(url)
        .tag(tag)
        .resizeDimen(sizeRes, sizeRes);
  }

  @Override
  public Observable<Drawable> queryMediaThumbnail(MediaEntity entity, int height, int width, Object tag) {
    final RequestCreator request = client.load(entity.getMediaURLHttps() + ":thumb")
        .tag(tag)
        .resize(width, height)
        .centerCrop();
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
          observer.onNext(errorDrawable);
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
          observer.onNext(placeHolderDrawable);
        }
      };
      request.into(target);
    }
  }
}
