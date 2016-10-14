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

import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import twitter4j.ExtendedMediaEntity;
import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.UserMentionEntity;

/**
 * SpannableStringUtil creates clickable text.
 *
 * Created by akihit on 2016/10/08.
 */
class SpannableStringUtil {
  static CharSequence create(Status bindingStatus) {
    final List<SpanningInfo> spannableInfo = createSpanningInfo(bindingStatus);
    return createClickableSpan(bindingStatus.getText(), spannableInfo);
  }

  static CharSequence create(final String text, URLEntity[] urlEntities) {
    final List<SpanningInfo> urlSpanningInfo = createURLSpanningInfo(text, urlEntities, null);
    return createClickableSpan(text, urlSpanningInfo);
  }

  private static List<SpanningInfo> createSpanningInfo(Status bindingStatus) {
    List<SpanningInfo> info = new ArrayList<>();
    info.addAll(createURLSpanningInfo(bindingStatus));
    info.addAll(createUserSpanningInfo(bindingStatus));
    for (int i = info.size() - 1; i >= 0; i--) {
      final SpanningInfo spanningInfo = info.get(i);
      for (int j = 0; j < i; j++) {
        if (spanningInfo.start == info.get(j).start) {
          info.remove(spanningInfo);
          break;
        }
      }
    }
    return info;
  }

  private static List<SpanningInfo> createURLSpanningInfo(Status bindingStatus) {
    final String text = bindingStatus.getText();
    final String quotedStatusIdStr = bindingStatus.getQuotedStatus() != null
        ? Long.toString(bindingStatus.getQuotedStatusId())
        : "";
    final List<SpanningInfo> info = new ArrayList<>();
    final URLEntity[] urlEntities = bindingStatus.getURLEntities();
    info.addAll(createURLSpanningInfo(text, urlEntities, quotedStatusIdStr));
    final ExtendedMediaEntity[] eme = bindingStatus.getExtendedMediaEntities();
    for (ExtendedMediaEntity e : eme) {
      int start = text.indexOf(e.getURL());
      int end = start + e.getURL().length();
      if (isInvalidRange(text, start, end)) {
        continue;
      }
      info.add(new SpanningInfo(null, start, end, ""));
    }
    return info;
  }

  private static List<SpanningInfo> createURLSpanningInfo(final String text,
                                                          URLEntity[] urlEntities,
                                                          @Nullable String quotedStatusIdStr) {
    List<SpanningInfo> info = new ArrayList<>(urlEntities.length);
    for (URLEntity u : urlEntities) {
      int start = text.indexOf(u.getURL());
      int end = start + u.getURL().length();
      if (isInvalidRange(text, start, end)) {
        if (TextUtils.isEmpty(u.getExpandedURL())) {
          continue;
        }
        start = text.indexOf(u.getExpandedURL());
        end = start + u.getExpandedURL().length();
        if (isInvalidRange(text, start, end)) {
          continue;
        }
      }
      if (!TextUtils.isEmpty(quotedStatusIdStr)
          && u.getExpandedURL().contains(quotedStatusIdStr)) {
        info.add(new SpanningInfo(null, start, end, ""));
      }
      info.add(new SpanningInfo(new URLSpan(u.getExpandedURL()), start, end, u.getDisplayURL()));
    }
    return info;
  }

  private static List<SpanningInfo> createUserSpanningInfo(Status bindingStatus) {
    final String text = bindingStatus.getText();
    final UserMentionEntity[] userMentionEntities = bindingStatus.getUserMentionEntities();
    final List<SpanningInfo> info = new ArrayList<>();
    for (UserMentionEntity u : userMentionEntities) {
      final int start = text.indexOf("@" + u.getScreenName());
      final int end = start + u.getScreenName().length() + 1;
      if (isInvalidRange(text, start, end)) {
        continue;
      }
      final long id = u.getId();
      info.add(new SpanningInfo(new ClickableSpan() {
        @Override
        public void onClick(View view) {
          UserInfoActivity.start(view.getContext(), id);
        }
      }, start, end, null));
    }
    return info;
  }

  private static boolean isInvalidRange(String text, int start, int end) {
    return start < 0 || end > text.length() || start > text.length();
  }

  private static CharSequence createClickableSpan(String text, List<SpanningInfo> info) {
    // to manipulate changing tweet text length, sort descending
    SpannableStringBuilder ssb = new SpannableStringBuilder(text);
    Collections.sort(info, new Comparator<SpanningInfo>() {
      @Override
      public int compare(SpanningInfo l, SpanningInfo r) {
        return r.start - l.start;
      }
    });
    for (SpanningInfo si : info) {
      if (si.isSpanning()) {
        ssb.setSpan(si.span, si.start, si.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
      if (si.isReplacing()) {
        ssb.replace(si.start, si.end, si.displayingText);
      }
    }
    return ssb;
  }
}
