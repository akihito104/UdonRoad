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

import com.freshdigitable.udonroad.LinkableTextView;
import com.freshdigitable.udonroad.R;

/**
 * StatusDetailView is a View to show in StatusDetailFragment.
 *
 * Created by akihit on 2016/08/18.
 */
public class StatusDetailView extends StatusView {
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
    rtCount = v.findViewById(R.id.d_rtcount);
    favCount = v.findViewById(R.id.d_favcount);
    hasReplyIcon = v.findViewById(R.id.d_has_reply);
    thumbnailContainer = v.findViewById(R.id.d_image_group);
    rtUser = v.findViewById(R.id.d_rt_user);
    quotedStatus = v.findViewById(R.id.d_quoted);
  }

  @Override
  protected int getGrid() {
    return getResources().getDimensionPixelSize(R.dimen.grid_margin_detail);
  }

  @Override
  public void setSelectedColor() {
  }

  @Override
  public void setUnselectedColor() {
  }
}
