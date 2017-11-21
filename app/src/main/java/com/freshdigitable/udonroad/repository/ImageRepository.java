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

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.DimenRes;
import android.view.View;

import io.reactivex.Observable;
import io.reactivex.Single;
import twitter4j.User;

/**
 * Created by akihit on 2017/11/09.
 */

public interface ImageRepository {

  Observable<Drawable> queryUserIcon(User user, @DimenRes int sizeRes, boolean placeholder, Object tag);

  Observable<Drawable> querySmallUserIcon(User user, Object tag);

  Observable<Drawable> queryPhotoMedia(String url, Object tag);

  Observable<Drawable> querySquareImage(String url, @DimenRes int sizeRes, Object tag);

  Observable<Drawable> queryToFit(Uri uri, View target, boolean centerCrop, Object tag);

  Observable<Drawable> queryToFit(String uri, View target, boolean centerCrop, Object tag);

  Observable<Drawable> queryImage(ImageQuery query);

  Observable<Drawable> queryImage(Single<ImageQuery> query);
}
