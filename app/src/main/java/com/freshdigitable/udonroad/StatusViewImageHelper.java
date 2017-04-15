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

package com.freshdigitable.udonroad;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.widget.ImageView;

import com.freshdigitable.udonroad.media.ThumbnailContainer;
import com.freshdigitable.udonroad.media.ThumbnailView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.User;

import static com.freshdigitable.udonroad.Utils.getBindingStatus;

/**
 * StatusViewImageHelper provides image loader for StatusView.
 *
 * Created by akihit on 2016/08/20.
 */
public class StatusViewImageHelper {
  @SuppressWarnings("unused")
  private static final String TAG = StatusViewImageHelper.class.getSimpleName();

  public static void load(Status status, FullStatusView itemView) {
    loadUserIcon(status, itemView);
    loadRTUserIcon(status, itemView);
    loadMediaView(status, itemView);
    loadQuotedStatusImages(status, itemView.getQuotedStatusView());
  }

  public static void load(User user, FullStatusView itemView) {
    loadUserIcon(user, user.getId(), itemView);
  }

  public static void unload(FullStatusView itemView, long entityId) {
    Picasso.with(itemView.getContext()).cancelTag(entityId);
    unloadRTUserIcon(itemView);
    unloadUserIcon(itemView);
    unloadMediaView(itemView);
    unloadQuotedStatusImages(itemView.getQuotedStatusView());
  }

  private static void loadUserIcon(Status status, FullStatusView itemView) {
    final User user = getBindingUser(status);
    loadUserIcon(user, status.getId(), itemView);
  }

  private static void loadUserIcon(User user, final long tagId, final FullStatusView itemView) {
    getRequest(itemView.getContext(), user.getProfileImageURLHttps(), tagId)
        .resizeDimen(R.dimen.tweet_user_icon, R.dimen.tweet_user_icon)
        .placeholder(R.drawable.ic_person_outline_black)
        .into(itemView.getIcon());
  }

  private static void unloadUserIcon(FullStatusView itemView) {
    unloadImage(itemView.getIcon());
  }

  private static User getBindingUser(Status status) {
    return getBindingStatus(status).getUser();
  }

  private static void loadRTUserIcon(Status status, FullStatusView itemView) {
    if (!status.isRetweet()) {
      return;
    }
    final Context context = itemView.getContext();
    final String miniProfileImageURLHttps = status.getUser().getMiniProfileImageURLHttps();
    final long tag = status.getId();

    final RetweetUserView rtUser = itemView.getRtUser();
    final String screenName = status.getUser().getScreenName();
    rtUser.bindUser(ContextCompat.getDrawable(context, R.drawable.ic_person_outline_black), screenName);
    final Target target = new Target() {
      @Override
      public void onPrepareLoad(Drawable placeHolderDrawable) {
        rtUser.bindUser(placeHolderDrawable, screenName);
      }

      @Override
      public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        rtUser.bindUser(bitmap, screenName);
      }

      @Override
      public void onBitmapFailed(Drawable errorDrawable) {
      }
    };

    // workaround: to prevent target object will be garbage collected
    // because of wrapped with WeakReference.
    getRequest(context, miniProfileImageURLHttps, tag)
        .resizeDimen(R.dimen.small_user_icon, R.dimen.small_user_icon)
        .fetch(new Callback() {
          @Override
          public void onSuccess() {
            getRequest(context, miniProfileImageURLHttps, tag)
                .resizeDimen(R.dimen.small_user_icon, R.dimen.small_user_icon)
                .placeholder(R.drawable.ic_person_outline_black)
                .into(target);
          }

          @Override
          public void onError() {
          }
        });
  }

  private static void unloadRTUserIcon(FullStatusView itemView) {
    itemView.getRtUser().setText("");
  }

  private static void loadMediaView(final Status status, final StatusViewBase statusView) {
    final MediaEntity[] mediaEntities = getBindingStatus(status).getMediaEntities();
    final ThumbnailContainer thumbnailContainer = statusView.getThumbnailContainer();
    thumbnailContainer.bindMediaEntities(mediaEntities);
    final int mediaCount = thumbnailContainer.getThumbCount();
    final long statusId = status.getId();
    for (int i = 0; i < mediaCount; i++) {
      final ThumbnailView mediaView = (ThumbnailView) thumbnailContainer.getChildAt(i);
      final String type = mediaEntities[i].getType();
      mediaView.setShowIcon("video".equals(type) || "animated_gif".equals(type));

      final RequestCreator rc = getRequest(thumbnailContainer.getContext(),
          mediaEntities[i].getMediaURLHttps() + ":thumb", statusId);
      if (thumbnailContainer.getHeight() == 0 || thumbnailContainer.getThumbWidth() == 0) {
        rc.fit();
      } else {
        rc.resize(thumbnailContainer.getThumbWidth(), thumbnailContainer.getHeight());
      }
      rc.centerCrop()
          .into(mediaView);
    }
  }

  private static void unloadMediaView(StatusViewBase statusView) {
    final ThumbnailContainer thumbnailContainer = statusView.getThumbnailContainer();
    final int thumbCount = thumbnailContainer.getThumbCount();
    for (int i=0; i<thumbCount;i++) {
      final ThumbnailView media = (ThumbnailView) thumbnailContainer.getChildAt(i);
      unloadImage(media);
    }
  }

  private static void loadQuotedStatusImages(Status status, QuotedStatusView quotedStatusView) {
    final Status quotedStatus = getBindingStatus(status).getQuotedStatus();
    if (quotedStatus == null) {
      return;
    }
    getRequest(quotedStatusView.getContext(), quotedStatus.getUser().getMiniProfileImageURLHttps(),
        status.getId())
        .resizeDimen(R.dimen.small_user_icon, R.dimen.small_user_icon)
        .placeholder(R.drawable.ic_person_outline_black)
        .into(quotedStatusView.getIcon());
    loadMediaView(quotedStatus, quotedStatusView);
  }

  private static void unloadQuotedStatusImages(QuotedStatusView quotedStatusView) {
    unloadImage(quotedStatusView.getIcon());
    unloadMediaView(quotedStatusView);
  }

  private static void unloadImage(ImageView v) {
    v.setImageDrawable(null);
  }

  private static RequestCreator getRequest(@NonNull Context context,
                                           @NonNull String url,
                                           long tag) {
    return Picasso.with(context)
        .load(url)
        .tag(tag);
  }

  private StatusViewImageHelper() {
    throw new AssertionError();
  }
}
