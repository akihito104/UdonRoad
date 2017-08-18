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

import com.freshdigitable.udonroad.CombinedScreenNameTextView;

import twitter4j.User;
import twitter4j.UserList;

/**
 * Created by akihit on 2017/08/18.
 */

public class TwitterCombinedName implements CombinedScreenNameTextView.CombinedName {
  private String name;
  private String screenName;
  private boolean isPrivate;
  private boolean isVerified;

  public TwitterCombinedName(User user) {
    this.name = user.getName();
    this.screenName = user.getScreenName();
    this.isPrivate = user.isProtected();
    this.isVerified = user.isVerified();
  }

  TwitterCombinedName(UserList userList) {
    this.name = userList.getName();
    this.screenName = "";
    this.isPrivate = !userList.isPublic();
    this.isVerified = false;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getScreenName() {
    return screenName;
  }

  @Override
  public boolean isPrivate() {
    return isPrivate;
  }

  @Override
  public boolean isVerified() {
    return isVerified;
  }

  @Override
  public int hashCode() {
    int res = 17;
    res = 31 * res + getName().hashCode();
    res = 31 * res + getScreenName().hashCode();
    res = 31 * res + (isPrivate() ? 1 : 0);
    res = 31 * res + (isVerified() ? 1 : 0);
    return res;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof TwitterCombinedName)) {
      return false;
    }
    final TwitterCombinedName other = (TwitterCombinedName) obj;
    return name.equals(other.getName())
        && screenName.equals(other.getScreenName())
        && isVerified == other.isVerified()
        && isPrivate == other.isPrivate();
  }
}
