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
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.freshdigitable.udonroad.CombinedScreenNameTextView;
import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.RetweetUserView;
import com.freshdigitable.udonroad.media.ThumbnailContainer;

import static com.freshdigitable.udonroad.listitem.StatusItemView.createVia;

/**
 * QuotedStatusView is for quoted tweet in StatusView and StatusDetailFragment.<br>
 *   QuotedStatusView does not have QuotedStatusView.
 *
 * Created by akihit on 2016/06/26.
 */
public class QuotedStatusView extends RelativeLayout implements StatusItemView {
  final ImageView icon;
  final CombinedScreenNameTextView names;
  final TextView tweet;
  final ReactionContainer reactionContainer;
  final int selectedColor;
  final TextView createdAt;
  final TextView clientName;
  final ThumbnailContainer thumbnailContainer;

  private TwitterListItem.TimeTextStrategy timeStrategy;

  public QuotedStatusView(Context context) {
    this(context, null);
  }

  public QuotedStatusView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public QuotedStatusView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    setBackgroundResource(R.drawable.s_rounded_frame_default);

    final View v = View.inflate(context, R.layout.view_quoted_status, this);
    createdAt = v.findViewById(R.id.q_create_at);
    icon = v.findViewById(R.id.q_icon);
    names = v.findViewById(R.id.q_names);
    tweet = v.findViewById(R.id.q_tweet);
    clientName = v.findViewById(R.id.q_via);
    thumbnailContainer = v.findViewById(R.id.q_image_group);
    reactionContainer = v.findViewById(R.id.q_reaction_container);

    final int grid = getResources().getDimensionPixelSize(R.dimen.grid_margin);
    setPadding(grid, grid, grid, grid);
    selectedColor = ContextCompat.getColor(context, R.color.twitter_action_normal_transparent);
  }

  public void setItem(TwitterListItem item) {
    bind(item);
  }

  public void bind(TwitterListItem item) {
    if (item == null) {
      return;
    }
    names.setNames(item.getCombinedName());
    tweet.setText(item.getText());
    reactionContainer.update(item.getStats());
    timeStrategy = item.getTimeStrategy();
    StatusItemView.updateTextView(createdAt, item.getCreatedTime(getContext()));
    thumbnailContainer.bindMediaEntities(item.getMediaCount());
    StatusItemView.updateTextView(clientName, createVia(item.getSource()));
  }

  @Override
  public void update(TwitterListItem item) {
    if (item == null) {
      setVisibility(GONE);
      return;
    }
    reactionContainer.update(item.getStats());
    names.setNames(item.getCombinedName());
  }

  @Override
  public void updateTime() {
    if (timeStrategy == null) {
      return;
    }
    StatusItemView.updateTextView(createdAt, timeStrategy.getCreatedTime(getContext()));
  }

  public void reset() {
    setBackgroundColor(Color.TRANSPARENT);

    icon.setOnClickListener(null);
    setOnClickListener(null);
    thumbnailContainer.reset();
    thumbnailContainer.setOnMediaClickListener(null);
    setBackgroundResource(R.drawable.s_rounded_frame_default);
    timeStrategy = null;
  }

  public void setSelectedColor() {
    setBackgroundResource(R.drawable.s_rounded_frame_pressed);
  }

  public void setUnselectedColor() {
    setBackgroundResource(R.drawable.s_rounded_frame_default);
  }

  @Override
  public ThumbnailContainer getThumbnailContainer() {
    return thumbnailContainer;
  }

  @Override
  public ImageView getIcon() {
    return icon;
  }

  @Override
  public RetweetUserView getRtUser() {
    return null;
  }

  @Override
  public QuotedStatusView getQuotedStatusView() {
    return null;
  }
}
