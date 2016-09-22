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

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Transformation;

import twitter4j.ExtendedMediaEntity;
import twitter4j.Status;
import twitter4j.User;

/**
 * StatusViewImageHelper provides image loader for StatusView.
 *
 * Created by akihit on 2016/08/20.
 */
public class StatusViewImageHelper {
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

  private static RoundedCornerTrans userIconTransformer = new RoundedCornerTrans(10);

  private static void loadUserIcon(User user, final long tagId, final FullStatusView itemView) {
    Picasso.with(itemView.getContext())
        .load(user.getProfileImageURLHttps())
        .tag(tagId)
        .fit()
        .transform(userIconTransformer)
        .into(itemView.getIcon());
  }

  private static void unloadUserIcon(FullStatusView itemView) {
    unloadImage(itemView.getIcon());
  }

  public static User getBindingUser(Status status) {
    return getBindingStatus(status).getUser();
  }

  public static Status getBindingStatus(Status status) {
    return status.isRetweet()
        ? status.getRetweetedStatus()
        : status;
  }

  private static RoundedCornerTrans smallUserIconTransformer = new RoundedCornerTrans(4);

  private static void loadRTUserIcon(Status status, FullStatusView itemView) {
    if (!status.isRetweet()) {
      return;
    }
    Picasso.with(itemView.getContext())
        .load(status.getUser().getMiniProfileImageURLHttps())
        .tag(status.getId())
        .fit()
        .transform(smallUserIconTransformer)
        .into(itemView.getRtUserIcon());
  }

  private static void unloadRTUserIcon(FullStatusView itemView) {
    unloadImage(itemView.getRtUserIcon());
  }

  private static void loadMediaView(final Status status, final StatusViewBase statusView) {
    final ExtendedMediaEntity[] extendedMediaEntities
        = getBindingStatus(status).getExtendedMediaEntities();
    final MediaContainer mediaContainer = statusView.getMediaContainer();
    mediaContainer.bindMediaEntities(extendedMediaEntities);
    final int mediaCount = mediaContainer.getThumbCount();
    final long statusId = status.getId();
    for (int i = 0; i < mediaCount; i++) {
      final MediaImageView mediaView = (MediaImageView) mediaContainer.getChildAt(i);
      final String type = extendedMediaEntities[i].getType();
      mediaView.setShowIcon("video".equals(type) || "animated_gif".equals(type));

      final RequestCreator rc = Picasso.with(mediaContainer.getContext())
          .load(extendedMediaEntities[i].getMediaURLHttps() + ":thumb")
          .tag(statusId);
      if (mediaContainer.getHeight() == 0 || mediaContainer.getThumbWidth() == 0) {
        rc.fit();
      } else {
        rc.resize(mediaContainer.getThumbWidth(), mediaContainer.getHeight());
      }
      rc.centerCrop()
          .into(mediaView);
    }
  }

  private static void unloadMediaView(StatusViewBase statusView) {
    final MediaContainer mediaContainer = statusView.getMediaContainer();
    final int thumbCount = mediaContainer.getThumbCount();
    for (int i=0; i<thumbCount;i++) {
      final MediaImageView media = (MediaImageView) mediaContainer.getChildAt(i);
      unloadImage(media);
    }
  }

  private static void loadQuotedStatusImages(Status status, QuotedStatusView quotedStatusView) {
    final Status quotedStatus = getBindingStatus(status).getQuotedStatus();
    if (quotedStatus == null) {
      return;
    }
    Picasso.with(quotedStatusView.getContext())
        .load(quotedStatus.getUser().getMiniProfileImageURLHttps())
        .tag(status.getId())
        .fit()
        .transform(smallUserIconTransformer)
        .into(quotedStatusView.getIcon());
    loadMediaView(quotedStatus, quotedStatusView);
  }

  private static void unloadQuotedStatusImages(QuotedStatusView quotedStatusView) {
    unloadImage(quotedStatusView.getIcon());
    unloadMediaView(quotedStatusView);
  }

  private static void unloadImage(ImageView v) {
    v.setImageDrawable(null);
    v.setImageResource(android.R.color.transparent);
  }

  private StatusViewImageHelper() {
    throw new AssertionError();
  }

  private static class RoundedCornerTrans implements Transformation {
    private final float radius;
    private final String key;
    private final Paint paint;

    private RoundedCornerTrans(float radius) {
      this.radius = radius;
      this.key = "roundedCorner(" + Float.toString(radius) + ")";
      this.paint = new Paint();
      this.paint.setAntiAlias(true);
    }

    @Override
    public Bitmap transform(Bitmap source) {
      final int width = source.getWidth();
      final int height = source.getHeight();
      final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
      final Canvas canvas = new Canvas(bitmap);
      paint.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
      canvas.drawRoundRect(new RectF(0, 0, width, height), radius, radius, paint);
      source.recycle();
      return bitmap;
    }

    @Override
    public String key() {
      return key;
    }
  }
}
