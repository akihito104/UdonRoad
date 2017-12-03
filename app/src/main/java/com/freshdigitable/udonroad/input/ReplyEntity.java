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

package com.freshdigitable.udonroad.input;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import twitter4j.Status;
import twitter4j.User;
import twitter4j.UserMentionEntity;

/**
 * Created by akihit on 2017/12/02.
 */
class ReplyEntity implements Parcelable {
  long getInReplyToStatusId() {
    return inReplyToStatusId;
  }

  private final long inReplyToStatusId;
  private final Set<String> screenNames;

  static ReplyEntity create(@NonNull Status status, long fromUserId) {
    final Set<String> screenNames = new LinkedHashSet<>();
    final UserMentionEntity[] userMentionEntities = status.getUserMentionEntities();
    for (UserMentionEntity u : userMentionEntities) {
      if (u.getId() != fromUserId) {
        screenNames.add(u.getScreenName());
      }
    }

    if (status.isRetweet()) {
      final Status retweetedStatus = status.getRetweetedStatus();
      final User user = retweetedStatus.getUser();
      if (user.getId() != fromUserId) {
        screenNames.add(user.getScreenName());
      }
    }

    final User user = status.getUser();
    if (user.getId() != fromUserId) {
      screenNames.add(user.getScreenName());
    }
    return new ReplyEntity(status.getId(), screenNames);
  }

  private ReplyEntity(long replyToStatusId, Set<String> replyToUsers) {
    this.inReplyToStatusId = replyToStatusId;
    this.screenNames = replyToUsers;
  }

  String createReplyString() {
    StringBuilder s = new StringBuilder();
    for (String sn : screenNames) {
      s.append("@").append(sn).append(" ");
    }
    return s.toString();
  }

  private ReplyEntity(Parcel in) {
    inReplyToStatusId = in.readLong();
    final ArrayList<String> strings = new ArrayList<>();
    in.readStringList(strings);
    screenNames = new LinkedHashSet<>();
    screenNames.addAll(strings);
  }

  public static final Creator<ReplyEntity> CREATOR = new Creator<ReplyEntity>() {
    @Override
    public ReplyEntity createFromParcel(Parcel in) {
      return new ReplyEntity(in);
    }

    @Override
    public ReplyEntity[] newArray(int size) {
      return new ReplyEntity[size];
    }
  };

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(inReplyToStatusId);
    final ArrayList<String> strings = new ArrayList<>();
    strings.addAll(screenNames);
    dest.writeStringList(strings);
  }
}
