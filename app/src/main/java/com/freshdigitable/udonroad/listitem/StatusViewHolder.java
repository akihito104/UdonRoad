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

package com.freshdigitable.udonroad.listitem;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.Utils;
import com.freshdigitable.udonroad.databinding.ViewQuotedStatusBinding;
import com.freshdigitable.udonroad.databinding.ViewStatusBinding;
import com.freshdigitable.udonroad.media.MediaViewActivity;
import com.freshdigitable.udonroad.media.ThumbnailContainer;
import com.freshdigitable.udonroad.timeline.SelectedItem;
import com.freshdigitable.udonroad.timeline.TimelineViewModel;

import io.reactivex.disposables.Disposable;

/**
 * Created by akihit on 2017/07/12.
 */

public class StatusViewHolder extends ItemViewHolder {

  private final ViewStatusBinding binding;
  private final TimelineViewModel viewModel;
  private Disposable imageSubs;
  private ViewQuotedStatusBinding quotedBinding;

  public StatusViewHolder(@NonNull ViewStatusBinding binding,
                          @NonNull TimelineViewModel viewModel) {
    super(binding.getRoot());
    this.binding = binding;
    this.viewModel = viewModel;
  }

  @Override
  public void bind(ListItem item, StatusViewImageLoader imageLoader) {
    super.bind(item, imageLoader);
    final TwitterListItem twitterListItem = (TwitterListItem) item;
    binding.setViewModel(viewModel);
    binding.setItem(twitterListItem);
    binding.executePendingBindings();
    bindQuotedStatus(twitterListItem.getId(), twitterListItem.getQuotedItem());
    Utils.maybeDispose(imageSubs);
    if (imageLoader != null) {
      imageSubs = imageLoader.load(twitterListItem, binding, quotedBinding);
    }
    setupMediaView(item.getId(), twitterListItem, binding.tlImageGroup);
    setupQuotedStatusView(twitterListItem, quotedBinding);
  }

  private void bindQuotedStatus(long containerItemId, TwitterListItem quotedItem) {
    if (quotedItem == null && quotedBinding == null) {
      return;
    } else if (quotedItem != null && quotedBinding == null) {
      final LayoutInflater layoutInflater = LayoutInflater.from(itemView.getContext());
      quotedBinding = ViewQuotedStatusBinding.inflate(layoutInflater, (ViewGroup) binding.getRoot(), false);
      attachQuotedView(quotedBinding);
    }
    quotedBinding.setViewModel(viewModel);
    quotedBinding.setContainerItemId(containerItemId);
    quotedBinding.setItem(quotedItem);
    quotedBinding.executePendingBindings();
  }

  public void updateTime() {
    updateTime(binding.tlCreateAt, binding.getItem());
    if (quotedBinding != null && quotedBinding.getRoot().getVisibility() == View.VISIBLE) {
      updateTime(quotedBinding.qCreateAt, quotedBinding.getItem());
    }
  }

  private void updateTime(@NonNull TextView createdAt, @Nullable TwitterListItem item) {
    if (item == null) {
      return;
    }
    final TwitterListItem.TimeTextStrategy timeStrategy = item.getTimeStrategy();
    if (timeStrategy == null) {
      return;
    }
    createdAt.setText(timeStrategy.getCreatedTime(createdAt.getContext()));
  }

  @Override
  public ImageView getUserIcon() {
    return binding.tlIcon;
  }

  @Override
  public void onUpdate(ListItem item) {
    if (item == null) {
      return;
    }
    binding.tlReactionContainer.update(item.getStats());
    binding.tlNames.setNames(item.getCombinedName());
    if (quotedBinding != null) {
      final TwitterListItem quotedItem = ((TwitterListItem) item).getQuotedItem();
      if (quotedItem == null) {
        quotedBinding.setItem(null);
        return;
      }
      quotedBinding.qReactionContainer.update(quotedItem.getStats());
      quotedBinding.qNames.setNames(quotedItem.getCombinedName());
    }
  }

  @Override
  public void recycle() {
    super.recycle();
    binding.tlImageGroup.reset();
    unloadMediaView(binding.tlImageGroup);
    if (quotedBinding != null && quotedBinding.getRoot().getVisibility() == View.VISIBLE) {
      unloadMediaView(quotedBinding.qImageGroup);
    }
    Utils.maybeDispose(imageSubs);
  }

  private void setupMediaView(long containerItemId, TwitterListItem item, ThumbnailContainer thumbnailContainer) {
    final long statusId = item.getId();
    thumbnailContainer.setOnMediaClickListener((view, index) -> {
      final SelectedItem selectedItem = viewModel.selectedItem.get();
      if (selectedItem == null || SelectedItem.NONE.equals(selectedItem) || !selectedItem.isSame(containerItemId, statusId)) {
        viewModel.setSelectedItem(containerItemId, statusId);
      }
      MediaViewActivity.start(view.getContext(), item, index);
    });
  }

  private void setupQuotedStatusView(TwitterListItem status, final ViewQuotedStatusBinding quotedBinding) {
    final TwitterListItem quotedStatus = status.getQuotedItem();
    if (quotedStatus == null) {
      return;
    }
    setupMediaView(status.getId(), quotedStatus, quotedBinding.qImageGroup);
  }

  private void unloadMediaView(ThumbnailContainer thumbnailContainer) {
    for (int i = 0; i < thumbnailContainer.getThumbCount(); i++) {
      thumbnailContainer.getChildAt(i).setOnClickListener(null);
    }
  }

  public boolean hasQuotedView() {
    return quotedBinding != null;
  }

  public ViewQuotedStatusBinding getQuotedStatusView() {
    return quotedBinding;
  }

  public void attachQuotedView(ViewQuotedStatusBinding quotedBinding) {
    final RelativeLayout.LayoutParams lp = createQuotedItemLayoutParams();
    ((ViewGroup) binding.getRoot()).addView(quotedBinding.getRoot(), lp);
    this.quotedBinding = quotedBinding;
  }

  @NonNull
  private RelativeLayout.LayoutParams createQuotedItemLayoutParams() {
    final RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    lp.topMargin = itemView.getResources().getDimensionPixelSize(R.dimen.grid_margin);
    lp.addRule(RelativeLayout.BELOW, R.id.tl_via);
    lp.addRule(RelativeLayout.RIGHT_OF, R.id.tl_icon);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      lp.addRule(RelativeLayout.END_OF, R.id.tl_icon);
    }
    return lp;
  }

  public ViewQuotedStatusBinding detachQuotedView() {
    if (quotedBinding == null) {
      return null;
    }
    ((ViewGroup) binding.getRoot()).removeView(quotedBinding.getRoot());
    final ViewQuotedStatusBinding res = this.quotedBinding;
    this.quotedBinding = null;
    return res;
  }
}
