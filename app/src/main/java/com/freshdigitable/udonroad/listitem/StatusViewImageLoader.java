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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.preference.PreferenceManager;
import android.view.View;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.RetweetUserView;
import com.freshdigitable.udonroad.media.ThumbnailContainer;
import com.freshdigitable.udonroad.media.ThumbnailView;
import com.freshdigitable.udonroad.repository.ImageQuery;
import com.freshdigitable.udonroad.repository.ImageRepository;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.disposables.EmptyDisposable;
import twitter4j.MediaEntity;
import twitter4j.User;

/**
 * StatusViewImageLoader provides image loader for StatusView.
 *
 * Created by akihit on 2016/08/20.
 */
public class StatusViewImageLoader {
  @SuppressWarnings("unused")
  private static final String TAG = StatusViewImageLoader.class.getSimpleName();

  private final ImageRepository imageRepository;

  @Inject
  StatusViewImageLoader(ImageRepository imageRepository) {
    this.imageRepository = imageRepository;
  }

  public <T extends View & StatusItemView> Disposable load(TwitterListItem item, T statusView) {
    final CompositeDisposable compositeDisposable = new CompositeDisposable();
    compositeDisposable.add(loadUserIcon(item.getUser(), statusView));
    compositeDisposable.add(loadRTUserIcon(item, statusView));
    compositeDisposable.add(loadMediaView(item, statusView));
    compositeDisposable.add(loadQuotedStatusImages(item, statusView.getQuotedStatusView()));
    return compositeDisposable;
  }

  private final Consumer<Throwable> emptyError = th -> {};

  <T extends View & ItemView> Disposable loadUserIcon(User user, final T itemView) {
    if (user == null) {
      return EmptyDisposable.INSTANCE;
    }
    final Context context = itemView.getContext();
    final ImageQuery query = new ImageQuery.Builder(user.getProfileImageURLHttps())
        .sizeForSquare(context, R.dimen.tweet_user_icon)
        .placeholder(context, R.drawable.ic_person_outline_black)
        .build();
    return imageRepository.queryImage(query)
        .subscribe(d -> itemView.getIcon().setImageDrawable(d), emptyError);
  }

  private <T extends View & StatusItemView> Disposable loadRTUserIcon(TwitterListItem item, T itemView) {
    if (!item.isRetweet()) {
      return EmptyDisposable.INSTANCE;
    }
    final User retweetUser = item.getRetweetUser();
    final String screenName = retweetUser.getScreenName();
    final RetweetUserView rtUser = itemView.getRtUser();

    final Context context = itemView.getContext();
    final ImageQuery query = getQueryForSmallIcon(context, retweetUser);
    return imageRepository.queryImage(query)
        .subscribe(d -> rtUser.bindUser(d, screenName), emptyError);
  }

  private Disposable loadMediaView(final TwitterListItem item, final ThumbnailCapable statusView) {
    final MediaEntity[] mediaEntities = item.getMediaEntities();
    final ThumbnailContainer thumbnailContainer = statusView.getThumbnailContainer();
    thumbnailContainer.bindMediaEntities(mediaEntities);
    final int mediaCount = thumbnailContainer.getThumbCount();
    if (mediaCount < 1) {
      return EmptyDisposable.INSTANCE;
    }
    for (int i = 0; i < mediaCount; i++) {
      final ThumbnailView mediaView = (ThumbnailView) thumbnailContainer.getChildAt(i);
      final String type = mediaEntities[i].getType();
      mediaView.setShowIcon("video".equals(type) || "animated_gif".equals(type));
    }

    final Context context = thumbnailContainer.getContext();
    if (item.isPossiblySensitive() && isHideSensitive(context)) {
      for (int i = 0; i < mediaCount; i++) {
        final ThumbnailView mediaView = (ThumbnailView) thumbnailContainer.getChildAt(i);
        mediaView.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_whatshot));
      }
      return EmptyDisposable.INSTANCE;
    } else {
      return loadThumbnails(mediaEntities, thumbnailContainer);
    }
  }

  private static boolean isHideSensitive(Context context) {
    final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
    final String key = context.getString(R.string.settings_key_sensitive);
    return sp.getBoolean(key, false);
  }

  private Disposable loadThumbnails(MediaEntity[] entities, ThumbnailContainer thumbnailContainer) {
    final CompositeDisposable compositeDisposable = new CompositeDisposable();
    final int width = thumbnailContainer.getThumbWidth();
    final int height = thumbnailContainer.getHeight();
    final int mediaCount = entities.length;
    for (int i = 0; i < mediaCount; i++) {
      final ThumbnailView mediaView = (ThumbnailView) thumbnailContainer.getChildAt(i);
      final Observable<Drawable> observable = (width > 0 && height > 0) ?
          imageRepository.queryImage(getQueryForThumbnail(entities[i], height, width))
          : imageRepository.queryImage(getQueryForThumbnail(entities[i], mediaView));
      compositeDisposable.add(observable.subscribe(mediaView::setImageDrawable, emptyError));
    }
    return compositeDisposable;
  }

  private Disposable loadQuotedStatusImages(TwitterListItem item, @Nullable QuotedStatusView quotedStatusView) {
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
      final ImageQuery query = getQueryForSmallIcon(quotedStatusView.getContext(), user);
      final Disposable subs = imageRepository.queryImage(query)
          .subscribe(d -> quotedStatusView.getIcon().setImageDrawable(d), emptyError);
      compositeDisposable.add(subs);
    }
    compositeDisposable.add(loadMediaView((TwitterListItem) quotedStatus, quotedStatusView));
    return compositeDisposable;
  }

  private static ImageQuery getQueryForSmallIcon(Context context, User user) {
    return new ImageQuery.Builder(user.getMiniProfileImageURLHttps())
        .sizeForSquare(context, R.dimen.small_user_icon)
        .placeholder(context, R.drawable.ic_person_outline_black)
        .build();
  }

  private static final ColorDrawable defaultPlaceholder = new ColorDrawable(Color.LTGRAY);

  private static ImageQuery getQueryForThumbnail(MediaEntity entity, int height, int width) {
    return new ImageQuery.Builder(entity.getMediaURLHttps() + ":thumb")
        .height(height)
        .width(width)
        .placeholder(defaultPlaceholder)
        .centerCrop()
        .build();
  }

  private static Single<ImageQuery> getQueryForThumbnail(MediaEntity entity, View view) {
    return new ImageQuery.Builder(entity.getMediaURLHttps() + ":thumb")
        .placeholder(defaultPlaceholder)
        .centerCrop()
        .build(view);
  }
}
