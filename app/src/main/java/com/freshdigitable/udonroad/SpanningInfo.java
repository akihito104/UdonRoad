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
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import twitter4j.ExtendedMediaEntity;
import twitter4j.Status;
import twitter4j.URLEntity;

/**
 * Created by akihit on 2016/08/20.
 */
class SpanningInfo {
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

  static List<SpanningInfo> create(Status bindingStatus) {
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

  private static List<SpanningInfo> createURLSpanningInfo(Status bindingStatus) {
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
}
