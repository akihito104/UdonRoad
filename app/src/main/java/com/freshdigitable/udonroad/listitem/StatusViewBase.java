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
import android.graphics.Color;
import android.support.annotation.CallSuper;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.freshdigitable.udonroad.CombinedScreenNameTextView;
import com.freshdigitable.udonroad.IconAttachedTextView;
import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.media.ThumbnailContainer;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.User;
import twitter4j.util.TimeSpanConverter;

import static com.freshdigitable.udonroad.Utils.getBindingStatus;

/**
 * StatusViewBase defines how to bind Status and StatusView.
 *
 * Created by akihit on 2016/06/28.
 */
public abstract class StatusViewBase extends RelativeLayout {
  TextView createdAt;
  ImageView icon;
  CombinedScreenNameTextView names;
  TextView tweet;
  TextView clientName;
  IconAttachedTextView rtCount;
  IconAttachedTextView favCount;
  ThumbnailContainer thumbnailContainer;
  ImageView hasReplyIcon;
  final int grid;
  static final TimeSpanConverter timeSpanConv = new TimeSpanConverter();
  final int selectedColor;

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
    selectedColor = ContextCompat.getColor(context, R.color.twitter_action_normal_transparent);
  }

  protected int getGrid() {
    return getResources().getDimensionPixelSize(R.dimen.grid_margin);
  }

  @CallSuper
  public void bind(ListItem item) {
    if (item == null) {
      return;
    }
    names.setNames(item.getUser());
    createdAt.setText(item.getCreatedTime(getContext()));
    tweet.setText(item.getText());
    thumbnailContainer.bindMediaEntities(item.getMediaCount());

    for (ListItem.Stat s : item.getStats()) {
      if (s.getType() == R.drawable.ic_retweet) {
        rtCount.setVisibility(s.getCount() > 0 || s.isMarked() ? VISIBLE : GONE);
        rtCount.tintIcon(s.isMarked()
            ? R.color.twitter_action_retweeted
            : R.color.twitter_action_normal);
        rtCount.setText(String.valueOf(s.getCount()));
      } else if (s.getType() == R.drawable.ic_like) {
        favCount.setVisibility(s.getCount() > 0 || s.isMarked() ? VISIBLE : GONE);
        favCount.tintIcon(s.isMarked()
            ? R.color.twitter_action_faved
            : R.color.twitter_action_normal);
        favCount.setText(String.valueOf(s.getCount()));
      } else if (s.getType() == R.drawable.ic_forum) {
        hasReplyIcon.setVisibility(s.isMarked() ? VISIBLE : GONE);
      }
    }

    clientName.setText(formatString(R.string.tweet_via, item.getSource()));
  }

  @Deprecated
  @CallSuper
  public void bindStatus(final Status status) {
    final Status bindingStatus = getBindingStatus(status);

    bindCreatedAt(bindingStatus.getCreatedAt());
    if (status.isRetweet()) {
      setTextColor(ContextCompat.getColor(getContext(), R.color.twitter_action_retweeted));
    }
    final User user = bindingStatus.getUser();
    bindTweetUserName(user);

    bindText(status);
    bindSource(bindingStatus);

    bindRT(bindingStatus);
    bindFavorite(bindingStatus);
    bindHasReply(bindingStatus);

    bindMediaEntities(status);
  }

  @Deprecated
  @CallSuper
  public void update(Status status) {
    final Status bindingStatus = getBindingStatus(status);
    bindRT(bindingStatus);
    bindFavorite(bindingStatus);
    final User user = bindingStatus.getUser();
    bindTweetUserName(user);
  }

  @Deprecated
  @CallSuper
  public void bindUser(final User user) {
    bindTweetUserName(user);
    tweet.setText(user.getDescription());
    createdAt.setVisibility(GONE);
    clientName.setVisibility(GONE);
  }

  Date createdAtDate;

  void bindCreatedAt(Date bindingStatus) {
    createdAtDate = bindingStatus;
    updateCreatedAt(createdAtDate);
  }

  void updateCreatedAt(Date createdAtDate) {
    if (createdAtDate == null) {
      return;
    }
    final String text = createTimeString(createdAtDate);
    createdAt.setText(text);
  }

  private String createTimeString(Date createdAtDate) {
    final long deltaInSec = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - createdAtDate.getTime());
    if (deltaInSec <= TimeUnit.SECONDS.toSeconds(1)) {
      return getContext().getString(R.string.created_now);
    }
    if (deltaInSec < TimeUnit.SECONDS.toSeconds(60)) {
      return getContext().getString(R.string.created_seconds_ago, TimeUnit.SECONDS.toSeconds(deltaInSec));
    }
    if (deltaInSec <= TimeUnit.MINUTES.toSeconds(1)) {
      return getContext().getString(R.string.created_a_minute_ago);
    }
    if (deltaInSec < TimeUnit.MINUTES.toSeconds(45)) {
      return getContext().getString(R.string.created_minutes_ago, TimeUnit.SECONDS.toMinutes(deltaInSec));
    }
    if (deltaInSec < TimeUnit.MINUTES.toSeconds(105)) {
      return getContext().getString(R.string.created_a_hour_ago);
    }
    if (deltaInSec < TimeUnit.DAYS.toSeconds(1)) {
      long hours = deltaInSec + TimeUnit.MINUTES.toSeconds(15);
      return getContext().getString(R.string.created_hours_ago, TimeUnit.SECONDS.toHours(hours));
    }
    return timeSpanConv.toTimeSpanString(createdAtDate);
  }

  public void updateTime() {
    updateCreatedAt(this.createdAtDate);
  }

  void bindTweetUserName(User user) {
      names.setNames(user);
  }

  void bindText(Status status) {
    final CharSequence text = parseText(status);
    tweet.setText(text);
  }

  void bindSource(Status bindingStatus) {
    final String source = bindingStatus.getSource();
    if (source != null) {
      final String formattedVia = formatString(R.string.tweet_via,
          Html.fromHtml(source).toString());
      clientName.setText(formattedVia);
    } else {
      clientName.setText(formatString(R.string.tweet_via, "none provided"));
    }
  }

  void bindRT(Status bindingStatus) {
    final int rtCount = bindingStatus.getRetweetCount();
    if (rtCount > 0 || bindingStatus.isRetweeted()) {
      this.rtCount.setVisibility(VISIBLE);
      this.rtCount.tintIcon(bindingStatus.isRetweeted()
          ? R.color.twitter_action_retweeted
          : R.color.twitter_action_normal);
      this.rtCount.setText(String.valueOf(rtCount));
    }
  }

  void bindFavorite(Status bindingStatus) {
    final int favCount = bindingStatus.getFavoriteCount();
    if (favCount > 0 || bindingStatus.isFavorited()) {
      this.favCount.setVisibility(VISIBLE);
      this.favCount.tintIcon(bindingStatus.isFavorited()
          ? R.color.twitter_action_faved
          : R.color.twitter_action_normal);
      this.favCount.setText(String.valueOf(favCount));
    }
  }

  private void bindHasReply(Status bindingStatus) {
    final long inReplyToStatusId = bindingStatus.getInReplyToStatusId();
    hasReplyIcon.setVisibility(inReplyToStatusId > 0 ? VISIBLE : GONE);
  }

  void bindMediaEntities(Status status) {
    final MediaEntity[] mediaEntities = getBindingStatus(status).getMediaEntities();
    thumbnailContainer.bindMediaEntities(mediaEntities);
  }

  void setTextColor(int color) {
    names.setTextColor(color);
    createdAt.setTextColor(color);
    tweet.setTextColor(color);
  }

  @CallSuper
  public void reset() {
    setBackgroundColor(Color.TRANSPARENT);
    rtCount.setVisibility(GONE);
    favCount.setVisibility(GONE);
    hasReplyIcon.setVisibility(GONE);
    setTextColor(Color.GRAY);

    rtCount.tintIcon(R.color.twitter_action_normal);
    favCount.tintIcon(R.color.twitter_action_normal);

    icon.setImageDrawable(null);
    icon.setImageResource(android.R.color.transparent);
    icon.setOnClickListener(null);
    setOnClickListener(null);

    thumbnailContainer.reset();
    thumbnailContainer.setOnMediaClickListener(null);
  }

  String formatString(@StringRes int id, Object... items) {
    final String format = getResources().getString(id);
    return String.format(format, items);
  }

  public ImageView getIcon() {
    return icon;
  }

  @Deprecated
  abstract CharSequence parseText(Status status);

  String removeMediaUrl(String text, MediaEntity[] mediaEntities) {
    for (MediaEntity me : mediaEntities) {
      text = text.replace(me.getURL(), "");
    }
    return text;
  }

  public ThumbnailContainer getThumbnailContainer() {
    return thumbnailContainer;
  }

  public abstract void setSelectedColor();

  public abstract void setUnselectedColor();

  public TextView getUserName() {
    return names;
  }
}