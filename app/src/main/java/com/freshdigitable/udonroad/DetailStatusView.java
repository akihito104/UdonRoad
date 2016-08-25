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
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import twitter4j.Status;

/**
 * Created by akihit on 2016/08/18.
 */
public class DetailStatusView extends FullStatusView {
  public DetailStatusView(Context context) {
    this(context, null);
  }

  public DetailStatusView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public DetailStatusView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    final View v = View.inflate(context, R.layout.view_detail_status, this);
    createdAt = (TextView) v.findViewById(R.id.d_create_at);
    icon = (ImageView) v.findViewById(R.id.d_icon);
    names = (CombinedScreenNameTextView) v.findViewById(R.id.d_names);
    tweet = (TextView) v.findViewById(R.id.d_tweet);
    tweet.setMovementMethod(LinkMovementMethod.getInstance());
    clientName = (TextView) v.findViewById(R.id.d_via);
    rtIcon = (ImageView) v.findViewById(R.id.d_rt_icon);
    rtCount = (TextView) v.findViewById(R.id.d_rtcount);
    favIcon = (ImageView) v.findViewById(R.id.d_fav_icon);
    favCount = (TextView) v.findViewById(R.id.d_favcount);
    mediaContainer = (MediaContainer) v.findViewById(R.id.d_image_group);
    rtUserContainer = (LinearLayout) v.findViewById(R.id.d_rt_user_container);
    rtUser = (TextView) v.findViewById(R.id.d_rt_user);
    rtUserIcon = (ImageView) v.findViewById(R.id.d_rt_user_icon);
    quotedStatus = (QuotedStatusView) v.findViewById(R.id.d_quoted);
  }

  @Override
  protected int getGrid() {
    return getResources().getDimensionPixelSize(R.dimen.grid_margin_detail);
  }

  @Override
  protected CharSequence parseText(Status status) {
    final Status bindingStatus = getBindingStatus(status);
    String text = bindingStatus.getText();
    SpannableStringBuilder ssb = new SpannableStringBuilder(text);
    final List<SpanningInfo> spannableInfo = SpanningInfo.create(bindingStatus);
    setupClickableSpans(ssb, spannableInfo);
    return ssb;
  }

  private void setupClickableSpans(SpannableStringBuilder ssb, List<SpanningInfo> info) {
    for (SpanningInfo si : info) {
      if (si.isSpanning()) {
        ssb.setSpan(si.span, si.start, si.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
      if (si.isReplacing()) {
        ssb.replace(si.start, si.end, si.displayingText);
      }
    }
  }

  @Override
  public void setSelectedColor() {
  }

  @Override
  public void setUnselectedColor() {
  }
}
