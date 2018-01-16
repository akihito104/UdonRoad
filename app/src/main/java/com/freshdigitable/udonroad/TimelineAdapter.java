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

package com.freshdigitable.udonroad;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.freshdigitable.udonroad.listitem.ItemViewHolder;
import com.freshdigitable.udonroad.listitem.ListItem;
import com.freshdigitable.udonroad.listitem.OnItemViewClickListener;
import com.freshdigitable.udonroad.listitem.OnUserIconClickedListener;
import com.freshdigitable.udonroad.listitem.QuotedStatusView;
import com.freshdigitable.udonroad.listitem.StatusListItem;
import com.freshdigitable.udonroad.listitem.StatusViewHolder;
import com.freshdigitable.udonroad.listitem.StatusViewImageLoader;
import com.freshdigitable.udonroad.listitem.UserItemViewHolder;
import com.freshdigitable.udonroad.media.ThumbnailView;
import com.freshdigitable.udonroad.timeline.repository.ListItemRepository;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import twitter4j.Status;
import twitter4j.User;
import twitter4j.UserList;

/**
 * TimelineAdapter is a adapter for RecyclerView.
 *
 * Created by akihit on 15/10/18.
 */
public abstract class TimelineAdapter<T> extends RecyclerView.Adapter<ItemViewHolder> {
  @SuppressWarnings("unused")
  private static final String TAG = TimelineAdapter.class.getSimpleName();

  final StatusViewImageLoader imageLoader;
  final ListItemRepository repository;

  TimelineAdapter(ListItemRepository repository, StatusViewImageLoader imageLoader) {
    this.repository = repository;
    this.imageLoader = imageLoader;
    setHasStableIds(true);
  }

  @Override
  public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final StoreType type = repository.getStoreType();
    if (type.isForStatus()) {
      return new StatusViewHolder(parent);
    } else if (type.isForUser() || type.isForLists()) {
      return new UserItemViewHolder(parent);
    }
    throw new IllegalStateException("unsupported StoreType of repository: " + type);
  }

  @Override
  public long getItemId(int position) {
    return repository.getId(position);
  }

  @Override
  public void onBindViewHolder(final ItemViewHolder holder, int position) {
    final ListItem elem = repository.get(position);
    holder.bind(elem, imageLoader);
    final Observable<? extends ListItem> observable = repository.observe(elem);
    holder.subscribe(observable);

    if (position == getItemCount() - 1) {
      lastItemBoundListener.onLastItemBound();
    }

    bindSelectedItemView(holder);
  }

  private void bindSelectedItemView(ItemViewHolder holder) {
    if (!isItemSelected()) {
      return;
    }
    final long selectedItemId = getSelectedItemId();
    if (holder.getItemId() == selectedItemId) {
      selectedItemHolder = new SelectedItem(holder);
    } else if (holder.hasQuotedItem()) {
      final long quotedItemId = holder.getQuotedItemId();
      if (quotedItemId == selectedItemId) {
        selectedItemHolder = new SelectedItem(holder, quotedItemId);
      }
    }
  }

  @Override
  public void onViewAttachedToWindow(ItemViewHolder holder) {
    super.onViewAttachedToWindow(holder);
    subscribe(holder);
    holder.setItemViewClickListener(itemViewClickListener);
    holder.setUserIconClickedListener(userIconClickedListener);
  }

  private void subscribe(ItemViewHolder holder) {
    if (!holder.isSubscribed()) {
      final Observable<? extends ListItem> observable
          = repository.observeById(holder.getItemId());
      holder.subscribe(observable);
    }
  }

  @Override
  public void onViewDetachedFromWindow(ItemViewHolder holder) {
    super.onViewDetachedFromWindow(holder);
    holder.setItemViewClickListener(null);
    holder.setUserIconClickedListener(null);
    holder.unsubscribe();
  }

  @Override
  public void onViewRecycled(ItemViewHolder holder) {
    super.onViewRecycled(holder);
    if (holder.hasSameItemId(selectedItemHolder.id)) {
      selectedItemHolder.onViewRecycled();
    }
    holder.recycle();
  }

  private static final SelectedItem EMPTY = new SelectedItem();
  private SelectedItem selectedItemHolder = EMPTY;

  private OnItemViewClickListener itemClickListener;

  private final OnItemViewClickListener itemViewClickListener = (vh, itemId, clickedItem) -> {
    if (itemClickListener != null) {
      itemClickListener.onItemViewClicked(vh, itemId, clickedItem);
      return;
    }
    if (isItemSelected()
        && itemId == selectedItemHolder.id) {
      if (clickedItem instanceof ThumbnailView) {
        return;
      }
      clearSelectedItem();
    } else {
      fixSelectedItem(itemId, vh);
    }
  };

  public void clearSelectedItem() {
    if (isItemSelected()) {
      selectedItemHolder.setUnselectedBackground();
    }
    selectedItemHolder = EMPTY;
    if (selectedItemChangeListener != null) {
      selectedItemChangeListener.onItemUnselected();
    }
  }

  private void fixSelectedItem(long selectedItemId, ItemViewHolder selectedView) {
    if (isItemSelected()) {
      selectedItemHolder.setUnselectedBackground();
    }
    selectedItemHolder = new SelectedItem(selectedView, selectedItemId);
    if (selectedItemChangeListener != null) {
      selectedItemChangeListener.onItemSelected(selectedItemId);
    }
  }

  public long getSelectedItemId() {
    return selectedItemHolder.id;
  }

  public boolean isItemSelected() {
    return selectedItemHolder != EMPTY;
  }

  public int getSelectedItemViewPosition() {
    return repository.getPositionById(selectedItemHolder.containerId);
  }

  public interface LastItemBoundListener {
    void onLastItemBound();
  }

  private LastItemBoundListener lastItemBoundListener;

  public void setLastItemBoundListener(LastItemBoundListener listener) {
    this.lastItemBoundListener = listener;
  }

  private OnSelectedItemChangeListener selectedItemChangeListener;

  public void setOnSelectedItemChangeListener(OnSelectedItemChangeListener listener) {
    this.selectedItemChangeListener = listener;
  }

  interface OnSelectedItemChangeListener {
    void onItemSelected(long itemId);
    void onItemUnselected();
  }

  void setOnItemViewClickListener(OnItemViewClickListener listener) {
    this.itemClickListener = listener;
  }

  @Override
  public int getItemCount() {
    return repository.getItemCount();
  }

  private static class SelectedItem {
    private final long id;
    private final long containerId;
    private final WeakReference<ItemViewHolder> viewHolder;

    private SelectedItem() {
      this(null, -1);
    }

    private SelectedItem(ItemViewHolder vh) {
      this(vh, vh.getItemId());
    }

    private SelectedItem(ItemViewHolder vh, long id) {
      this.id = id;
      this.containerId = vh != null ? vh.getItemId() : -1;
      this.viewHolder = vh != null
          ? new WeakReference<>(vh)
          : null;
      setSelectedBackground();
    }

    /**
     * for restore saved state
     *
     * @param selectedContainerId container ID that has selected item
     * @param selectedItemId ID of actually selected item
     */
    private SelectedItem(long selectedContainerId, long selectedItemId) {
      this.id = selectedItemId;
      this.containerId = selectedContainerId;
      this.viewHolder = null;
    }

    private void setSelectedBackground() {
      final ItemViewHolder vh = getViewHolder();
      if (vh != null) {
        vh.onSelected(id);
      }
    }

    private void setUnselectedBackground() {
      final ItemViewHolder vh = getViewHolder();
      if (vh != null) {
        vh.onUnselected(id);
      }
    }

    private void onViewRecycled() {
      final ItemViewHolder viewHolder = getViewHolder();
      if (viewHolder != null) {
        this.viewHolder.clear();
      }
    }

    @Nullable
    private ItemViewHolder getViewHolder() {
      return this.viewHolder != null ? this.viewHolder.get() : null;
    }
  }

  private OnUserIconClickedListener userIconClickedListener;

  public void setOnUserIconClickedListener(OnUserIconClickedListener listener) {
    this.userIconClickedListener = listener;
  }

  public static class StatusTimelineAdapter extends TimelineAdapter<Status> {
    StatusTimelineAdapter(ListItemRepository repository, StatusViewImageLoader imageLoader) {
      super(repository, imageLoader);
    }

    @Override
    public void onBindViewHolder(final ItemViewHolder holder, int position) {
      final StatusListItem item = ((StatusListItem) repository.get(position));
      final StatusViewHolder statusViewHolder = (StatusViewHolder) holder;
      if (item.getQuotedItem() != null && !statusViewHolder.hasQuotedView()) {
        statusViewHolder.attachQuotedView(getQuotedView(holder.itemView.getContext()));
      }
      holder.bind(item, imageLoader);
      final Observable<? extends ListItem> observable = repository.observe(item);
      holder.subscribe(observable);

      if (position == getItemCount() - 1) {
        super.lastItemBoundListener.onLastItemBound();
      }

      super.bindSelectedItemView(holder);
    }

    private final List<QuotedStatusView> quotedViewCache = new ArrayList<>();

    private QuotedStatusView getQuotedView(Context context) {
      if (quotedViewCache.isEmpty()) {
        return new QuotedStatusView(context);
      }
      return quotedViewCache.remove(quotedViewCache.size() - 1);
    }

    @Override
    public void onViewRecycled(ItemViewHolder holder) {
      super.onViewRecycled(holder);
      final QuotedStatusView v = ((StatusViewHolder) holder).detachQuotedView();
      if (v != null) {
        quotedViewCache.add(v);
      }
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
      quotedViewCache.clear();
    }
  }

  public static class UserListAdapter extends TimelineAdapter<User> {
    UserListAdapter(ListItemRepository repository, StatusViewImageLoader imageLoader) {
      super(repository, imageLoader);
    }
  }

  public static class ListListAdapter extends TimelineAdapter<UserList> {
    ListListAdapter(ListItemRepository repository, StatusViewImageLoader imageLoader) {
      super(repository, imageLoader);
    }
  }

  Parcelable onSaveInstanceState() {
    final SavedState savedState = new SavedState();
    if (selectedItemHolder != null) {
      savedState.selectedContainerId = selectedItemHolder.containerId;
      savedState.selectedItemId = selectedItemHolder.id;
    }
    return savedState;
  }

  void onRestoreInstanceState(Parcelable parcelable) {
    if (!(parcelable instanceof SavedState)) {
      return;
    }
    final SavedState ss = (SavedState) parcelable;
    if (ss.selectedItemId == EMPTY.id && ss.selectedContainerId == EMPTY.containerId) {
      selectedItemHolder = EMPTY;
    } else {
      selectedItemHolder = new SelectedItem(ss.selectedContainerId, ss.selectedItemId);
    }
  }

  private static class SavedState implements Parcelable {
    long selectedContainerId = EMPTY.containerId;
    long selectedItemId = EMPTY.id;

    private SavedState() {}

    SavedState(Parcel in) {
      selectedContainerId = in.readLong();
      selectedItemId = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
      parcel.writeLong(selectedContainerId);
      parcel.writeLong(selectedItemId);
    }

    public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
      @Override
      public SavedState createFromParcel(Parcel in) {
        return new SavedState(in);
      }

      @Override
      public SavedState[] newArray(int size) {
        return new SavedState[size];
      }
    };

    @Override
    public int describeContents() {
      return 0;
    }
  }
}
