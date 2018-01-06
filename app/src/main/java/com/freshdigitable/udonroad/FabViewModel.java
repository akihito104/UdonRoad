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

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

/**
 * Created by akihit on 2018/01/06.
 */

public class FabViewModel extends ViewModel {

  private final MutableLiveData<Type> fabState;

  public FabViewModel() {
    fabState = new MutableLiveData<>();
  }

  public LiveData<Type> getFabState() {
    return fabState;
  }

  public void showFab(Type type) {
    fabState.setValue(type);
  }

  public void hideFab() {
    fabState.setValue(Type.HIDE);
  }

  public enum Type {
    FAB, TOOLBAR, HIDE
  }
}
