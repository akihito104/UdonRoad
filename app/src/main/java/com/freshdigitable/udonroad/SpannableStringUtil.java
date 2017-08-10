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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import twitter4j.URLEntity;

/**
 * SpannableStringUtil creates clickable text.
 *
 * Created by akihit on 2016/10/08.
 */
public class SpannableStringUtil {
  public static CharSequence create(final String text, URLEntity[] urlEntities) {
    if (TextUtils.isEmpty(text)) {
      return "";
    }
    final List<SpanningInfo> urlSpanningInfo = createURLSpanningInfo(text, urlEntities, null);
    return createClickableSpan(text, urlSpanningInfo);
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

  private static boolean isInvalidRange(String text, int start, int end) {
    return start < 0 || end > text.length() || start > text.length();
  }

  private static CharSequence createClickableSpan(String text, List<SpanningInfo> info) {
    // to manipulate changing tweet text length, sort descending
    SpannableStringBuilder ssb = new SpannableStringBuilder(text);
    Collections.sort(info, (l, r) -> r.start - l.start);
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

  /**
   * SpanningInfo is information to create SpannableStringBuilder.
   *
   * Created by akihit on 2016/08/20.
   */
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
}
