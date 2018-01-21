/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad.timeline.fetcher;

import twitter4j.Paging;

/**
 * Created by akihit on 2018/01/11.
 */
public class FetchQuery {
  final long id;
  final long lastPageCursor;
  final String searchQuery;

  public FetchQuery() {
    this.id = -1;
    this.lastPageCursor = -1;
    this.searchQuery = "";
  }

  Paging getPaging() {
    return new Paging(1, 20, 1, lastPageCursor);
  }

  private FetchQuery(Builder builder) {
    this.id = builder.id;
    this.lastPageCursor = builder.lastPageCursor;
    this.searchQuery = builder.searchQuery;
  }

  public static class Builder {
    long id;
    long lastPageCursor;
    private String searchQuery;

    public Builder id(long id) {
      this.id = id;
      return this;
    }

    public Builder lastPageCursor(long lastPageCursor) {
      this.lastPageCursor = lastPageCursor;
      return this;
    }

    public Builder searchQuery(String searchQuery) {
      this.searchQuery = searchQuery;
      return this;
    }

    public FetchQuery build() {
      return new FetchQuery(this);
    }
  }
}
