/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad.detail;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.View;

import com.freshdigitable.udonroad.CombinedScreenNameTextView;
import com.freshdigitable.udonroad.OnSpanClickListener;
import com.freshdigitable.udonroad.listitem.ListItem;
import com.freshdigitable.udonroad.listitem.StatusListItem;
import com.freshdigitable.udonroad.listitem.TwitterListItem;

import java.util.List;

import twitter4j.Status;
import twitter4j.User;

/**
 * Created by akihit on 2018/01/27.
 */

public class DetailItem {

  public final CombinedScreenNameTextView.CombinedName combinedName;
  public final String createdTime;
  public final String source;
  public final int mediaCount;
  public final TwitterListItem quotedItem;
  public final Spannable tweet;
  public final boolean retweet;
  public final List<ListItem.Stat> stats;
  public final long retweetUserId;
  public final long id;
  public final User user;

  DetailItem(Status status, Context context, OnSpanClickListener listener) {
    final StatusListItem statusListItem = new StatusListItem(status, StatusListItem.TextType.DETAIL, StatusListItem.TimeTextType.ABSOLUTE);
    id = statusListItem.getId();
    user = statusListItem.getUser();
    retweetUserId = statusListItem.getRetweetUser().getId();
    retweet = statusListItem.isRetweet();
    combinedName = statusListItem.getCombinedName();
    createdTime = statusListItem.getCreatedTime(context);
    source = statusListItem.getSource();
    mediaCount = statusListItem.getMediaCount();
    stats = statusListItem.getStats();
    quotedItem = statusListItem.getQuotedItem();

    final CharSequence text = statusListItem.getText();
    final List<OnSpanClickListener.SpanItem> spanItems = statusListItem.createSpanItems();
    final SpannableStringBuilder ssb = new SpannableStringBuilder(text);
    for (OnSpanClickListener.SpanItem spanItem : spanItems) {
      ssb.setSpan(new ClickableSpan() {
        @Override
        public void onClick(View widget) {
          listener.onSpanClicked(widget, spanItem);
        }
      }, spanItem.getStart(), spanItem.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    this.tweet = ssb;
  }
}
