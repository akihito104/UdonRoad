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

package com.freshdigitable.udonroad.realmdata;

import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import twitter4j.UserMentionEntity;

/**
 * Created by akihit on 2016/08/21.
 */
@RealmClass
public class UserMentionEntityRealm implements RealmModel, UserMentionEntity {
  @PrimaryKey
  private long id;
  private String screenName;
  private String name;

  public UserMentionEntityRealm() {
  }

  UserMentionEntityRealm(UserMentionEntity u) {
    this.id = u.getId();
    this.screenName = u.getScreenName();
    this.name = u.getName();
  }

  @Override
  public String getText() {
    return getScreenName();
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
  public long getId() {
    return id;
  }

  @Override
  public int getStart() {
    throw new RuntimeException("not implemented yet.");
  }

  @Override
  public int getEnd() {
    throw new RuntimeException("not implemented yet.");
  }
}
