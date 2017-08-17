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

package com.freshdigitable.udonroad.module.realm;

import android.support.annotation.NonNull;

import java.net.URI;
import java.util.Date;

import io.realm.RealmModel;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import twitter4j.RateLimitStatus;
import twitter4j.User;
import twitter4j.UserList;

/**
 * Created by akihit on 2017/08/17.
 */
@RealmClass
public class UserListRealm implements UserList, RealmModel {
  @PrimaryKey
  private long id;
  private String name;
  private String fullName;
  private String description;
  private long userId;
  private boolean isPublic;
  private int order;
  @Ignore
  private User user;

  public UserListRealm() {}

  UserListRealm(UserList userList, int order) {
    this.id = userList.getId();
    this.name = userList.getName();
    this.fullName = userList.getFullName();
    this.description = userList.getDescription();
    this.userId = userList.getUser().getId();
    this.isPublic = userList.isPublic();
    this.order = order;
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getFullName() {
    return fullName;
  }

  @Override
  public String getSlug() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public int getSubscriberCount() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public int getMemberCount() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public URI getURI() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public boolean isPublic() {
    return isPublic;
  }

  @Override
  public User getUser() {
    return user;
  }

  long getUserId() {
    return userId;
  }

  int getOrder() {
    return order;
  }

  void setUser(User user) {
    this.user = user;
  }

  @Override
  public boolean isFollowing() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public Date getCreatedAt() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public int compareTo(@NonNull UserList userList) {
    return (int) (userList.getId() - id);
  }

  @Override
  public RateLimitStatus getRateLimitStatus() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public int getAccessLevel() {
    throw new RuntimeException("not implemented yet...");
  }
}
