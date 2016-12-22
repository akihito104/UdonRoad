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

import twitter4j.Status;

import static com.freshdigitable.udonroad.Utils.getBindingStatus;

/**
 * FullStatusView defines StatusView which has full of elements.
 *
 * Created by akihit on 2016/08/20.
 */
public abstract class FullStatusView extends StatusViewBase {
  protected QuotedStatusView quotedStatus;
  protected RetweetUserView rtUser;

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
      setRetweetedUserVisibility(VISIBLE);
    }

    final Status quotedBindingStatus = getBindingStatus(status).getQuotedStatus();
    if (quotedBindingStatus != null) {
      quotedStatus.bindStatus(quotedBindingStatus);
      quotedStatus.setVisibility(VISIBLE);
    }
  }

  @Override
  public void update(Status status) {
    super.update(status);
    final Status quotedBindingStatus = getBindingStatus(status).getQuotedStatus();
    if (quotedBindingStatus != null) {
      quotedStatus.update(quotedBindingStatus);
      quotedStatus.setVisibility(VISIBLE);
    }
  }

  private void setRetweetedUserVisibility(int visibility) {
    rtUser.setVisibility(visibility);
  }

  @Override
  public void reset() {
    super.reset();
    setRetweetedUserVisibility(GONE);
    rtUser.setText("");
    quotedStatus.setBackgroundResource(R.drawable.s_rounded_frame_default);
    quotedStatus.setVisibility(GONE);
    quotedStatus.reset();
  }

  public RetweetUserView getRtUser() {
    return rtUser;
  }

  public QuotedStatusView getQuotedStatusView() {
    return quotedStatus;
  }
}
