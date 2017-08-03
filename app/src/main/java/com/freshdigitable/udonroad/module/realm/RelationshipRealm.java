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

import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import twitter4j.RateLimitStatus;
import twitter4j.Relationship;

/**
 * Created by akihit on 2017/08/02.
 */
@RealmClass
public class RelationshipRealm implements Relationship, RealmModel {
  @PrimaryKey
  private long id;
  private boolean following;
  private boolean blocking;
  private boolean muting;
  private boolean wantRetweets;
  private boolean notificationsEnabled;

  public RelationshipRealm() {
  }

  RelationshipRealm(Relationship relationship) {
    this.id = relationship.getTargetUserId();
    this.following = relationship.isSourceFollowingTarget();
    this.blocking = relationship.isSourceBlockingTarget();
    this.muting = relationship.isSourceMutingTarget();
    this.wantRetweets = relationship.isSourceWantRetweets();
    this.notificationsEnabled = relationship.isSourceNotificationsEnabled();
  }

  public void merge(Relationship other) {
    if (id != other.getTargetUserId()) {
      throw new IllegalArgumentException();
    }
    if (following != other.isSourceFollowingTarget()) {
      following = other.isSourceFollowingTarget();
    }
    if (blocking != other.isSourceBlockingTarget()) {
      blocking = other.isSourceBlockingTarget();
    }
    if (muting != other.isSourceMutingTarget()) {
      muting = other.isSourceMutingTarget();
    }
    if (wantRetweets != other.isSourceWantRetweets()) {
      wantRetweets = other.isSourceWantRetweets();
    }
    if (notificationsEnabled != other.isSourceNotificationsEnabled()) {
      notificationsEnabled = other.isSourceNotificationsEnabled();
    }
  }

  @Override
  public long getTargetUserId() {
    return id;
  }

  @Override
  public boolean isSourceBlockingTarget() {
    return blocking;
  }

  void setSourceBlockingTarget(boolean blocking) {
    this.blocking = blocking;
  }

  @Override
  public boolean isSourceMutingTarget() {
    return muting;
  }

  void setSourceMutingTarget(boolean muting) {
    this.muting = muting;
  }

  @Override
  public boolean isSourceFollowingTarget() {
    return following;
  }

  void setSourceFollowingTarget(boolean following) {
    this.following = following;
  }

  @Override
  public boolean isSourceWantRetweets() {
    return wantRetweets;
  }

  @Override
  public boolean isTargetFollowedBySource() {
    return following;
  }

  @Override
  public boolean isSourceNotificationsEnabled() {
    return notificationsEnabled;
  }

  @Override
  public long getSourceUserId() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public String getSourceUserScreenName() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public String getTargetUserScreenName() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public boolean isTargetFollowingSource() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public boolean isSourceFollowedByTarget() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public boolean canSourceDm() {
    throw new RuntimeException("not implemented yet...");
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
