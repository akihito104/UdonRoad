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

package com.freshdigitable.udonroad;

import android.text.TextUtils;

import java.util.Arrays;

/**
 * Created by akihit on 2017/03/21.
 */

public enum StoreType {
  HOME("home"),
  USER_HOME("user_home"),
  USER_FAV("user_favs"),
  USER_FRIEND("user_friends"),
  USER_FOLLOWER("user_followers"),
  CONVERSATION("conv"),
  CONFIG("config"),
  APP_SETTINGS("appSettings"),
  POOL("cache"),
  USER_MEDIA("user_media"),
  SEARCH("search"),
  OWNED_LIST("owned_lists"),
  USER_LIST("user_lists"),
  LIST_TL("listTl"),
  DEMO("demo");

  public final String storeName;

  StoreType(String name) {
    this.storeName = name;
  }

  public String prefix() {
    return storeName + "_";
  }

  public String nameWithSuffix(long id, String query) {
    final String suffix = id > 0 ? Long.toString(id) : query;
    return TextUtils.isEmpty(suffix) ?
        this.storeName
        : prefix() + suffix;
  }

  public boolean isForStatus() {
    for (StoreType type : Arrays.asList(HOME, USER_HOME, USER_FAV, CONVERSATION, USER_MEDIA, SEARCH, LIST_TL)) {
      if (this == type) {
        return true;
      }
    }
    return false;
  }

  public boolean isForUser() {
    for (StoreType type : Arrays.asList(USER_FOLLOWER, USER_FRIEND)) {
      if (this == type) {
        return true;
      }
    }
    return false;
  }

  public boolean isForLists() {
    for (StoreType type : Arrays.asList(USER_LIST, OWNED_LIST)) {
      if (this == type) {
        return true;
      }
    }
    return false;
  }
}
