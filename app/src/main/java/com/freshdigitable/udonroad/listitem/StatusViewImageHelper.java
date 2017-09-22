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
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.preference.PreferenceManager;
import android.view.View;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.RetweetUserView;
import com.freshdigitable.udonroad.media.ThumbnailContainer;
import com.freshdigitable.udonroad.media.ThumbnailView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

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

  public static <T extends View & StatusItemView> void  load(TwitterListItem item, T statusView) {
    loadUserIcon(item.getUser(), item.getId(), statusView);
    loadRTUserIcon(item, statusView);
    loadMediaView(item, statusView);
    loadQuotedStatusImages(item, statusView.getQuotedStatusView());
  }

  public static <T extends View & StatusItemView> void unload(T itemView, long entityId) {
    Picasso.with(itemView.getContext()).cancelTag(entityId);
  }

  static <T extends View & ItemView> void loadUserIcon(User user, final long tagId, final T itemView) {
    if (user == null) {
      return;
    }
    getRequest(itemView.getContext(), user.getProfileImageURLHttps(), tagId)
        .resizeDimen(R.dimen.tweet_user_icon, R.dimen.tweet_user_icon)
        .placeholder(AppCompatResources.getDrawable(itemView.getContext(), R.drawable.ic_person_outline_black))
        .into(itemView.getIcon());
  }

  private static <T extends View & StatusItemView> void loadRTUserIcon(TwitterListItem item, T itemView) {
    if (!item.isRetweet()) {
      return;
    }
    final Context context = itemView.getContext();
    final User retweetUser = item.getRetweetUser();
    final String miniProfileImageURLHttps = retweetUser.getMiniProfileImageURLHttps();
    final long tag = item.getId();

    final RetweetUserView rtUser = itemView.getRtUser();
    final String screenName = retweetUser.getScreenName();
    rtUser.bindUser(AppCompatResources.getDrawable(context, R.drawable.ic_person_outline_black), screenName);
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
                .placeholder(AppCompatResources.getDrawable(context, R.drawable.ic_person_outline_black))
                .into(target);
          }

          @Override
          public void onError() {
          }
        });
  }

  private static void loadMediaView(final TwitterListItem item, final ThumbnailCapable statusView) {
    final MediaEntity[] mediaEntities = item.getMediaEntities();
    final ThumbnailContainer thumbnailContainer = statusView.getThumbnailContainer();
    thumbnailContainer.bindMediaEntities(mediaEntities);
    final int mediaCount = thumbnailContainer.getThumbCount();
    if (mediaCount < 1) {
      return;
    }

    final Context context = thumbnailContainer.getContext();
    final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
    final String key = context.getString(R.string.settings_key_sensitive);
    final boolean isHideSensitive = sp.getBoolean(key, false);

    final long statusId = item.getId();

    for (int i = 0; i < mediaCount; i++) {
      final ThumbnailView mediaView = (ThumbnailView) thumbnailContainer.getChildAt(i);
      final String type = mediaEntities[i].getType();
      mediaView.setShowIcon("video".equals(type) || "animated_gif".equals(type));

      if (isHideSensitive && item.isPossiblySensitive()) {
        mediaView.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_whatshot));
      } else {
        final RequestCreator rc = getRequest(context,
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
  }

  private static void loadQuotedStatusImages(TwitterListItem item, @Nullable QuotedStatusView quotedStatusView) {
    if (quotedStatusView == null) {
      return;
    }
    final ListItem quotedStatus = item.getQuotedItem();
    if (quotedStatus == null) {
      return;
    }
    final User user = quotedStatus.getUser();
    if (user != null) {
      getRequest(quotedStatusView.getContext(), user.getMiniProfileImageURLHttps(),
          item.getId())
          .resizeDimen(R.dimen.small_user_icon, R.dimen.small_user_icon)
          .placeholder(AppCompatResources.getDrawable(quotedStatusView.getContext(), R.drawable.ic_person_outline_black))
          .into(quotedStatusView.getIcon());
    }
    loadMediaView((TwitterListItem) quotedStatus, quotedStatusView);
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
