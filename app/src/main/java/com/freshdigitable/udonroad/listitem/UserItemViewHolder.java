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

import android.view.ViewGroup;
import android.widget.ImageView;

import com.freshdigitable.udonroad.Utils;

import io.reactivex.disposables.Disposable;

/**
 * Created by akihit on 2017/07/12.
 */

public class UserItemViewHolder extends ItemViewHolder {

  private Disposable iconSubscription;

  public UserItemViewHolder(ViewGroup parent) {
    super(new UserItemView(parent.getContext()));
  }

  @Override
  public void bind(ListItem item, StatusViewImageLoader imageLoader) {
    super.bind(item, imageLoader);
    getView().bind(item);
    Utils.maybeDispose(iconSubscription);
    iconSubscription = imageLoader.loadUserIcon(item.getUser(), getItemId(), getView());
  }

  @Override
  public void unsubscribe() {
    super.unsubscribe();
    Utils.maybeDispose(iconSubscription);
  }

  @Override
  public void onUpdate(ListItem item) {
    getView().update((TwitterListItem) item);
  }

  public UserItemView getView() {
    return (UserItemView) itemView;
  }

  @Override
  public ImageView getUserIcon() {
    return getView().getIcon();
  }

  @Override
  public void onSelected(long itemId) {}

  @Override
  public void onUnselected(long itemId) {}
}
