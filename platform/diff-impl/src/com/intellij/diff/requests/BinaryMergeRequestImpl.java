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
package com.intellij.diff.requests;

import com.intellij.CommonBundle;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.contents.FileContent;
import com.intellij.diff.merge.ThreesideMergeRequest;
import com.intellij.diff.merge.MergeResult;
import com.intellij.diff.util.ThreeSide;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

public class BinaryMergeRequestImpl extends ThreesideMergeRequest {
  private static final Logger LOG = Logger.getInstance(BinaryMergeRequestImpl.class);

  @NotNull private final FileContent myFile;
  @NotNull private final List<DiffContent> myContents;

  @NotNull private final List<byte[]> myByteContents;
  @NotNull private final byte[] myOriginalContent;

  @Nullable private final String myTitle;
  @NotNull private final List<String> myTitles;

  @Nullable private final Consumer<MergeResult> myApplyCallback;

  public BinaryMergeRequestImpl(@NotNull FileContent file,
                                @NotNull byte[] originalContent,
                                @NotNull List<DiffContent> contents,
                                @NotNull List<byte[]> byteContents,
                                @Nullable String title,
                                @NotNull List<String> contentTitles,
                                @Nullable Consumer<MergeResult> applyCallback) {
    assert byteContents.size() == 3;
    assert contents.size() == 3;
    assert contentTitles.size() == 3;

    myFile = file;
    myOriginalContent = originalContent;

    myByteContents = byteContents;
    myContents = contents;
    myTitle = title;
    myTitles = contentTitles;

    myApplyCallback = applyCallback;
  }

  @NotNull
  @Override
  public FileContent getOutputContent() {
    return myFile;
  }

  @NotNull
  @Override
  public List<DiffContent> getContents() {
    return myContents;
  }

  @Nullable
  @Override
  public String getTitle() {
    return myTitle;
  }

  @NotNull
  @Override
  public List<String> getContentTitles() {
    return myTitles;
  }

  @Override
  public void applyResult(@NotNull MergeResult result) {
    final byte[] applyContent;
    switch (result) {
      case CANCEL:
        applyContent = myOriginalContent;
        break;
      case LEFT:
        applyContent = ThreeSide.LEFT.select(myByteContents);
        break;
      case RIGHT:
        applyContent = ThreeSide.RIGHT.select(myByteContents);
        break;
      case RESOLVED:
        applyContent = null;
        break;
      default:
        throw new IllegalArgumentException(result.toString());
    }

    if (applyContent != null) {
      new WriteCommandAction.Simple(null) {
        @Override
        protected void run() throws Throwable {
          try {
            myFile.getFile().setBinaryContent(applyContent);
          }
          catch (IOException e) {
            LOG.error(e);
            Messages.showErrorDialog((Project)null, "Can't apply result", CommonBundle.getErrorTitle());
          }
        }
      }.execute();
    }

    if (myApplyCallback != null) myApplyCallback.consume(result);
  }
}
