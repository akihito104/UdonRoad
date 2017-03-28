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

package com.freshdigitable.udonroad.datastore;

/**
 * BaseCache defines basic operation for name-defined data store.
 *
 * Created by akihit on 2016/09/14.
 */
public interface BaseCache {
  /**
   * opens data store resource. Depended data stores are also opened.
   */
  void open();

  /**
   * deletes all stored entities. This operation should be called when the data store is opened.
   */
  void clear();

  /**
   * closes data store resources. Depended data stores are also closed.
   */
  void close();

  /**
   * removes the data store resource. This operation should be called when the data store is closed.
   */
  void drop();
}
