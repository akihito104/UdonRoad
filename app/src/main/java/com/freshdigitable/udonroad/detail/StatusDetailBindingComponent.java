/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad.detail;

import android.content.Context;
import android.content.SharedPreferences;
import android.databinding.BindingAdapter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.RetweetUserView;
import com.freshdigitable.udonroad.RoundedCornerImageView;
import com.freshdigitable.udonroad.Utils;
import com.freshdigitable.udonroad.media.ThumbnailContainer;
import com.freshdigitable.udonroad.media.ThumbnailView;
import com.freshdigitable.udonroad.repository.ImageQuery;
import com.freshdigitable.udonroad.repository.ImageRepository;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import twitter4j.MediaEntity;

/**
 * Created by akihit on 2018/01/30.
 */

public class StatusDetailBindingComponent implements android.databinding.DataBindingComponent {
  private final ImageRepository imageRepository;
  private Disposable cardSummaryImageSubs;
  private Disposable rtUserImageSubs;
  private Disposable thumbnailsSubs;
  private Disposable userIconSubs;

  @Inject
  StatusDetailBindingComponent(ImageRepository imageRepository) {
    this.imageRepository = imageRepository;
  }

  @BindingAdapter({"imageUrl", "size"})
  public void bindImage(RoundedCornerImageView imageView, String url, float size) {
    cardSummaryImageSubs = bindImageImpl(imageView, url, size, null);
  }

  @BindingAdapter({"imageUrl", "imageHeight", "imageWidth"})
  public void bindImage(RoundedCornerImageView imageView,
                        String url, float imageHeight, float imageWidth) {
    cardSummaryImageSubs = bindImageImpl(imageView, url, imageHeight, imageWidth, null);
  }

  @BindingAdapter({"imageUrl", "size", "placeholder"})
  public void bindImage(RoundedCornerImageView imageView, String url, float size, Drawable placeholder) {
    userIconSubs = bindImageImpl(imageView, url, size, placeholder);
  }

  private Disposable bindImageImpl(RoundedCornerImageView imageView, String url, float size, Drawable placeholder) {
    return bindImageImpl(imageView, url, size, size, placeholder);
  }

  private Disposable bindImageImpl(RoundedCornerImageView imageView,
                                   String url, float imageHeight, float imageWidth, Drawable placeholder) {
    if (TextUtils.isEmpty(url)) {
      return null;
    }
    final ImageQuery query = new ImageQuery.Builder(url)
        .height((int) imageHeight)
        .width((int) imageWidth)
        .placeholder(placeholder)
        .centerCrop()
        .build();
    return imageRepository.queryImage(query)
        .subscribe(imageView::setImageDrawable, th -> {});
  }

  @BindingAdapter({"isRetweet", "imageUrl", "screenName"})
  public void bindRtUser(RetweetUserView v,
                         boolean isRetweet, String imageUrl, String screenName) {
    if (!isRetweet) {
      return;
    }
    final Context context = v.getContext();
    final ImageQuery query = new ImageQuery.Builder(imageUrl)
        .sizeForSquare(context, R.dimen.small_user_icon)
        .placeholder(context, R.drawable.ic_person_outline_black)
        .build();
    rtUserImageSubs = imageRepository.queryImage(query)
        .subscribe(d -> v.bindUser(d, screenName), th -> {});
  }

  @BindingAdapter({"mediaEntities", "isPossiblySensitive"})
  public void bindThumbnails(ThumbnailContainer thumbnailContainer,
                             MediaEntity[] mediaEntities, boolean isPossiblySensitive) {
    if (mediaEntities == null) {
      return;
    }
    thumbnailContainer.bindMediaEntities(mediaEntities);
    final int mediaCount = thumbnailContainer.getThumbCount();
    if (mediaCount < 1) {
      return;
    }
    for (int i = 0; i < mediaCount; i++) {
      final ThumbnailView mediaView = (ThumbnailView) thumbnailContainer.getChildAt(i);
      final String type = mediaEntities[i].getType();
      mediaView.setShowIcon("video".equals(type) || "animated_gif".equals(type));
    }

    final Context context = thumbnailContainer.getContext();
    if (isPossiblySensitive && isHideSensitive(context)) {
      for (int i = 0; i < mediaCount; i++) {
        final ThumbnailView mediaView = (ThumbnailView) thumbnailContainer.getChildAt(i);
        mediaView.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_whatshot));
      }
    } else {
      thumbnailsSubs = loadThumbnails(thumbnailContainer, mediaEntities);
    }
  }

  private static boolean isHideSensitive(Context context) {
    final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
    final String key = context.getString(R.string.settings_key_sensitive);
    return sp.getBoolean(key, context.getResources().getBoolean(R.bool.settings_sensitive_default));
  }

  private Disposable loadThumbnails(ThumbnailContainer thumbnailContainer, MediaEntity[] entities) {
    final CompositeDisposable compositeDisposable = new CompositeDisposable();
    final int width = thumbnailContainer.getThumbWidth();
    final int height = thumbnailContainer.getHeight();
    final int mediaCount = entities.length;
    for (int i = 0; i < mediaCount; i++) {
      final ThumbnailView mediaView = (ThumbnailView) thumbnailContainer.getChildAt(i);
      final Observable<Drawable> observable = (width > 0 && height > 0) ?
          imageRepository.queryImage(getQueryForThumbnail(entities[i], height, width))
          : imageRepository.queryImage(getQueryForThumbnail(entities[i], mediaView));
      compositeDisposable.add(observable.subscribe(mediaView::setImageDrawable, th -> {}));
    }
    return compositeDisposable;
  }

  private static ImageQuery getQueryForThumbnail(MediaEntity entity, int height, int width) {
    return new ImageQuery.Builder(entity.getMediaURLHttps() + ":thumb")
        .height(height)
        .width(width)
        .placeholder(new ColorDrawable(Color.LTGRAY))
        .centerCrop()
        .build();
  }

  private static Single<ImageQuery> getQueryForThumbnail(MediaEntity entity, View view) {
    return new ImageQuery.Builder(entity.getMediaURLHttps() + ":thumb")
        .placeholder(new ColorDrawable(Color.LTGRAY))
        .centerCrop()
        .build(view);
  }

  void dispose() {
    Utils.maybeDispose(cardSummaryImageSubs);
    Utils.maybeDispose(rtUserImageSubs);
    Utils.maybeDispose(thumbnailsSubs);
    Utils.maybeDispose(userIconSubs);
  }

  @Override
  public StatusDetailBindingComponent getStatusDetailBindingComponent() {
    return this;
  }
}
