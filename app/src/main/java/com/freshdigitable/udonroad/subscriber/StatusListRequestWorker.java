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

package com.freshdigitable.udonroad.subscriber;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.datastore.SortedCache;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;

import java.util.Arrays;

import javax.inject.Inject;

import io.reactivex.Observable;
import twitter4j.Paging;
import twitter4j.Status;

/**
 * Created by akihit on 2017/03/31.
 */

public class StatusListRequestWorker implements ListRequestWorker<Status> {
  private final StatusRequestWorker<SortedCache<Status>> requestWorker;
  private StoreType storeType;
  private String storeName;

  @Inject
  public StatusListRequestWorker(StatusRequestWorker<SortedCache<Status>> requestWorker) {
    this.requestWorker = requestWorker;
  }

  @Override
  public void open(@NonNull StoreType type, @Nullable String suffix) {
    if (!type.isForStatus()) {
      throw new IllegalArgumentException();
    }
    this.storeType = type;
    storeName = TextUtils.isEmpty(suffix)
        ? type.storeName : type.prefix() + suffix;
    requestWorker.open(storeName);
  }

  @Override
  public SortedCache<Status> getCache() {
    return requestWorker.getCache();
  }

  @Override
  public ListFetchStrategy getFetchStrategy(final long id) {
    if (storeType == StoreType.HOME) {
      return new ListFetchStrategy() {
        @Override
        public void fetch() {
          requestWorker.fetchHomeTimeline();
        }

        @Override
        public void fetch(Paging paging) {
          requestWorker.fetchHomeTimeline(paging);
        }
      };
    } else if (storeType == StoreType.USER_HOME) {
      return new ListFetchStrategy() {
        @Override
        public void fetch() {
          requestWorker.fetchHomeTimeline(id);
        }

        @Override
        public void fetch(Paging paging) {
          requestWorker.fetchHomeTimeline(id, paging);
        }
      };
    } else if (storeType == StoreType.USER_FAV) {
      return new ListFetchStrategy() {
        @Override
        public void fetch() {
          requestWorker.fetchFavorites(id);
        }

        @Override
        public void fetch(Paging paging) {
          requestWorker.fetchFavorites(id, paging);
        }
      };
    } else if (storeType == StoreType.CONVERSATION) {
      return new ListFetchStrategy() {
        @Override
        public void fetch() {
          requestWorker.fetchConversations(id);
        }

        @Override
        public void fetch(Paging paging) {
        }
      };
    }
    throw new IllegalStateException();
  }

  @Override
  public OnIffabItemSelectedListener getOnIffabItemSelectedListener(long selectedId) {
    return item -> {
      final int itemId = item.getItemId();
      if (itemId == R.id.iffabMenu_main_fav) {
        if (!item.isChecked()) {
          requestWorker.createFavorite(selectedId);
        } else {
          requestWorker.destroyFavorite(selectedId);
        }
      } else if (itemId == R.id.iffabMenu_main_rt) {
        if (!item.isChecked()) {
          requestWorker.retweetStatus(selectedId);
        } else {
          requestWorker.destroyRetweet(selectedId);
        }
      } else if (itemId == R.id.iffabMenu_main_favRt) {
        Observable.concatDelayError(Arrays.asList(
            requestWorker.observeCreateFavorite(selectedId).toObservable(),
            requestWorker.observeRetweetStatus(selectedId).toObservable())
        ).subscribe(s -> {}, e -> {});
      }
    };
  }

  @Override
  public void close() {
    requestWorker.close();
  }

  @Override
  public void drop() {
    requestWorker.getCache().drop(storeName);
  }
}
