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

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

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

  private static void loadUserIcon(User user, long tagId, FullStatusView itemView) {
    Picasso.with(itemView.getContext())
        .load(user.getProfileImageURLHttps())
        .placeholder(android.R.color.transparent)
        .tag(tagId)
        .fit()
        .into(itemView.getIcon());
  }

  private static void unloadUserIcon(FullStatusView itemView) {
    itemView.getIcon().setImageDrawable(null);
  }

  public static User getBindingUser(Status status) {
    return status.isRetweet()
        ? status.getRetweetedStatus().getUser()
        : status.getUser();
  }

  private static void loadRTUserIcon(Status status, FullStatusView itemView) {
    if (!status.isRetweet()) {
      return;
    }
    Picasso.with(itemView.getContext())
        .load(status.getUser().getMiniProfileImageURLHttps())
        .placeholder(android.R.color.transparent)
        .tag(status.getId())
        .fit()
        .into(itemView.getRtUserIcon());
  }

  private static void unloadRTUserIcon(FullStatusView itemView) {
    itemView.getRtUserIcon().setImageDrawable(null);
  }

  private static void loadMediaView(final Status status, final StatusViewBase statusView) {
    ExtendedMediaEntity[] extendedMediaEntities = status.getExtendedMediaEntities();
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
          .placeholder(android.R.color.transparent)
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
      final MediaImageView childAt = (MediaImageView) mediaContainer.getChildAt(i);
      childAt.setImageDrawable(null);
    }
  }

  private static void loadQuotedStatusImages(Status status, QuotedStatusView quotedStatusView) {
    final Status quotedStatus = status.isRetweet()
        ? status.getRetweetedStatus().getQuotedStatus()
        : status.getQuotedStatus();
    if (quotedStatus == null) {
      return;
    }
    Picasso.with(quotedStatusView.getContext())
        .load(quotedStatus.getUser().getMiniProfileImageURLHttps())
        .placeholder(android.R.color.transparent)
        .tag(status.getId())
        .fit()
        .into(quotedStatusView.getIcon());
    loadMediaView(quotedStatus, quotedStatusView);
  }

  private static void unloadQuotedStatusImages(QuotedStatusView quotedStatusView) {
    quotedStatusView.getIcon().setImageDrawable(null);
    unloadMediaView(quotedStatusView);
  }

  private StatusViewImageHelper() {
    throw new AssertionError();
  }
}
