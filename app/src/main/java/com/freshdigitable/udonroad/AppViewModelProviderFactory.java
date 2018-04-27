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

package com.freshdigitable.udonroad;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Created by akihit on 2018/01/07.
 */
@Singleton
public class AppViewModelProviderFactory implements ViewModelProvider.Factory {
  private Map<Class<? extends ViewModel>, Provider<ViewModel>> providers;

  @Inject
  public AppViewModelProviderFactory(Map<Class<? extends ViewModel>, Provider<ViewModel>> providers) {
    this.providers = providers;
  }


  @NonNull
  @Override
  public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
    return findByClass(modelClass);
  }

  @NonNull
  private <T extends ViewModel> T findByClass(Class<T> modelClass) {
    final Provider<ViewModel> p = providers.get(modelClass);
    if (p != null) {
      return modelClass.cast(p.get());
    }
    for (Map.Entry<Class<? extends ViewModel>, Provider<ViewModel>> entry : providers.entrySet()) {
      if (entry.getKey().isAssignableFrom(modelClass)) {
        return modelClass.cast(entry.getValue());
      }
    }
    throw new IllegalStateException("unknown ViewModel class: " + modelClass);
  }
}
