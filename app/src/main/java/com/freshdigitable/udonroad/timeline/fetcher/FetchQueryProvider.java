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

/**
 * Created by akihit on 2018/01/13.
 */
public class FetchQueryProvider {
  public FetchQuery getInitQuery(long id, String q) {
    return new FetchQuery.Builder()
        .id(id)
        .searchQuery(q)
        .build();
  }

  public FetchQuery getNextQuery(long id, String q, long nextCursor) {
    return new FetchQuery.Builder()
        .id(id)
        .searchQuery(q)
        .lastPageCursor(nextCursor)
        .build();
  }
}
