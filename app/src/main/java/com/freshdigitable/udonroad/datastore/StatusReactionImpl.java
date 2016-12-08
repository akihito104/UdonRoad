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

package com.freshdigitable.udonroad.datastore;

import android.support.annotation.Nullable;

import twitter4j.Status;

/**
 * Created by akihit on 2016/11/07.
 */

public class StatusReactionImpl implements StatusReaction {
  private long id;
  private Boolean favorited;
  private Boolean retweeted;

  public StatusReactionImpl(Status status) {
    this.id = status.getId();
    this.favorited = status.isFavorited();
    this.retweeted = status.isRetweeted();
  }

  @Override
  public long getId() {
    return id;
  }

  @Override
  public void setRetweeted(@Nullable Boolean retweeted) {
    this.retweeted = retweeted;
  }

  @Override
  public Boolean isRetweeted() {
    return retweeted;
  }

  @Override
  public void setFavorited(@Nullable Boolean favorited) {
    this.favorited = favorited;
  }

  @Override
  public Boolean isFavorited() {
    return favorited;
  }
}
