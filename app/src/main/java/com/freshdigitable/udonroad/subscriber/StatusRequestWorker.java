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

package com.freshdigitable.udonroad.subscriber;

import android.support.annotation.NonNull;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;

import java.util.Arrays;

import javax.inject.Inject;

import io.reactivex.Observable;

/**
 * StatusRequestWorker creates twitter request for status resources and subscribes its response
 * with user feedback.
 * <p>
 * Created by akihit on 2016/08/01.
 */
public class StatusRequestWorker implements RequestWorker {
  private static final String TAG = StatusRequestWorker.class.getSimpleName();
  private StatusRepository repository;

  @Inject
  public StatusRequestWorker(@NonNull StatusRepository repository) {
    this.repository = repository;
  }

  @Override
  public OnIffabItemSelectedListener getOnIffabItemSelectedListener(long selectedId) {
    return item -> {
      final int itemId = item.getItemId();
      if (itemId == R.id.iffabMenu_main_fav) {
        if (!item.isChecked()) {
          repository.createFavorite(selectedId);
        } else {
          repository.destroyFavorite(selectedId);
        }
      } else if (itemId == R.id.iffabMenu_main_rt) {
        if (!item.isChecked()) {
          repository.retweetStatus(selectedId);
        } else {
          repository.destroyRetweet(selectedId);
        }
      } else if (itemId == R.id.iffabMenu_main_favRt) {
        Observable.concatDelayError(Arrays.asList(
            repository.observeCreateFavorite(selectedId).toObservable(),
            repository.observeRetweetStatus(selectedId).toObservable())
        ).subscribe(s -> {
        }, e -> {
        });
      }
    };
  }
}
