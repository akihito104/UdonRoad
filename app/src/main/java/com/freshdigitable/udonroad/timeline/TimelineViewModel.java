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

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.databinding.ObservableField;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.datastore.UpdateEvent;
import com.freshdigitable.udonroad.listitem.ListItem;
import com.freshdigitable.udonroad.listitem.StatusViewImageLoader;
import com.freshdigitable.udonroad.oauth.DemoTimelineAdapter;
import com.freshdigitable.udonroad.timeline.repository.ListItemRepository;
import com.freshdigitable.udonroad.timeline.repository.ListItemRepositoryFactory;

import java.util.EnumSet;

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
  private final MutableLiveData<SelectedItem> selectedItemSource = new MutableLiveData<>();
  public final ObservableField<SelectedItem> selectedItem = new ObservableField<>(SelectedItem.NONE);
  private final Observer<SelectedItem> selectedItemObserver = selectedItem::set;

  @Inject
  TimelineViewModel(ListItemRepositoryFactory repositoryFactory,
                    StatusViewImageLoader imageLoader) {
    this.repositoryFactory = repositoryFactory;
    this.imageLoader = imageLoader;
    selectedItemSource.setValue(SelectedItem.NONE);
    selectedItemSource.observeForever(selectedItemObserver);
    autoScrollSource.setValue(EnumSet.noneOf(AutoScrollStopper.class));
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

  @Nullable
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

  public void setSelectedItem(long containerItemId, long selectedItemId) {
    final SelectedItem selectedItem = new SelectedItem(containerItemId, selectedItemId);
    setSelectedItem(selectedItem);
  }

  public void setSelectedItem(SelectedItem selectedItem) {
    if (selectedItem.equals(this.selectedItemSource.getValue())) {
      clearSelectedItem();
    } else {
      this.selectedItemSource.setValue(selectedItem);
    }
  }

  public void clearSelectedItem() {
    selectedItemSource.setValue(SelectedItem.NONE);
  }

  public long getSelectedItemId() {
    return selectedItemSource.getValue().getId();
  }

  public boolean isItemSelected() {
    return SelectedItem.NONE != selectedItemSource.getValue();
  }

  public int getSelectedItemViewPosition() {
    return getPositionById(selectedItemSource.getValue().getId());
  }

  LiveData<SelectedItem> getSelectedItem() {
    return selectedItemSource;
  }

  @Override
  protected void onCleared() {
    Timber.tag(TimelineViewModel.class.getSimpleName()).d("onCleared: ");
    super.onCleared();
    listItemRepository.close();
    selectedItemSource.removeObserver(selectedItemObserver);
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

  private final MutableLiveData<EnumSet<AutoScrollStopper>> autoScrollSource = new MutableLiveData<>();

  LiveData<EnumSet<AutoScrollStopper>> getAutoScrollStopper() {
    return autoScrollSource;
  }

  void addAutoScrollStopper(AutoScrollStopper stopper) {
    final EnumSet<AutoScrollStopper> value = autoScrollSource.getValue();
    value.add(stopper);
    autoScrollSource.setValue(value);
  }

  void removeAutoScrollStopper(AutoScrollStopper stopper) {
    final EnumSet<AutoScrollStopper> value = autoScrollSource.getValue();
    value.remove(stopper);
    autoScrollSource.setValue(value);
  }

  void enableAutoScroll() {
    final EnumSet<AutoScrollStopper> value = autoScrollSource.getValue();
    value.clear();
    autoScrollSource.setValue(value);
  }

  private static final String SS_AUTO_SCROLL_STATE = "ss_auto_scroll_state";
  private static final String SS_SELECTED_ITEM = "ss_selectedItem";

  void onSaveInstanceState(@NonNull Bundle outState) {
    outState.putSerializable(SS_AUTO_SCROLL_STATE, autoScrollSource.getValue());
    outState.putParcelable(SS_SELECTED_ITEM, selectedItemSource.getValue());
  }

  void onViewRestored(@Nullable Bundle savedInstanceState) {
    if (savedInstanceState == null) {
      return;
    }
    final EnumSet<AutoScrollStopper> autoScrollState =
        (EnumSet<AutoScrollStopper>) savedInstanceState.getSerializable(SS_AUTO_SCROLL_STATE);
    if (autoScrollState != null) {
      final EnumSet<AutoScrollStopper> value = this.autoScrollSource.getValue();
      value.addAll(autoScrollState);
      this.autoScrollSource.setValue(value);
    }
    final SelectedItem selectedItem = savedInstanceState.getParcelable(SS_SELECTED_ITEM);
    setSelectedItem(selectedItem);
  }

  public static TimelineViewModel getInstance(Fragment fragment, ViewModelProvider.Factory factory) {
    return ViewModelProviders.of(fragment, factory).get(TimelineViewModel.class);
  }
}
