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

package com.freshdigitable.udonroad.detail;

import com.freshdigitable.udonroad.databinding.ViewStatusDetailBinding;
import com.freshdigitable.udonroad.listitem.StatusViewImageLoader;
import com.freshdigitable.udonroad.listitem.TwitterListItem;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

/**
 * Created by akihit on 2018/01/28.
 */

class StatusDetailViewImageLoader {
  private StatusViewImageLoader imageLoader;

  @Inject
  StatusDetailViewImageLoader(StatusViewImageLoader imageLoader) {
    this.imageLoader = imageLoader;
  }

  Disposable loadImages(ViewStatusDetailBinding binding, TwitterListItem item) {
    final CompositeDisposable compositeDisposable = new CompositeDisposable();
    compositeDisposable.add(imageLoader.loadUserIcon(item.getUser(), binding.dIcon));
    compositeDisposable.add(imageLoader.loadMediaView(item, binding.dImageGroup));
    compositeDisposable.add(imageLoader.loadQuotedStatusImages(item, binding.dQuoted));
    return compositeDisposable;
  }
}
