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

import com.freshdigitable.udonroad.datastore.StatusReaction;

import io.realm.RealmModel;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import twitter4j.Status;

/**
 * Created by akihit on 2016/11/05.
 */
@RealmClass
public class StatusReactionRealm implements StatusReaction, RealmModel {
  @PrimaryKey
  private long id;
  private boolean retweeted;
  private boolean favorited;

  public StatusReactionRealm() {
  }

  public StatusReactionRealm(Status status) {
    this.id = status.getId();
    this.retweeted = status.isRetweeted();
    this.favorited = status.isFavorited();
  }

  public StatusReactionRealm(StatusReaction reaction) {
    this.id = reaction.getId();
    this.retweeted = reaction.isRetweeted();
    this.favorited = reaction.isFavorited();
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public void setRetweeted(boolean retweeted) {
    this.retweeted = retweeted;
  }

  @Override
  public boolean isRetweeted() {
    return retweeted;
  }

  @Override
  public void setFavorited(boolean favorited) {
    this.favorited = favorited;
  }

  @Override
  public boolean isFavorited() {
    return favorited;
  }

  public void merge(StatusReaction others) {
    // `Status.favorited` is nullable and in the case `favoried` is false.
    if (!this.favorited && others.isFavorited()) {
      this.favorited = others.isFavorited();
    }
    // `Status.retweeted` is nullable and in the case `retweeted` is false.
    if (!this.retweeted && others.isRetweeted()) {
      this.retweeted = others.isRetweeted();
    }
  }
}
