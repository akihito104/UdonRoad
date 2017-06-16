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
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.freshdigitable.udonroad.R;

import twitter4j.Status;
import twitter4j.URLEntity;

import static com.freshdigitable.udonroad.Utils.getBindingStatus;

/**
 * StatusView shows Status data in RecyclerView.
 *
 * Created by akihit on 2016/01/11.
 */
public class StatusView extends FullStatusView {
  @SuppressWarnings("unused")
  private static final String TAG = StatusView.class.getSimpleName();
  private TwitterListItem.TimeTextStrategy timeStrategy;

  public StatusView(Context context) {
    this(context, null);
  }

  public StatusView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public StatusView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    final View v = View.inflate(context, R.layout.view_status, this);
    createdAt = v.findViewById(R.id.tl_create_at);
    icon = v.findViewById(R.id.tl_icon);
    names = v.findViewById(R.id.tl_names);
    tweet = v.findViewById(R.id.tl_tweet);
    clientName = v.findViewById(R.id.tl_via);
    rtCount = v.findViewById(R.id.tl_rtcount);
    favCount = v.findViewById(R.id.tl_favcount);
    hasReplyIcon = v.findViewById(R.id.tl_has_reply);
    rtUser = v.findViewById(R.id.tl_rt_user);
    thumbnailContainer = v.findViewById(R.id.tl_image_group);
    quotedStatus = v.findViewById(R.id.tl_quoted);
  }

  public void bind(TwitterListItem item) {
    if (item == null) {
      return;
    }
    if (TextUtils.isEmpty(item.getSource())) {
      clientName.setVisibility(GONE);
    }
    timeStrategy = item.getTimeStrategy();
    if (timeStrategy == null) {
      createdAt.setVisibility(GONE);
    }
    setTextColor(item.isRetweet()
        ? ContextCompat.getColor(getContext(), R.color.twitter_action_retweeted)
        : Color.GRAY);

    rtUser.setVisibility(item.isRetweet() ? VISIBLE : GONE);
    super.bind(item);
    final ListItem quotedItem = item.getQuotedItem();
    if (quotedItem != null) {
      quotedStatus.setVisibility(VISIBLE);
      quotedStatus.bind(quotedItem);
    } else {
      quotedStatus.setVisibility(GONE);
    }
  }

  @Override
  public void updateTime() {
    if (timeStrategy == null) {
      return;
    }
    createdAt.setText(timeStrategy.getCreatedTime(getContext()));
    if (quotedStatus.getVisibility() == VISIBLE) {
      quotedStatus.updateTime();
    }
  }

  public void update(TwitterListItem item) {
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
      }
    }
    names.setNames(item.getUser());
  }

  void setTextColor(int color) {
    names.setTextColor(color);
    createdAt.setTextColor(color);
    tweet.setTextColor(color);
  }

  @Override
  protected String parseText(Status status) {
    final Status bindingStatus = getBindingStatus(status);
    String text = bindingStatus.getText();
    final String quotedStatusIdStr = Long.toString(bindingStatus.getQuotedStatusId());
    final URLEntity[] urlEntities = bindingStatus.getURLEntities();
    for (URLEntity u : urlEntities) {
      if (bindingStatus.getQuotedStatus() != null
          && u.getExpandedURL().contains(quotedStatusIdStr)) {
        text = text.replace(u.getURL(), "");
      } else {
        text = text.replace(u.getURL(), u.getDisplayURL());
      }
    }
    return removeMediaUrl(text, bindingStatus.getMediaEntities());
  }

  @Override
  public String toString() {
    final CharSequence text = tweet.getText();
    final CharSequence cs = text.length() > 10 ? text.subSequence(0, 9) : text;
    return "height: " + getHeight()
        + ", text: " + cs;
  }

  @Override
  public void setSelectedColor() {
    setBackgroundColor(selectedColor);
  }

  @Override
  public void setUnselectedColor() {
    setBackgroundColor(Color.TRANSPARENT);
  }
}
