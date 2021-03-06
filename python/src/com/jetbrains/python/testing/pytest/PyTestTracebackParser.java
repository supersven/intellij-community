/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.jetbrains.python.testing.pytest;

import com.jetbrains.python.traceBackParsers.LinkInTrace;
import com.jetbrains.python.traceBackParsers.TraceBackParser;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User : catherine
 */
public final class PyTestTracebackParser extends TraceBackParser {

  public PyTestTracebackParser() {
    // File name can't start with number, can't be more then 200 chars long (its insane) and line number is also limited to int maxvalue
   super(Pattern.compile("([^0-9][^\"]{0,200}?):(\\d{1,8})"));
  }

  @NotNull
  @Override
  protected LinkInTrace findLinkInTrace(@NotNull final String line, @NotNull final Matcher matchedMatcher) {
    final String fileName = matchedMatcher.group(1).replace('\\', '/');
    final int lineNumber = Integer.parseInt(matchedMatcher.group(2));
    final int startPos = matchedMatcher.start(1);
    final int endPos = matchedMatcher.end(2);
    return new LinkInTrace(fileName, lineNumber, startPos, endPos);
  }
}
