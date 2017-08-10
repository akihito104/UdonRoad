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

package com.freshdigitable.udonroad;

import android.view.View;

/**
 * Created by akihit on 2017/08/10.
 */
public interface OnSpanClickListener {
  void onClicked(View v, SpanItem item);

  class SpanItem {
    public static final int TYPE_URL = 0;
    public static final int TYPE_MENTION = 1;
    public static final int TYPE_HASHTAG = 2;

    private int type;
    private int start, end;

    private long idForQuery;
    private String query;

    public SpanItem(int type, int start, int end, String query) {
      this.type = type;
      this.start = start;
      this.end = end;
      this.query = query;
    }

    public SpanItem(int type, int start, int end, long id) {
      this.type = type;
      this.start = start;
      this.end = end;
      this.idForQuery = id;
    }

    public int getType() {
      return type;
    }

    public String getQuery() {
      return query;
    }

    public long getId() {
      return idForQuery;
    }

    public int getStart() {
      return start;
    }

    public int getEnd() {
      return end;
    }
  }
}
