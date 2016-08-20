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
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import twitter4j.Status;
import twitter4j.User;

/**
 * Created by akihit on 2016/08/20.
 */
public abstract class FullStatusView extends StatusViewBase {
  protected TextView rtUser;
  protected ImageView rtUserIcon;
  protected LinearLayout rtUserContainer;
  protected QuotedStatusView quotedStatus;

  public FullStatusView(Context context) {
    super(context);
  }

  public FullStatusView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public FullStatusView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  public void bindStatus(final Status status) {
    super.bindStatus(status);
    if (status.isRetweet()) {
      bindRtUser(status.getUser());
    }

    final Status quotedBindingStatus = getBindingStatus(status).getQuotedStatus();
    if (quotedBindingStatus != null) {
      quotedStatus.bindStatus(quotedBindingStatus);
      quotedStatus.setVisibility(VISIBLE);
    }
  }
  private void bindRtUser(User user) {
    setRetweetedUserVisibility(VISIBLE);
    final String formattedRtUser = formatString(R.string.tweet_retweeting_user,
        user.getScreenName());
    rtUser.setText(formattedRtUser);
  }

  private void setRetweetedUserVisibility(int visibility) {
    rtUserContainer.setVisibility(visibility);
  }

  @Override
  public void reset() {
    super.reset();
    setRetweetedUserVisibility(GONE);
    rtUserIcon.setImageDrawable(null);
    quotedStatus.setBackgroundResource(R.drawable.s_quoted_frame);
    quotedStatus.setVisibility(GONE);
    quotedStatus.reset();
  }

  public ImageView getRtUserIcon() {
    return rtUserIcon;
  }

  public QuotedStatusView getQuotedStatusView() {
    return quotedStatus;
  }
}
