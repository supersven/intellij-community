/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.application;

import com.intellij.openapi.wm.IdeFrame;
import com.intellij.util.messages.Topic;

/**
 * @author yole
 */
public interface ApplicationActivationListener {
  Topic<ApplicationActivationListener> TOPIC = Topic.create("Application activation notifications", ApplicationActivationListener.class);

  /**
   * Called when app is activated by transferring focus to it.
   */
  void applicationActivated(IdeFrame ideFrame);

  /**
   * Called when app is de-activated by transferring focus from it.
   */
  void applicationDeactivated(IdeFrame ideFrame);

  abstract class Adapter implements ApplicationActivationListener {
    @Override
    public void applicationActivated(IdeFrame ideFrame) { }

    @Override
    public void applicationDeactivated(IdeFrame ideFrame) { }
  }
}
