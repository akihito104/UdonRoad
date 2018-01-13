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

package com.freshdigitable.udonroad.timeline.repository;

import com.freshdigitable.udonroad.StoreType;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Created by akihit on 2018/01/13.
 */

public class ListItemRepositoryProvider {
  private final Map<StoreType, Provider<ListItemRepository>> repositories;

  @Inject
  ListItemRepositoryProvider(Map<StoreType, Provider<ListItemRepository>> repositories) {
    this.repositories = repositories;
  }

  public Provider<ListItemRepository> get(StoreType storeType) {
    return repositories.get(storeType);
  }
}
