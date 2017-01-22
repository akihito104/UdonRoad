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

package com.freshdigitable.udonroad.ffab;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;
import com.freshdigitable.udonroad.ffab.OnFlingListener.Direction;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * IffabMenu integrates menu items.
 *
 * Created by akihit on 2017/01/11.
 */

class IffabMenu implements Menu {
  private final List<IffabMenuItem> menuItems = new ArrayList<>();
  private final Context context;
  private OnIffabItemSelectedListener selectedListener;
  private final WeakReference<IffabMenuPresenter> presenter;

  IffabMenu(Context context, IffabMenuPresenter presenter) {
    this.context = context;
    this.presenter = new WeakReference<>(presenter);
  }

  @Override
  public MenuItem add(@StringRes int titleRes) {
    return add(context.getString(titleRes));
  }

  @Override
  public MenuItem add(CharSequence title) {
    return add(0, 0, 0, title);
  }

  @Override
  public MenuItem add(int groupId, int itemId, int order, @StringRes int titleRes) {
    return add(groupId, itemId, order, context.getString(titleRes));
  }

  @Override
  public MenuItem add(int groupId, int itemId, int order, CharSequence title) {
    final IffabMenuItem menuItem = new IffabMenuItem(this, groupId, itemId, order, title);
    menuItems.add(menuItem);
    return menuItem;
  }

  @Override
  public int addIntentOptions(int groupId, int itemId, int order,
                              ComponentName caller, Intent[] specifics, Intent intent, int flags,
                              MenuItem[] outSpecificItems) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public void removeItem(int id) {
    final IffabMenuItem item = findItem(id);
    if (item != null) {
      menuItems.remove(item);
    }
  }

  @Override
  public void removeGroup(int groupId) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public void setGroupVisible(int group, boolean visible) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public void setGroupEnabled(int group, boolean enabled) {
    throw new RuntimeException("not implemented yet...");
  }

  @NonNull
  private List<IffabMenuItem> findItemByDirection(Direction direction) {
    final ArrayList<IffabMenuItem> res = new ArrayList<>(menuItems.size());
    for (IffabMenuItem item : menuItems) {
      if (item.getDirection() == direction) {
        res.add(item);
      }
    }
    return res;
  }

  boolean isVisibleByDirectin(Direction direction) {
    for (IffabMenuItem item : findItemByDirection(direction)) {
      if (item.isVisible()) {
        return true;
      }
    }
    return false;
  }

  boolean isEnabledByDirection(Direction direction) {
    for (IffabMenuItem item : findItemByDirection(direction)) {
      if (item.isEnabled()) {
        return true;
      }
    }
    return false;
  }

  List<IffabMenuItem> getVisibleItems() {
    final List<IffabMenuItem> res = new ArrayList<>();
    for (IffabMenuItem item : menuItems) {
      if (item.isVisible()) {
        res.add(item);
      }
    }
    return res;
  }

  @Override
  public boolean hasVisibleItems() {
    return getVisibleItems().size() > 0;
  }

  @Override
  public IffabMenuItem findItem(int id) {
    for (IffabMenuItem i : menuItems) {
      if (i.getItemId() == id) {
        return i;
      }
    }
    return null;
  }

  void dispatchMenuItemSelected(Direction direction) {
    if (selectedListener == null) {
      return;
    }
    for (IffabMenuItem item : findItemByDirection(direction)) {
      if (item.isEnabled()) {
        selectedListener.onItemSelected(item);
        return;
      }
    }
  }

  void dispatchUpdatePresenter() {
    final IffabMenuPresenter iffabMenuPresenter = presenter.get();
    if (iffabMenuPresenter != null) {
      iffabMenuPresenter.updateMenu();
    }
  }

  void setOnIffabItemSelectedListener(OnIffabItemSelectedListener selectedListener) {
    this.selectedListener = selectedListener;
  }

  @Override
  public int size() {
    return menuItems.size();
  }

  @Override
  public MenuItem getItem(int index) {
    return menuItems.get(index);
  }

  @Override
  public void clear() {
    menuItems.clear();
  }

  @Override
  public void close() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public boolean performShortcut(int keyCode, KeyEvent event, int flags) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public boolean isShortcutKey(int keyCode, KeyEvent event) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public boolean performIdentifierAction(int id, int flags) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public void setQwertyMode(boolean isQwerty) {
    throw new RuntimeException("not implemented yet...");
  }

  Context getContext() {
    return context;
  }

  @Override
  public SubMenu addSubMenu(CharSequence title) {
    throw new RuntimeException("IndicatableFFAB does not accept sub menu.");
  }

  @Override
  public SubMenu addSubMenu(@StringRes int titleRes) {
    throw new RuntimeException("IndicatableFFAB does not accept sub menu.");
  }

  @Override
  public SubMenu addSubMenu(int groupId, int itemId, int order, CharSequence title) {
    throw new RuntimeException("IndicatableFFAB does not accept sub menu.");
  }

  @Override
  public SubMenu addSubMenu(int groupId, int itemId, int order, @StringRes int titleRes) {
    throw new RuntimeException("IndicatableFFAB does not accept sub menu.");
  }

  @Override
  public void setGroupCheckable(int group, boolean checkable, boolean exclusive) {
    throw new RuntimeException("IffabMenu is not checkable.");
  }
}
