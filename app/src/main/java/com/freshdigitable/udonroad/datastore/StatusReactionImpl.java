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

import twitter4j.Status;

/**
 * Created by akihit on 2016/11/07.
 */

public class StatusReactionImpl implements StatusReaction {
  private long id;
  private boolean favorited;
  private boolean retweeted;

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
}
