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

package com.freshdigitable.udonroad.listitem;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.preference.PreferenceManager;
import android.view.View;
import android.view.ViewTreeObserver;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.RetweetUserView;
import com.freshdigitable.udonroad.media.ThumbnailContainer;
import com.freshdigitable.udonroad.media.ThumbnailView;
import com.freshdigitable.udonroad.repository.ImageRepository;
import com.squareup.picasso.Picasso;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.EmptyDisposable;
import twitter4j.MediaEntity;
import twitter4j.User;

/**
 * StatusViewImageHelper provides image loader for StatusView.
 *
 * Created by akihit on 2016/08/20.
 */
public class StatusViewImageHelper {
  @SuppressWarnings("unused")
  private static final String TAG = StatusViewImageHelper.class.getSimpleName();

  public static <T extends View & StatusItemView> Disposable load(TwitterListItem item, T statusView, ImageRepository imageRepository) {
    final CompositeDisposable compositeDisposable = new CompositeDisposable();
    compositeDisposable.add(loadUserIcon(item.getUser(), item.getId(), statusView, imageRepository));
    compositeDisposable.add(loadRTUserIcon(item, statusView, imageRepository));
    compositeDisposable.add(loadMediaView(item, statusView, imageRepository));
    compositeDisposable.add(loadQuotedStatusImages(item, statusView.getQuotedStatusView(), imageRepository));
    return compositeDisposable;
  }

  public static <T extends View & StatusItemView> void unload(T itemView, long entityId) {
    Picasso.with(itemView.getContext()).cancelTag(entityId);
  }

  static <T extends View & ItemView> Disposable loadUserIcon(
      User user, final long tagId, final T itemView, ImageRepository imageRepository) {
    if (user == null) {
      return EmptyDisposable.INSTANCE;
    }
    return imageRepository.queryUserIcon(user, tagId)
        .subscribe(d -> itemView.getIcon().setImageDrawable(d));
  }

  private static <T extends View & StatusItemView> Disposable loadRTUserIcon(
      TwitterListItem item, T itemView, ImageRepository imageRepository) {
    if (!item.isRetweet()) {
      return EmptyDisposable.INSTANCE;
    }
    final User retweetUser = item.getRetweetUser();
    final String screenName = retweetUser.getScreenName();
    final long tag = item.getId();
    final RetweetUserView rtUser = itemView.getRtUser();
    return imageRepository.querySmallUserIcon(retweetUser, tag)
        .subscribe(d -> rtUser.bindUser(d, screenName));
  }

  private static Disposable loadMediaView(
      final TwitterListItem item, final ThumbnailCapable statusView, ImageRepository imageRepository) {
    final MediaEntity[] mediaEntities = item.getMediaEntities();
    final ThumbnailContainer thumbnailContainer = statusView.getThumbnailContainer();
    thumbnailContainer.bindMediaEntities(mediaEntities);
    final int mediaCount = thumbnailContainer.getThumbCount();
    if (mediaCount < 1) {
      return EmptyDisposable.INSTANCE;
    }

    final Context context = thumbnailContainer.getContext();
    final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
    final String key = context.getString(R.string.settings_key_sensitive);
    final boolean isHideSensitive = sp.getBoolean(key, false);

    if (isHideSensitive && item.isPossiblySensitive()) {
      for (int i = 0; i < mediaCount; i++) {
        final ThumbnailView mediaView = (ThumbnailView) thumbnailContainer.getChildAt(i);
        final String type = mediaEntities[i].getType();
        mediaView.setShowIcon("video".equals(type) || "animated_gif".equals(type));
        mediaView.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_whatshot));
      }
      return EmptyDisposable.INSTANCE;
    } else {
      return loadThumbnails(mediaEntities, thumbnailContainer, item.getId(), imageRepository);
    }
  }

  private static Disposable loadThumbnails(
      MediaEntity[] entities, ThumbnailContainer thumbnailContainer, long statusId,
      ImageRepository imageRepository) {
    final int width = thumbnailContainer.getThumbWidth();
    final int height = thumbnailContainer.getHeight();
    if (width <= 0 || height <= 0) {
      return Single.<ThumbnailContainer>create(e -> thumbnailContainer.getViewTreeObserver().addOnGlobalLayoutListener(
          new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
              if (thumbnailContainer.getHeight() > 0 && thumbnailContainer.getThumbWidth() > 0) {
                e.onSuccess(thumbnailContainer);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                  thumbnailContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                  thumbnailContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
              }
            }
          }))
          .subscribe(tc -> loadThumbnails(entities, tc, statusId, imageRepository));
    }

    final CompositeDisposable compositeDisposable = new CompositeDisposable();
    final int mediaCount = entities.length;
    for (int i = 0; i < mediaCount; i++) {
      final ThumbnailView mediaView = (ThumbnailView) thumbnailContainer.getChildAt(i);
      final String type = entities[i].getType();
      mediaView.setShowIcon("video".equals(type) || "animated_gif".equals(type));
      final Observable<Drawable> observable = imageRepository.queryMediaThumbnail(entities[i], height, width, statusId);
      compositeDisposable.add(observable.subscribe(mediaView::setImageDrawable));
    }
    return compositeDisposable;
  }

  private static Disposable loadQuotedStatusImages(
      TwitterListItem item, @Nullable QuotedStatusView quotedStatusView, ImageRepository imageRepository) {
    if (quotedStatusView == null) {
      return EmptyDisposable.INSTANCE;
    }
    final ListItem quotedStatus = item.getQuotedItem();
    if (quotedStatus == null) {
      return EmptyDisposable.INSTANCE;
    }
    final User user = quotedStatus.getUser();
    final CompositeDisposable compositeDisposable = new CompositeDisposable();
    if (user != null) {
      compositeDisposable.add(imageRepository.querySmallUserIcon(user, item.getId())
          .subscribe(d -> quotedStatusView.getIcon().setImageDrawable(d)));
    }
    compositeDisposable.add(loadMediaView((TwitterListItem) quotedStatus, quotedStatusView, imageRepository));
    return compositeDisposable;
  }

  private StatusViewImageHelper() {
    throw new AssertionError();
  }
}
