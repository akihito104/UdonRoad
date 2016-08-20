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
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import twitter4j.ExtendedMediaEntity;
import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.User;

/**
 * Created by akihit on 2016/08/18.
 */
public class DetailStatusView extends StatusViewBase {
  protected TextView rtUser;
  protected ImageView rtUserIcon;
  protected LinearLayout rtUserContainer;
  private QuotedStatusView quotedStatus;

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

  @Override
  protected CharSequence parseText(Status status) {
    final Status bindingStatus = getBindingStatus(status);
    String text = bindingStatus.getText();
    SpannableStringBuilder ssb = new SpannableStringBuilder(text);
    final List<SpanningInfo> spannableInfo = createSpanningInfo(bindingStatus);
    setupClickableSpans(ssb, spannableInfo);
    return ssb;
  }

  private List<SpanningInfo> createSpanningInfo(Status bindingStatus) {
    List<SpanningInfo> info = new ArrayList<>();
    info.addAll(createURLSpanningInfo(bindingStatus));
    for (int i = info.size() - 1; i >= 0; i--) {
      final SpanningInfo spanningInfo = info.get(i);
      for (int j = 0; j < i; j++) {
        if (spanningInfo.start == info.get(j).start) {
          info.remove(spanningInfo);
          break;
        }
      }
    }
    // to manipulate changing tweet text length, sort descending
    Collections.sort(info, new Comparator<SpanningInfo>() {
      @Override
      public int compare(SpanningInfo l, SpanningInfo r) {
        return r.start - l.start;
      }
    });
    return info;
  }

  private List<SpanningInfo> createURLSpanningInfo(Status bindingStatus) {
    final String text = bindingStatus.getText();
    final URLEntity[] urlEntities = bindingStatus.getURLEntities();
    final String quotedStatusIdStr = Long.toString(bindingStatus.getQuotedStatusId());
    final List<SpanningInfo> info = new ArrayList<>();
    for (URLEntity u : urlEntities) {
      int start = text.indexOf(u.getURL());
      int end = start + u.getURL().length();
      if (start < 0 || end > text.length() || start > text.length()) {
        continue;
      }
      if (bindingStatus.getQuotedStatus() != null
          && u.getExpandedURL().contains(quotedStatusIdStr)) {
        info.add(new SpanningInfo(null, start, end, ""));
      }
      info.add(new SpanningInfo(new URLSpan(u.getExpandedURL()), start, end, u.getDisplayURL()));
    }
    final ExtendedMediaEntity[] eme = bindingStatus.getExtendedMediaEntities();
    for (ExtendedMediaEntity e : eme) {
      int start = text.indexOf(e.getURL());
      int end = start + e.getURL().length();
      if (start < 0 || end > text.length() || start > text.length()) {
        continue;
      }
      info.add(new SpanningInfo(null, start, end, ""));
    }
    return info;
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

  private static class SpanningInfo {
    final ClickableSpan span;
    final int start;
    final int end;
    final String displayingText;

    SpanningInfo(@Nullable ClickableSpan span, int start, int end, @Nullable String displayingText) {
      this.span = span;
      this.start = start;
      this.end = end;
      this.displayingText = displayingText;
    }

    boolean isSpanning() {
      return span != null;
    }

    boolean isReplacing() {
      return displayingText != null;
    }
  }

  public ImageView getRtUserIcon() {
    return rtUserIcon;
  }

  public QuotedStatusView getQuotedStatusView() {
    return quotedStatus;
  }

  @Override
  public void setSelectedColor() {
  }

  @Override
  public void setUnselectedColor() {
  }
}
