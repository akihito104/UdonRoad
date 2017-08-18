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

import com.freshdigitable.udonroad.CombinedScreenNameTextView.CombinedName;

import java.util.Collections;
import java.util.List;

import twitter4j.User;
import twitter4j.UserList;

/**
 * Created by akihit on 2017/08/17.
 */

public class ListsListItem implements ListItem {
  private final UserList userList;

  public ListsListItem(UserList item) {
    this.userList = item;
  }

  @Override
  public long getId() {
    return userList.getId();
  }

  @Override
  public CharSequence getText() {
    return userList.getDescription();
  }

  @Override
  public User getUser() {
    return userList.getUser();
  }

  @Override
  public List<Stat> getStats() {
    return Collections.emptyList();
  }

  @Override
  public CombinedName getCombinedName() {
    return new TwitterCombinedName(userList);
  }
}
