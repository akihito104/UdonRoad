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

package com.freshdigitable.udonroad.input;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.Utils;
import com.freshdigitable.udonroad.media.ThumbnailContainer;
import com.freshdigitable.udonroad.repository.ImageQuery;

import java.util.List;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

/**
 * Created by akihit on 2017/12/09.
 */

class MediaContainerPresenter implements LifecycleObserver {
  private ThumbnailContainer mediaContainer;
  private TweetInputViewModel viewModel;
  private Disposable mediaUpdateSubs;

  MediaContainerPresenter(ThumbnailContainer mediaContainer, TweetInputViewModel viewModel) {
    this.mediaContainer = mediaContainer;
    this.viewModel = viewModel;
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  void onStart() {
    if(!Utils.isSubscribed(mediaUpdateSubs)) {
      mediaUpdateSubs = this.viewModel.observeMedia().subscribe(this::updateMediaContainer,
          th -> Log.e(this.getClass().getSimpleName(), "mediaUpdate: ", th));
    }
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
  void onStop() {
    Utils.maybeDispose(mediaUpdateSubs);
    Utils.maybeDispose(uploadedMediaSubs);
  }

  private CompositeDisposable uploadedMediaSubs;

  private void updateMediaContainer(List<Uri> media) {
    Utils.maybeDispose(uploadedMediaSubs);
    mediaContainer.reset();
    final int childCount = mediaContainer.getChildCount();
    for (int i = 0; i < childCount; i++) {
      mediaContainer.getChildAt(i).setOnCreateContextMenuListener(null);
    }

    uploadedMediaSubs = new CompositeDisposable();
    mediaContainer.bindMediaEntities(media.size());
    final int thumbCount = mediaContainer.getThumbCount();
    for (int i = 0; i < thumbCount; i++) {
      final Uri uri = media.get(i);
      final ImageView imageView = (ImageView) mediaContainer.getChildAt(i);
      final Single<ImageQuery> query = new ImageQuery.Builder(uri)
          .placeholder(new ColorDrawable(Color.LTGRAY))
          .centerCrop()
          .build(imageView);
      final Disposable d = viewModel.queryImage(query)
          .subscribe(imageView::setImageDrawable, th -> {});
      uploadedMediaSubs.add(d);

      imageView.setOnCreateContextMenuListener((contextMenu, view, contextMenuInfo) -> {
        final MenuItem delete = contextMenu.add(0, 1, 0, R.string.media_upload_delete);
        delete.setOnMenuItemClickListener(menuItem -> {
          viewModel.removeMedia(uri);
          return true;
        });
      });
    }
  }
}
