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

import rx.Observable;
import rx.subjects.PublishSubject;

/**
 * Created by akihit on 2017/03/28.
 */

public class UpdateSubject {
  private final PublishSubject<UpdateEvent> eventPublishSubject;
  private final String name;

  UpdateSubject(String name) {
    this.name = name;
    eventPublishSubject = PublishSubject.create();
  }

  public Observable<UpdateEvent> observeUpdateEvent() {
    return eventPublishSubject.onBackpressureBuffer();
  }

  public void onComplete() {
    if (!eventPublishSubject.hasCompleted()) {
      eventPublishSubject.onCompleted();
    }
  }

  public boolean hasCompleted() {
    return eventPublishSubject.hasCompleted();
  }

  public boolean hasObservers() {
    return eventPublishSubject.hasObservers();
  }

  public void onNext(UpdateEvent.EventType type, int index) {
    onNext(new UpdateEvent(type, index));
  }

  public void onNext(UpdateEvent updateEvent) {
    eventPublishSubject.onNext(updateEvent);
  }
}
