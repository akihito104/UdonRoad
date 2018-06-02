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

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.databinding.ViewQuotedStatusBinding;
import com.freshdigitable.udonroad.databinding.ViewStatusBinding;
import com.freshdigitable.udonroad.databinding.ViewUserListBinding;
import com.freshdigitable.udonroad.listitem.ItemViewHolder;
import com.freshdigitable.udonroad.listitem.ListItem;
import com.freshdigitable.udonroad.listitem.OnItemViewClickListener;
import com.freshdigitable.udonroad.listitem.OnUserIconClickedListener;
import com.freshdigitable.udonroad.listitem.StatusListItem;
import com.freshdigitable.udonroad.listitem.StatusViewHolder;
import com.freshdigitable.udonroad.listitem.UserItemViewHolder;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;

/**
 * TimelineAdapter is a adapter for RecyclerView.
 *
 * Created by akihit on 15/10/18.
 */
public class TimelineAdapter extends RecyclerView.Adapter<ItemViewHolder> {
  @SuppressWarnings("unused")
  private static final String TAG = TimelineAdapter.class.getSimpleName();
  protected final TimelineViewModel viewModel;
  private final StoreType type;

  public TimelineAdapter(TimelineViewModel viewModel) {
    this.viewModel = viewModel;
    type = viewModel.getStoreType();
    setHasStableIds(true);
  }

  @Override
  public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
    if (type.isForStatus()) {
      final ViewStatusBinding binding = ViewStatusBinding.inflate(layoutInflater, parent, false);
      return new StatusViewHolder(binding, viewModel);
    } else if (type.isForUser() || type.isForLists()) {
      final ViewUserListBinding binding = ViewUserListBinding.inflate(layoutInflater, parent, false);
      return new UserItemViewHolder(binding, viewModel);
    }
    throw new IllegalStateException("unsupported StoreType of repository: " + type);
  }

  @Override
  public long getItemId(int position) {
    return viewModel.getId(position);
  }

  @Override
  public void onBindViewHolder(final ItemViewHolder holder, int position) {
    final ListItem elem = viewModel.get(position);
    if (elem == null) {
      return;
    }
    holder.bind(elem);
    final Observable<? extends ListItem> observable = viewModel.observe(elem);
    holder.subscribe(observable);

    if (position == getItemCount() - 1) {
      lastItemBoundListener.onLastItemBound();
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
          = viewModel.observeById(holder.getItemId());
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
    holder.recycle();
  }

  private OnItemViewClickListener itemClickListener;

  private final OnItemViewClickListener itemViewClickListener = (vh, itemId, clickedItem) -> {
    if (itemClickListener != null) {
      itemClickListener.onItemViewClicked(vh, itemId, clickedItem);
    }
  };

  public interface LastItemBoundListener {
    void onLastItemBound();
  }

  private LastItemBoundListener lastItemBoundListener;

  public void setLastItemBoundListener(LastItemBoundListener listener) {
    this.lastItemBoundListener = listener;
  }

  void setOnItemViewClickListener(OnItemViewClickListener listener) {
    this.itemClickListener = listener;
  }

  @Override
  public int getItemCount() {
    return viewModel.getItemCount();
  }

  private OnUserIconClickedListener userIconClickedListener;

  public void setOnUserIconClickedListener(OnUserIconClickedListener listener) {
    this.userIconClickedListener = listener;
  }

  public static class StatusTimelineAdapter extends TimelineAdapter {
    StatusTimelineAdapter(TimelineViewModel viewModel) {
      super(viewModel);
    }

    @Override
    public void onBindViewHolder(final ItemViewHolder holder, int position) {
      final StatusListItem item = (StatusListItem) viewModel.get(position);
      if (item == null) {
        return;
      }
      final StatusViewHolder statusViewHolder = (StatusViewHolder) holder;
      if (item.getQuotedItem() != null && !statusViewHolder.hasQuotedView()) {
        statusViewHolder.attachQuotedView(getQuotedView((ViewGroup) holder.itemView));
      }
      holder.bind(item);
      final Observable<? extends ListItem> observable = viewModel.observe(item);
      holder.subscribe(observable);

      if (position == getItemCount() - 1) {
        super.lastItemBoundListener.onLastItemBound();
      }
    }

    private final List<ViewQuotedStatusBinding> quotedViewCache = new ArrayList<>();

    private ViewQuotedStatusBinding getQuotedView(ViewGroup parent) {
      if (quotedViewCache.isEmpty()) {
        return ViewQuotedStatusBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
      }
      return quotedViewCache.remove(quotedViewCache.size() - 1);
    }

    @Override
    public void onViewRecycled(ItemViewHolder holder) {
      super.onViewRecycled(holder);
      final ViewQuotedStatusBinding v = ((StatusViewHolder) holder).detachQuotedView();
      if (v != null) {
        quotedViewCache.add(v);
      }
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
      quotedViewCache.clear();
    }
  }
}
