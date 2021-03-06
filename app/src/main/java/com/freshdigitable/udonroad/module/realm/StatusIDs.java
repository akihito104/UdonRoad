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

package com.freshdigitable.udonroad.module.realm;

import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import twitter4j.Status;

/**
 * StatusIDs is IDs in Status class.
 *
 * Created by akihit on 2016/07/22.
 */
@RealmClass
public class StatusIDs implements RealmModel {
  @PrimaryKey
  private long id;
  private long retweetedStatusId;
  private long quotedStatusId;
  private long userId;
  private long retweetedUserId;

  public StatusIDs() {}

  StatusIDs(Status status) {
    this.id = status.getId();
    this.userId = status.getUser().getId();
    final Status retweetedStatus = status.getRetweetedStatus();
    if (retweetedStatus != null) {
      this.retweetedStatusId = retweetedStatus.getId();
      this.retweetedUserId = retweetedStatus.getUser().getId();
    } else {
      this.retweetedStatusId = -1;
      this.retweetedUserId = -1;
    }
    this.quotedStatusId = status.getQuotedStatusId();
  }

  public long getId() {
    return id;
  }

  public long getRetweetStatusId() {
    return retweetedStatusId;
  }

  public long getQuotedStatusId() {
    return quotedStatusId;
  }
}
