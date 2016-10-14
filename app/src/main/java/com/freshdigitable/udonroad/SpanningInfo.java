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

/**
 * SpanningInfo is information to create SpannableStringBuilder.
 *
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
}
