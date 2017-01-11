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
import android.content.Intent;
import android.support.annotation.StringRes;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by akihit on 2017/01/11.
 */

class IffabMenu implements Menu {
  private final List<MenuItem> menuItems = new ArrayList<>();

  @Override
  public MenuItem add(CharSequence title) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public MenuItem add(@StringRes int titleRes) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public MenuItem add(int groupId, int itemId, int order, CharSequence title) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public MenuItem add(int groupId, int itemId, int order, @StringRes int titleRes) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public int addIntentOptions(int groupId, int itemId, int order, ComponentName caller, Intent[] specifics, Intent intent, int flags, MenuItem[] outSpecificItems) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public void removeItem(int id) {
    final MenuItem item = findItem(id);
    if (item != null) {
      menuItems.remove(item);
    }
  }

  @Override
  public void removeGroup(int groupId) {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public void setGroupCheckable(int group, boolean checkable, boolean exclusive) {
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

  @Override
  public boolean hasVisibleItems() {
    throw new RuntimeException("not implemented yet...");
  }

  @Override
  public MenuItem findItem(int id) {
    for (MenuItem i : menuItems) {
      if (i.getItemId() == id) {
        return i;
      }
    }
    return null;
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
}
