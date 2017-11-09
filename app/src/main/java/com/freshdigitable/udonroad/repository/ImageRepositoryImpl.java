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
import android.support.v7.content.res.AppCompatResources;

import com.freshdigitable.udonroad.R;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * Created by akihit on 2017/11/09.
 */

class ImageRepositoryImpl implements ImageRepository {

  private final Picasso client;
  private final Drawable placeholder;

  @Inject
  ImageRepositoryImpl(Context appContext) {
    client = Picasso.with(appContext);
    placeholder = AppCompatResources.getDrawable(appContext, R.drawable.ic_person_outline_black);
  }

  @Override
  public Observable<Drawable> queryUserIcon(String url, long tag) {
    final ImageObservable imageObservable = new ImageObservable();
    imageObservable.client = client;
    imageObservable.placeholder = placeholder;
    imageObservable.url = url;
    imageObservable.tag = tag;
    return imageObservable;
  }

  private static class ImageObservable extends Observable<Drawable> {
    Picasso client;
    Drawable placeholder;
    String url;
    long tag;
    private Target target;
    boolean disposed;

    @Override
    protected void subscribeActual(Observer<? super Drawable> observer) {
      observer.onSubscribe(new Disposable() {
        @Override
        public void dispose() {
          client.cancelTag(tag);
          disposed = true;
        }

        @Override
        public boolean isDisposed() {
          return disposed;
        }
      });
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
      client.load(url)
          .tag(tag)
          .resizeDimen(R.dimen.tweet_user_icon, R.dimen.tweet_user_icon)
          .placeholder(placeholder)
          .into(target);
    }
  }
}
