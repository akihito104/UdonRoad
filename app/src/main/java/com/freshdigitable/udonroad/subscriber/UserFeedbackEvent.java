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

package com.freshdigitable.udonroad.subscriber;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

/**
 * Created by akihit on 2016/12/07.
 */

public class UserFeedbackEvent {
  @StringRes
  private final int msgResId;
  private final Object[] args;

  public UserFeedbackEvent(@StringRes int msgResId) {
    this(msgResId, (String[]) null);
  }

  public UserFeedbackEvent(@StringRes int msgResId, @Nullable String... args) {
    this.msgResId = msgResId;
    this.args = args;
  }

  CharSequence createMessage(Context context) {
    return context.getString(msgResId, args);
  }
}
