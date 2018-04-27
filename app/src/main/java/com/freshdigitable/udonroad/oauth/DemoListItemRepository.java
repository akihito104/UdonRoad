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

package com.freshdigitable.udonroad.oauth;

import android.support.annotation.NonNull;

import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.datastore.UpdateEvent;
import com.freshdigitable.udonroad.datastore.UpdateSubject;
import com.freshdigitable.udonroad.datastore.UpdateSubjectFactory;
import com.freshdigitable.udonroad.listitem.ListItem;
import com.freshdigitable.udonroad.timeline.fetcher.ListFetcher;
import com.freshdigitable.udonroad.timeline.repository.ListItemRepository;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * Created by akihit on 2018/01/14.
 */

class DemoListItemRepository implements ListItemRepository {
  private final ListFetcher<ListItem> fetcher;
  private final UpdateSubject updateSubject;
  private final List<ListItem> listItems;

  DemoListItemRepository(ListFetcher<ListItem> fetcher, UpdateSubjectFactory updateSubjectFactory) {
    this.fetcher = fetcher;
    updateSubject = updateSubjectFactory.getInstance("demo");
    listItems = new ArrayList<>();
  }

  @Override
  public Flowable<UpdateEvent> observeUpdateEvent() {
    return updateSubject.observeUpdateEvent();
  }

  @Override
  public long getId(int position) {
    return listItems.get(position).getId();
  }

  @Override
  public ListItem get(int position) {
    return listItems.get(position);
  }

  @Override
  public int getItemCount() {
    return listItems.size();
  }

  @Override
  public int getPositionById(long id) {
    return -1;
  }

  @Override
  public void getInitList() {
    fetcher.fetchInit(null)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(listItems::addAll, th -> {});
  }

  @Override
  public void init(long id, String query) {}

  @Override
  public void close() {}

  @Override
  public void getListOnEnd() {}

  @NonNull
  @Override
  public Observable<? extends ListItem> observeById(long id) {
    return Observable.empty();
  }

  @NonNull
  @Override
  public Observable<? extends ListItem> observe(ListItem element) {
    return Observable.empty();
  }

  @Override
  public void drop() {}

  @NonNull
  @Override
  public StoreType getStoreType() {
    return StoreType.DEMO;
  }
}
