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

package com.freshdigitable.udonroad.timeline.repository;

import android.support.annotation.NonNull;

import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.datastore.UpdateEvent;
import com.freshdigitable.udonroad.listitem.ListItem;

import io.reactivex.Flowable;
import io.reactivex.Observable;

/**
 * Created by akihit on 2018/01/13.
 */

public interface ListItemRepository {
  void init(long id, String query);

  void close();

  Flowable<UpdateEvent> observeUpdateEvent();

  long getId(int position);

  ListItem get(int position);

  int getItemCount();

  int getPositionById(long id);

  @NonNull
  Observable<? extends ListItem> observeById(long id);

  @NonNull
  Observable<? extends ListItem> observe(ListItem element);

  void getInitList();

  void getListOnEnd();

  void getListOnStart();

  void drop();

  @NonNull
  StoreType getStoreType();

  abstract class Adapter implements ListItemRepository {
    @Override
    public void getListOnStart() {
      getInitList();
    }
  }
}
