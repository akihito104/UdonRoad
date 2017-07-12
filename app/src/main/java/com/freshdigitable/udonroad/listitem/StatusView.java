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
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.RetweetUserView;
import com.freshdigitable.udonroad.media.ThumbnailContainer;

/**
 * StatusView shows Status data in RecyclerView.
 *
 * Created by akihit on 2016/01/11.
 */
public class StatusView extends ItemView implements TwitterItemView, ThumbnailCapable {
  @SuppressWarnings("unused")
  private static final String TAG = StatusView.class.getSimpleName();
  TextView createdAt;
  TextView clientName;
  ThumbnailContainer thumbnailContainer;
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
    reactionContainer = v.findViewById(R.id.tl_reaction_container);
  }

  public void bind(TwitterListItem item) {
    if (item == null) {
      return;
    }
    timeStrategy = item.getTimeStrategy();
    setTextColor(item.isRetweet()
        ? ContextCompat.getColor(getContext(), R.color.twitter_action_retweeted)
        : Color.GRAY);

    rtUser.setVisibility(item.isRetweet() ? VISIBLE : GONE);
    super.bind(item);
    createdAt.setText(item.getCreatedTime(getContext()));
    thumbnailContainer.bindMediaEntities(item.getMediaCount());
    clientName.setText(formatString(R.string.tweet_via, item.getSource()));

    final TwitterListItem quotedItem = item.getQuotedItem();
    bindQuotedStatus(quotedItem);
  }

  private void bindQuotedStatus(TwitterListItem quotedItem) {
    if (quotedItem == null) {
      if (quotedStatus == null) {
        return;
      }
      quotedStatus.setVisibility(GONE);
    } else {
      if (quotedStatus == null) {
        quotedStatus = new QuotedStatusView(getContext());
        final LayoutParams lp = createQuotedItemLayoutParams();
        addView(quotedStatus, lp);
      }
      quotedStatus.setVisibility(VISIBLE);
      quotedStatus.bind(quotedItem);
    }
  }

  @NonNull
  private LayoutParams createQuotedItemLayoutParams() {
    final LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    lp.topMargin = getResources().getDimensionPixelSize(R.dimen.grid_margin);
    lp.addRule(RelativeLayout.BELOW, R.id.tl_via);
    lp.addRule(RelativeLayout.RIGHT_OF, R.id.tl_icon);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      lp.addRule(RelativeLayout.END_OF, R.id.tl_icon);
    }
    return lp;
  }

  @Override
  public void updateTime() {
    if (timeStrategy == null) {
      return;
    }
    createdAt.setText(timeStrategy.getCreatedTime(getContext()));
    if (quotedStatus != null && quotedStatus.getVisibility() == VISIBLE) {
      quotedStatus.updateTime();
    }
  }

  @Override
  public void update(TwitterListItem item) {
    reactionContainer.update(item.getStats());
    names.setNames(item.getUser());
    if (quotedStatus != null) {
      quotedStatus.update(item.getQuotedItem());
    }
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
    if (quotedStatus != null) {
      quotedStatus.reset();
    }
    thumbnailContainer.reset();
    thumbnailContainer.setOnMediaClickListener(null);
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

  @Nullable
  public QuotedStatusView getQuotedStatusView() {
    return quotedStatus;
  }

  @Override
  public ThumbnailContainer getThumbnailContainer() {
    return thumbnailContainer;
  }
}
