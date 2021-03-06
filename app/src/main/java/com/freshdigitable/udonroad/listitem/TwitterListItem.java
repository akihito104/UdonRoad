/*
 * Copyright (c) 2017. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad.listitem;

import android.content.Context;
import android.support.annotation.Nullable;

import twitter4j.MediaEntity;
import twitter4j.User;

/**
 * Created by akihit on 2017/06/16.
 */

public interface TwitterListItem extends ListItem {
  boolean isRetweet();

  String getCreatedTime(Context context);

  String getSource();

  int getMediaCount();

  User getRetweetUser();

  @Nullable
  TwitterListItem getQuotedItem();

  @Nullable
  TimeTextStrategy getTimeStrategy();

  MediaEntity[] getMediaEntities();

  boolean isPossiblySensitive();

  interface TimeTextStrategy {
    String getCreatedTime(Context context);
  }
}
