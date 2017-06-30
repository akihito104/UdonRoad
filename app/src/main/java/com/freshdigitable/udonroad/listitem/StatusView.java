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
import com.freshdigitable.udonroad.RetweetUserView;

/**
 * StatusView shows Status data in RecyclerView.
 *
 * Created by akihit on 2016/01/11.
 */
public class StatusView extends ItemView {
  @SuppressWarnings("unused")
  private static final String TAG = StatusView.class.getSimpleName();
  QuotedStatusView quotedStatus;
  RetweetUserView rtUser;
  private TwitterListItem.TimeTextStrategy timeStrategy;

  public StatusView(Context context) {
    this(context, null);
  }

  public StatusView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public StatusView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  void init(Context context) {
    final View v = View.inflate(context, R.layout.view_status, this);
    createdAt = v.findViewById(R.id.tl_create_at);
    icon = v.findViewById(R.id.tl_icon);
    names = v.findViewById(R.id.tl_names);
    tweet = v.findViewById(R.id.tl_tweet);
    clientName = v.findViewById(R.id.tl_via);
    rtUser = v.findViewById(R.id.tl_rt_user);
    thumbnailContainer = v.findViewById(R.id.tl_image_group);
    quotedStatus = v.findViewById(R.id.tl_quoted);
    reactionContainer = v.findViewById(R.id.tl_reaction_container);
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
    reactionContainer.update(item.getStats());
    names.setNames(item.getUser());
    quotedStatus.update(item.getQuotedItem());
  }

  void setTextColor(int color) {
    names.setTextColor(color);
    createdAt.setTextColor(color);
    tweet.setTextColor(color);
  }

  @Override
  public void reset() {
    super.reset();
    rtUser.setText("");
    quotedStatus.reset();
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

  public RetweetUserView getRtUser() {
    return rtUser;
  }

  public QuotedStatusView getQuotedStatusView() {
    return quotedStatus;
  }
}