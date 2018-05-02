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

import android.support.annotation.NonNull;
import android.widget.ImageView;

import com.freshdigitable.udonroad.Utils;
import com.freshdigitable.udonroad.databinding.ViewUserListBinding;
import com.freshdigitable.udonroad.timeline.TimelineViewModel;

import io.reactivex.disposables.Disposable;

/**
 * Created by akihit on 2017/07/12.
 */

public class UserItemViewHolder extends ItemViewHolder {
  private final ViewUserListBinding binding;
  private final TimelineViewModel viewModel;
  private Disposable iconSubscription;

  public UserItemViewHolder(@NonNull ViewUserListBinding binding,
                            @NonNull TimelineViewModel viewModel) {
    super(binding.getRoot());
    this.binding = binding;
    this.viewModel = viewModel;
  }

  @Override
  public void bind(ListItem item, StatusViewImageLoader imageLoader) {
    super.bind(item, imageLoader);
    binding.setItem(item);
    binding.executePendingBindings();
    Utils.maybeDispose(iconSubscription);
    iconSubscription = viewModel.getImageLoader().loadUserIcon(item.getUser(), binding.tlIcon);
  }

  @Override
  public void unsubscribe() {
    super.unsubscribe();
    Utils.maybeDispose(iconSubscription);
  }

  @Override
  public void onUpdate(ListItem item) {
    binding.tlTweet.setText(item.getText());
  }

  @Override
  public ImageView getUserIcon() {
    return binding.tlIcon;
  }

  @Override
  public void onSelected(long itemId) {}

  @Override
  public void onUnselected(long itemId) {}
}
