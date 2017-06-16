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

import com.freshdigitable.udonroad.R;

/**
 * QuotedStatusView is for quoted tweet in StatusView and StatusDetailFragment.<br>
 *   QuotedStatusView does not have QuotedStatusView.
 *
 * Created by akihit on 2016/06/26.
 */
public class QuotedStatusView extends ItemView {

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
    rtCount = v.findViewById(R.id.q_rtcount);
    favCount = v.findViewById(R.id.q_favcount);
    hasReplyIcon = v.findViewById(R.id.q_has_reply);
    thumbnailContainer = v.findViewById(R.id.q_image_group);
  }

  public void bind(TwitterListItem item) {
    if (item == null) {
      return;
    }
    super.bind(item);
    timeStrategy = item.getTimeStrategy();
  }

  public void updateTime() {
    if (timeStrategy == null) {
      return;
    }
    createdAt.setText(timeStrategy.getCreatedTime(getContext()));
  }

  @Override
  public void reset() {
    super.reset();
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
}
