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
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.media.ThumbnailContainer;

/**
 * QuotedStatusView is for quoted tweet in StatusView and StatusDetailFragment.<br>
 *   QuotedStatusView does not have QuotedStatusView.
 *
 * Created by akihit on 2016/06/26.
 */
public class QuotedStatusView extends ItemView implements ThumbnailCapable {
  TextView createdAt;
  TextView clientName;
  ThumbnailContainer thumbnailContainer;

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
  }

  public void bind(TwitterListItem item) {
    if (item == null) {
      return;
    }
    super.bind(item);
    timeStrategy = item.getTimeStrategy();
    createdAt.setText(item.getCreatedTime(getContext()));
    thumbnailContainer.bindMediaEntities(item.getMediaCount());
    clientName.setText(formatString(R.string.tweet_via, item.getSource()));
  }

  public void updateTime() {
    if (timeStrategy == null) {
      return;
    }
    createdAt.setText(timeStrategy.getCreatedTime(getContext()));
  }

  public void update(ListItem item) {
    if (item == null) {
      return;
    }
    reactionContainer.update(item.getStats());
    names.setNames(item.getUser());
  }

  @Override
  public void reset() {
    super.reset();
    thumbnailContainer.reset();
    thumbnailContainer.setOnMediaClickListener(null);
    setBackgroundResource(R.drawable.s_rounded_frame_default);
  }

  @Override
  public void setSelectedColor() {
    setBackgroundResource(R.drawable.s_rounded_frame_pressed);
  }

  @Override
  public void setUnselectedColor() {
    setBackgroundResource(R.drawable.s_rounded_frame_default);
  }

  @Override
  public ThumbnailContainer getThumbnailContainer() {
    return thumbnailContainer;
  }
}
