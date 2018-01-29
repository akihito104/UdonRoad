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

import android.databinding.BindingAdapter;
import android.text.TextUtils;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.RoundedCornerImageView;
import com.freshdigitable.udonroad.Utils;
import com.freshdigitable.udonroad.repository.ImageQuery;
import com.freshdigitable.udonroad.repository.ImageRepository;

import javax.inject.Inject;

import io.reactivex.disposables.Disposable;

/**
 * Created by akihit on 2018/01/30.
 */

public class StatusDetailBindingComponent implements android.databinding.DataBindingComponent {
  private final ImageRepository imageRepository;
  private Disposable cardSummaryImageSubs;

  @Inject
  StatusDetailBindingComponent(ImageRepository imageRepository) {
    this.imageRepository = imageRepository;
  }

  @BindingAdapter({"imageUrl"})
  public void bindImage(RoundedCornerImageView imageView, String url) {
    if (TextUtils.isEmpty(url)) {
      return;
    }
    final ImageQuery query = new ImageQuery.Builder(url)
        .sizeForSquare(imageView.getContext(), R.dimen.card_summary_image)
        .centerCrop()
        .build();
    cardSummaryImageSubs = imageRepository.queryImage(query)
        .subscribe(imageView::setImageDrawable, th -> {});
  }

  void dispose() {
    Utils.maybeDispose(cardSummaryImageSubs);
  }

  @Override
  public StatusDetailBindingComponent getStatusDetailBindingComponent() {
    return this;
  }
}
