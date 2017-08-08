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

package com.freshdigitable.udonroad;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.freshdigitable.udonroad.databinding.ActivitySearchBinding;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;

import java.util.ArrayList;
import java.util.List;

import static com.freshdigitable.udonroad.StoreType.SEARCH;

/**
 * Created by akihit on 2017/08/08.
 */

public class SearchActivity extends AppCompatActivity implements FabHandleable {
  private ActivitySearchBinding binding;
  private TimelineFragment tlFragment;
  private TimelineContainerSwitcher timelineContainerSwitcher;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = DataBindingUtil.setContentView(this, R.layout.activity_search);
    setSupportActionBar(binding.searchToolbar);
    getSupportActionBar().setTitle(getQueryWord());
    setupHomeTimeline();
    timelineContainerSwitcher = new TimelineContainerSwitcher(binding.searchTimelineContainer, tlFragment, binding.ffab);
  }

  private String getQueryWord() {
    return getIntent().getStringExtra("query_word");
  }

  private void setupHomeTimeline() {
    tlFragment = TimelineFragment.getInstance(SEARCH, getQueryWord());
    getSupportFragmentManager().beginTransaction()
        .replace(R.id.search_timeline_container, tlFragment)
        .commit();
  }

  public static void start(Context context, String word) {
    final Intent intent = new Intent(context, SearchActivity.class);
    intent.putExtra("query_word", word);
    context.startActivity(intent);
  }

  @Override
  public void showFab() {
    binding.ffab.show();
  }

  @Override
  public void hideFab() {
    binding.ffab.hide();
  }

  @Override
  public void setCheckedFabMenuItem(int itemId, boolean checked) {
    binding.ffab.getMenu().findItem(itemId).setChecked(checked);
  }

  private final List<OnIffabItemSelectedListener> iffabItemSelectedListeners = new ArrayList<>();
  @Override
  public void addOnItemSelectedListener(OnIffabItemSelectedListener listener) {
    iffabItemSelectedListeners.add(listener);
  }

  @Override
  public void removeOnItemSelectedListener(OnIffabItemSelectedListener listener) {
    iffabItemSelectedListeners.remove(listener);
  }
}
