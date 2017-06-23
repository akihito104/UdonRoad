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

import io.reactivex.Flowable;
import io.reactivex.processors.PublishProcessor;

/**
 * Created by akihit on 2017/03/28.
 */

public class UpdateSubject {
  private final PublishProcessor<UpdateEvent> eventPublishProcessor;
  private final String name;

  UpdateSubject(String name) {
    this.name = name;
    eventPublishProcessor = PublishProcessor.create();
  }

  public Flowable<UpdateEvent> observeUpdateEvent() {
    return eventPublishProcessor.onBackpressureBuffer();
  }

  public void onComplete() {
    if (!eventPublishProcessor.hasComplete()) {
      eventPublishProcessor.onComplete();
    }
  }

  public boolean hasCompleted() {
    return eventPublishProcessor.hasComplete();
  }

  public boolean hasSubscribers() {
    return eventPublishProcessor.hasSubscribers();
  }

  public void onNext(UpdateEvent.EventType type, int index) {
    onNext(type, index, 1);
  }

  public void onNext(UpdateEvent.EventType type, int index, int length) {
    onNext(new UpdateEvent(type, index, length));
  }

  public void onNext(UpdateEvent updateEvent) {
    eventPublishProcessor.onNext(updateEvent);
  }
}
