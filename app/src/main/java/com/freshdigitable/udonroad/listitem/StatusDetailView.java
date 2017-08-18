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
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.freshdigitable.udonroad.CombinedScreenNameTextView;
import com.freshdigitable.udonroad.LinkableTextView;
import com.freshdigitable.udonroad.OnSpanClickListener;
import com.freshdigitable.udonroad.OnSpanClickListener.SpanItem;
import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.RetweetUserView;
import com.freshdigitable.udonroad.media.ThumbnailContainer;

import java.util.List;

/**
 * StatusDetailView is a View to show in StatusDetailFragment.
 *
 * Created by akihit on 2016/08/18.
 */
public class StatusDetailView extends RelativeLayout implements StatusItemView {
  final ImageView icon;
  final CombinedScreenNameTextView names;
  final TextView tweet;
  final ReactionContainer reactionContainer;
  final TextView createdAt;
  final TextView clientName;
  final ThumbnailContainer thumbnailContainer;
  QuotedStatusView quotedStatus;
  final RetweetUserView rtUser;

  public StatusDetailView(Context context) {
    this(context, null);
  }

  public StatusDetailView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public StatusDetailView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    final View v = View.inflate(context, R.layout.view_status_detail, this);
    createdAt = v.findViewById(R.id.d_create_at);
    icon = v.findViewById(R.id.d_icon);
    names = v.findViewById(R.id.d_names);
    tweet = (LinkableTextView) v.findViewById(R.id.d_tweet);
    clientName = v.findViewById(R.id.d_via);
    thumbnailContainer = v.findViewById(R.id.d_image_group);
    rtUser = v.findViewById(R.id.d_rt_user);
    quotedStatus = v.findViewById(R.id.d_quoted);
    reactionContainer = v.findViewById(R.id.d_reaction_container);

    final int grid = getResources().getDimensionPixelSize(R.dimen.grid_margin_detail);
    setPadding(grid, grid, grid, grid);
  }

  public void bind(TwitterListItem item) {
    if (item == null) {
      return;
    }
    setTextColor(item.isRetweet() ?
        ContextCompat.getColor(getContext(), R.color.twitter_action_retweeted)
        : Color.GRAY);
    rtUser.setVisibility(item.isRetweet() ? VISIBLE : GONE);
    names.setNames(item.getCombinedName());
    tweet.setText(item.getText(), TextView.BufferType.SPANNABLE);
    reactionContainer.update(item.getStats());
    createdAt.setText(item.getCreatedTime(getContext()));
    thumbnailContainer.bindMediaEntities(item.getMediaCount());
    clientName.setText(formatString(R.string.tweet_via, item.getSource()));

    final TwitterListItem quotedItem = item.getQuotedItem();
    bindQuotedStatus(quotedItem);
  }

  public void setClickableItems(List<SpanItem> spans, OnSpanClickListener listener) {
    final Spannable text = (Spannable) tweet.getText();
    for (SpanItem span : spans) {
      text.setSpan(new ClickableSpan() {
        @Override
        public void onClick(View view) {
          listener.onClicked(view, span);
        }
      }, span.getStart(), span.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
  }

  private void bindQuotedStatus(TwitterListItem quotedItem) {
    if (quotedItem == null) {
      quotedStatus.setVisibility(GONE);
    } else {
      quotedStatus.setVisibility(VISIBLE);
      quotedStatus.bind(quotedItem);
    }
  }

  public void update(TwitterListItem item) {
    reactionContainer.update(item.getStats());
    names.setNames(item.getCombinedName());
    if (quotedStatus != null) {
      quotedStatus.update(item.getQuotedItem());
    }
  }

  @Override
  public void updateTime() {
    quotedStatus.updateTime();
  }

  private void setTextColor(int color) {
    names.setTextColor(color);
    createdAt.setTextColor(color);
    tweet.setTextColor(color);
  }

  public void reset() {
    setBackgroundColor(Color.TRANSPARENT);

    icon.setImageDrawable(null);
    icon.setImageResource(android.R.color.transparent);
    icon.setOnClickListener(null);
    setOnClickListener(null);

    rtUser.setText("");
    if (quotedStatus != null) {
      quotedStatus.reset();
    }
    thumbnailContainer.reset();
    thumbnailContainer.setOnMediaClickListener(null);
  }

  public ImageView getIcon() {
    return icon;
  }

  public RetweetUserView getRtUser() {
    return rtUser;
  }

  public QuotedStatusView getQuotedStatusView() {
    return quotedStatus;
  }

  @Override
  public ThumbnailContainer getThumbnailContainer() {
    return thumbnailContainer;
  }

  public TextView getUserName() {
    return names;
  }

  private String formatString(@StringRes int id, Object... items) {
    final String format = getResources().getString(id);
    return String.format(format, items);
  }
}

