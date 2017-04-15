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
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.freshdigitable.udonroad.media.ThumbnailContainer;

import java.util.Date;

import twitter4j.Status;

import static com.freshdigitable.udonroad.Utils.getBindingStatus;

/**
 * StatusDetailView is a View to show in StatusDetailFragment.
 *
 * Created by akihit on 2016/08/18.
 */
public class StatusDetailView extends FullStatusView {
  public StatusDetailView(Context context) {
    this(context, null);
  }

  public StatusDetailView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public StatusDetailView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    final View v = View.inflate(context, R.layout.view_status_detail, this);
    createdAt = (TextView) v.findViewById(R.id.d_create_at);
    icon = (ImageView) v.findViewById(R.id.d_icon);
    names = (CombinedScreenNameTextView) v.findViewById(R.id.d_names);
    tweet = (LinkableTextView) v.findViewById(R.id.d_tweet);
    clientName = (TextView) v.findViewById(R.id.d_via);
    rtCount = (IconAttachedTextView) v.findViewById(R.id.d_rtcount);
    favCount = (IconAttachedTextView) v.findViewById(R.id.d_favcount);
    thumbnailContainer = (ThumbnailContainer) v.findViewById(R.id.d_image_group);
    rtUser = (RetweetUserView) v.findViewById(R.id.d_rt_user);
    quotedStatus = (QuotedStatusView) v.findViewById(R.id.d_quoted);
  }

  @Override
  protected void bindCreatedAt(Date tweetedDate) {
    createdAt.setText(DateUtils.formatDateTime(getContext(), tweetedDate.getTime(),
        DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));
  }

  @Override
  protected int getGrid() {
    return getResources().getDimensionPixelSize(R.dimen.grid_margin_detail);
  }

  @Override
  protected CharSequence parseText(Status status) {
    final Status bindingStatus = getBindingStatus(status);
    return SpannableStringUtil.create(bindingStatus);
  }

  @Override
  public void setSelectedColor() {
  }

  @Override
  public void setUnselectedColor() {
  }
}
