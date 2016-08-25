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
import android.graphics.Color;
import android.support.annotation.CallSuper;
import android.support.annotation.ColorRes;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Date;

import twitter4j.ExtendedMediaEntity;
import twitter4j.Status;
import twitter4j.User;
import twitter4j.util.TimeSpanConverter;

/**
 * Created by akihit on 2016/06/28.
 */
public abstract class StatusViewBase extends RelativeLayout {
  protected TextView createdAt;
  protected ImageView icon;
  protected CombinedScreenNameTextView names;
  protected TextView tweet;
  protected TextView clientName;
  protected ImageView rtIcon;
  protected TextView rtCount;
  protected ImageView favIcon;
  protected TextView favCount;
  protected MediaContainer mediaContainer;
  protected final int grid;
  protected static final TimeSpanConverter timeSpanConv = new TimeSpanConverter();
  protected final int selectedColor;

  public StatusViewBase(Context context) {
    this(context, null);
  }

  public StatusViewBase(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public StatusViewBase(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    grid = getGrid();
    setPadding(grid, grid, grid, grid);
    setBackgroundColor(Color.TRANSPARENT);
    selectedColor = ContextCompat.getColor(context, R.color.colorTwitterActionNormalTransparent);
  }

  protected int getGrid() {
    return getResources().getDimensionPixelSize(R.dimen.grid_margin);
  }

  @CallSuper
  public void bindStatus(final Status status) {
    final Status bindingStatus = getBindingStatus(status);

    bindCreatedAt(bindingStatus.getCreatedAt());
    if (status.isRetweet()) {
      setTextColor(ContextCompat.getColor(getContext(), R.color.colorTwitterActionRetweeted));
    }
    final User user = bindingStatus.getUser();
    bindTweetUserName(user);

    bindText(status);
    bindSource(bindingStatus);

    bindRT(bindingStatus);
    bindFavorite(bindingStatus);

    bindMediaEntities(status);
  }

  protected Date createdAtDate;

  protected void bindCreatedAt(Date bindingStatus) {
    createdAtDate = bindingStatus;
    updateCreatedAt(createdAtDate);
  }

  protected void updateCreatedAt(Date createdAtDate) {
    createdAt.setText(timeSpanConv.toTimeSpanString(createdAtDate));
  }

  public void updateTime() {
    updateCreatedAt(this.createdAtDate);
  }

  protected void bindTweetUserName(User user) {
      names.setNames(user);
  }

  protected void bindText(Status status) {
    final CharSequence text = parseText(status);
    tweet.setText(text);
  }

  protected void bindSource(Status bindingStatus) {
    final String source = bindingStatus.getSource();
    if (source != null) {
      final String formattedVia = formatString(R.string.tweet_via,
          Html.fromHtml(source).toString());
      clientName.setText(formattedVia);
    } else {
      clientName.setText(formatString(R.string.tweet_via, "none provided"));
    }
  }

  protected void bindRT(Status bindingStatus) {
    final int rtCount = bindingStatus.getRetweetCount();
    if (rtCount > 0) {
      this.setRtCountVisibility(VISIBLE);
      setTint(rtIcon, bindingStatus.isRetweeted()
          ? R.color.colorTwitterActionRetweeted
          : R.color.colorTwitterActionNormal);
      this.rtCount.setText(String.valueOf(rtCount));
    }
  }

  protected void bindFavorite(Status bindingStatus) {
    final int favCount = bindingStatus.getFavoriteCount();
    if (favCount > 0) {
      this.setFavCountVisibility(VISIBLE);
      setTint(favIcon, bindingStatus.isFavorited()
          ? R.color.colorTwitterActionFaved
          : R.color.colorTwitterActionNormal);
      this.favCount.setText(String.valueOf(favCount));
    }
  }

  protected void bindMediaEntities(Status status) {
    mediaContainer.bindMediaEntities(status.getExtendedMediaEntities());
  }

  protected Status getBindingStatus(Status status) {
    return status.isRetweet()
        ? status.getRetweetedStatus()
        : status;
  }

  protected void setTint(ImageView view, @ColorRes int color) {
//    Log.d(TAG, "setTint: " + color);
    DrawableCompat.setTint(view.getDrawable(), ContextCompat.getColor(getContext(), color));
  }

  protected void setRtCountVisibility(int visibility) {
    rtIcon.setVisibility(visibility);
    rtCount.setVisibility(visibility);
  }

  protected void setFavCountVisibility(int visibility) {
    favIcon.setVisibility(visibility);
    favCount.setVisibility(visibility);
  }

  protected void setTextColor(int color) {
    names.setTextColor(color);
    createdAt.setTextColor(color);
    tweet.setTextColor(color);
  }

  @CallSuper
  public void reset() {
    setBackgroundColor(Color.TRANSPARENT);
    setRtCountVisibility(GONE);
    setFavCountVisibility(GONE);
    setTextColor(Color.GRAY);

    setTint(rtIcon, R.color.colorTwitterActionNormal);
    setTint(favIcon, R.color.colorTwitterActionNormal);

    icon.setImageDrawable(null);
    icon.setImageResource(android.R.color.transparent);
    icon.setOnClickListener(null);
    setOnClickListener(null);
    setUserIconClickListener(null);

    mediaContainer.reset();
    mediaContainer.setOnMediaClickListener(null);
  }

  public void setUserIconClickListener(OnClickListener userIconClickListener) {
    icon.setOnClickListener(userIconClickListener);
  }

  protected String formatString(@StringRes int id, Object... items) {
    final String format = getResources().getString(id);
    return String.format(format, items);
  }

  public ImageView getIcon() {
    return icon;
  }

  protected abstract CharSequence parseText(Status status);

  protected String removeMediaUrl(String text, ExtendedMediaEntity[] extendedMediaEntities) {
    for (ExtendedMediaEntity eme : extendedMediaEntities) {
      text = text.replace(eme.getURL(), "");
    }
    return text;
  }

  public MediaContainer getMediaContainer() {
    return mediaContainer;
  }

  public abstract void setSelectedColor();

  public abstract void setUnselectedColor();

  interface OnUserIconClickedListener {
    void onClicked(View view, User user);
  }

  public TextView getUserName() {
    return names;
  }
}