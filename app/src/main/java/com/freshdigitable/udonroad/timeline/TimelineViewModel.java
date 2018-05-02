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

package com.freshdigitable.udonroad.timeline;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.support.v4.app.Fragment;

import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.datastore.UpdateEvent;
import com.freshdigitable.udonroad.listitem.ListItem;
import com.freshdigitable.udonroad.listitem.StatusViewImageLoader;
import com.freshdigitable.udonroad.oauth.DemoTimelineAdapter;
import com.freshdigitable.udonroad.timeline.repository.ListItemRepository;
import com.freshdigitable.udonroad.timeline.repository.ListItemRepositoryFactory;

import javax.inject.Inject;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import timber.log.Timber;

/**
 * Created by akihit on 2018/01/21.
 */

public class TimelineViewModel extends ViewModel {
  private final ListItemRepositoryFactory repositoryFactory;
  private final StatusViewImageLoader imageLoader;
  private ListItemRepository listItemRepository;

  @Inject
  TimelineViewModel(ListItemRepositoryFactory repositoryFactory,
                    StatusViewImageLoader imageLoader) {
    this.repositoryFactory = repositoryFactory;
    this.imageLoader = imageLoader;
  }

  public void init(StoreType storeType, long id, String query) {
    listItemRepository = repositoryFactory.create(storeType);
    listItemRepository.init(id, query);
  }

  public TimelineAdapter createAdapter() {
    final StoreType storeType = listItemRepository.getStoreType();
    if (storeType.isForStatus()) {
      return new TimelineAdapter.StatusTimelineAdapter(this);
    } else if (storeType.isForUser() || storeType.isForLists()) {
      return new TimelineAdapter(this);
    } else if (storeType == StoreType.DEMO) {
      return new DemoTimelineAdapter(this);
    }
    throw new IllegalStateException("not capable of StoreType: " + storeType);
  }

  StoreType getStoreType() {
    return listItemRepository.getStoreType();
  }

  public long getId(int position) {
    return listItemRepository.getId(position);
  }

  public ListItem get(int pos) {
    return listItemRepository.get(pos);
  }

  public Observable<? extends ListItem> observe(ListItem elem) {
    return listItemRepository.observe(elem);
  }

  public Observable<? extends ListItem> observeById(long itemId) {
    return listItemRepository.observeById(itemId);
  }

  public int getItemCount() {
    return listItemRepository.getItemCount();
  }

  public StatusViewImageLoader getImageLoader() {
    return imageLoader;
  }

  @Override
  protected void onCleared() {
    Timber.tag(TimelineViewModel.class.getSimpleName()).d("onCleared: ");
    super.onCleared();
    listItemRepository.close();
  }

  public Flowable<UpdateEvent> observeUpdateEvent() {
    return listItemRepository.observeUpdateEvent();
  }

  private boolean doneFirstFetch = false;

  public void getInitList() {
    if (doneFirstFetch) {
      return;
    }
    listItemRepository.getInitList();
    doneFirstFetch = true;
  }

  public void getListOnStart() {
    listItemRepository.getInitList();
  }

  public void getListOnEnd() {
    listItemRepository.getListOnEnd();
  }

  public ListItem findById(long itemId) {
    final int position = listItemRepository.getPositionById(itemId);
    return listItemRepository.get(position);
  }

  public long getItemIdByPosition(int position) {
    return listItemRepository.getId(position);
  }

  public int getPositionById(long itemId) {
    return listItemRepository.getPositionById(itemId);
  }

  public void drop() {
    listItemRepository.drop();
  }

  public static TimelineViewModel getInstance(Fragment fragment, ViewModelProvider.Factory factory) {
    return ViewModelProviders.of(fragment, factory).get(TimelineViewModel.class);
  }
}
