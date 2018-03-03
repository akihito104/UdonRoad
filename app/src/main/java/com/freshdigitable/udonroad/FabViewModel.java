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
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.IdRes;
import android.view.MenuItem;

import com.freshdigitable.udonroad.ffab.IndicatableFFAB;
import com.freshdigitable.udonroad.listitem.ListItem;
import com.freshdigitable.udonroad.listitem.StatusListItem;
import com.freshdigitable.udonroad.listitem.TwitterReactionContainer.ReactionIcon;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by akihit on 2018/01/06.
 */

public class FabViewModel extends ViewModel {

  private final MutableLiveData<Type> fabState;
  private final MutableLiveData<List<MenuState>> menuState;
  private final MutableLiveData<MenuItem> menuSelectedEvent;

  public FabViewModel() {
    fabState = new MutableLiveData<>();
    menuState = new MutableLiveData<>();
    menuSelectedEvent = new MutableLiveData<>();
  }

  LiveData<Type> getFabState() {
    return fabState;
  }

  public void showFab(Type type) {
    fabState.setValue(type);
  }

  void hideFab() {
    fabState.setValue(Type.HIDE);
  }

  public enum Type {
    FAB, TOOLBAR, HIDE
  }

  LiveData<List<MenuState>> getMenuState() {
    return menuState;
  }

  void setMenuState(final StatusListItem status) {
    setMenuState(status.getStats());
  }

  public void setMenuState(List<ListItem.Stat> stats) {
    final ArrayList<MenuState> menuStates = new ArrayList<>();
    for (ListItem.Stat stat : stats) {
      final int type = stat.getType();
      if (type == ReactionIcon.RETWEET.type) {
        menuStates.add(new MenuState(R.id.iffabMenu_main_rt, stat.isMarked()));
      } else if (type == ReactionIcon.FAV.type) {
        menuStates.add(new MenuState(R.id.iffabMenu_main_fav, stat.isMarked()));
      }
    }
    menuState.setValue(menuStates);
  }

  static class MenuState {
    final @IdRes int menuId;
    final boolean checked;

    private MenuState(@IdRes int menuId, boolean marked) {
      this.menuId = menuId;
      this.checked = marked;
    }

    @Override
    public String toString() {
      return "MenuState{" +
          "menuId=" + menuId +
          ", checked=" + checked +
          '}';
    }
  }

  static Observer<List<MenuState>> createMenuStateObserver(IndicatableFFAB ffab) {
    return states -> {
      if (states == null || states.isEmpty()) {
        return;
      }
      for (MenuState state : states) {
        ffab.getMenu().findItem(state.menuId).setChecked(state.checked);
      }
    };
  }

  void onMenuItemSelected(MenuItem item) {
    menuSelectedEvent.setValue(item);
    menuSelectedEvent.setValue(null); // XXX
  }

  public LiveData<MenuItem> getMenuItem() {
    return menuSelectedEvent;
  }
}
