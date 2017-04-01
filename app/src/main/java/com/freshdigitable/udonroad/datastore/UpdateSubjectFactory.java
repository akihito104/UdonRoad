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

package com.freshdigitable.udonroad.datastore;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by akihit on 2017/03/28.
 */

public class UpdateSubjectFactory {
  private final Map<String, UpdateSubject> subjectTable = new HashMap<>();

  public UpdateSubject getInstance(String name) {
    if (subjectTable.containsKey(name)) {
      final UpdateSubject updateSubject = subjectTable.get(name);
      if (!updateSubject.hasCompleted()) {
        return updateSubject;
      }
      subjectTable.remove(name);
    }
    final UpdateSubject updateSubject = new UpdateSubject(name);
    subjectTable.put(name, updateSubject);
    return updateSubject;
  }

  public void clear() {
    for (String name : subjectTable.keySet()) {
      subjectTable.get(name).onComplete();
    }
    subjectTable.clear();
  }
}