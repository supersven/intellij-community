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

/*
 * Class LocalVariableEvaluator
 * @author Jeka
 */
package com.intellij.debugger.engine.evaluation.expression;

import com.intellij.debugger.DebuggerBundle;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl;
import com.intellij.debugger.engine.jdi.StackFrameProxy;
import com.intellij.debugger.impl.PositionUtil;
import com.intellij.debugger.impl.SimpleStackFrameContext;
import com.intellij.debugger.jdi.LocalVariableProxyImpl;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl;
import com.intellij.debugger.ui.impl.watch.LocalVariableDescriptorImpl;
import com.intellij.debugger.ui.impl.watch.NodeDescriptorImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiVariable;
import com.sun.jdi.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class LocalVariableEvaluator implements Evaluator {
  private static final Logger LOG = Logger.getInstance("#com.intellij.debugger.engine.evaluation.expression.LocalVariableEvaluator");

  private final String myLocalVariableName;
  private EvaluationContextImpl myContext;
  private LocalVariableProxyImpl myEvaluatedVariable;
  private final boolean myCanScanFrames;
  private int myParameterIndex = -1;

  public LocalVariableEvaluator(String localVariableName, boolean canScanFrames) {
    myLocalVariableName = localVariableName;
    myCanScanFrames = canScanFrames;
  }

  public void setParameterIndex(int parameterIndex) {
    myParameterIndex = parameterIndex;
  }

  @Override
  public Object evaluate(EvaluationContextImpl context) throws EvaluateException {
    StackFrameProxyImpl frameProxy = context.getFrameProxy();
    if (frameProxy == null) {
      throw EvaluateExceptionUtil.createEvaluateException(DebuggerBundle.message("evaluation.error.no.stackframe"));
    }

    try {
      ThreadReferenceProxyImpl threadProxy = null;
      int lastFrameIndex = -1;
      PsiVariable variable = null;

      boolean topFrame = true;

      while (true) {
        try {
          LocalVariableProxyImpl local = frameProxy.visibleVariableByName(myLocalVariableName);
          if (local != null) {
            if (topFrame ||
                variable.equals(resolveVariable(frameProxy, myLocalVariableName, context.getProject(), context.getDebugProcess()))) {
              myEvaluatedVariable = local;
              myContext = context;
              return frameProxy.getValue(local);
            }
          }
        }
        catch (EvaluateException e) {
          if (!(e.getCause() instanceof AbsentInformationException)) {
            throw e;
          }
          if (topFrame) {
            if (myParameterIndex < 0) {
              throw e;
            }
            final List<Value> values = frameProxy.getArgumentValues();
            if (values.isEmpty() || myParameterIndex >= values.size()) {
              throw e;
            }
            return values.get(myParameterIndex);
          }
        }

        if (myCanScanFrames) {
          if (topFrame) {
            variable = resolveVariable(frameProxy, myLocalVariableName, context.getProject(), context.getDebugProcess());
            if (variable == null) break;
          }
          if (threadProxy == null /* initialize it lazily */) {
            threadProxy = frameProxy.threadProxy();
            lastFrameIndex = threadProxy.frameCount() - 1;
          }
          int currentFrameIndex = frameProxy.getFrameIndex();
          if (currentFrameIndex < lastFrameIndex) {
            frameProxy = threadProxy.frame(currentFrameIndex + 1);
            if (frameProxy != null) {
              topFrame = false;
              continue;
            }
          }
        }

        break;
      }
      throw EvaluateExceptionUtil.createEvaluateException(DebuggerBundle.message("evaluation.error.local.variable.missing", myLocalVariableName));
    }
    catch (EvaluateException e) {
      myEvaluatedVariable = null;
      myContext = null;
      throw e;
    }
  }

  @Override
  public Modifier getModifier() {
    Modifier modifier = null;
    if (myEvaluatedVariable != null && myContext != null) {
      modifier = new Modifier() {
        @Override
        public boolean canInspect() {
          return true;
        }

        @Override
        public boolean canSetValue() {
          return true;
        }

        @Override
        public void setValue(Value value) throws ClassNotLoadedException, InvalidTypeException {
          StackFrameProxyImpl frameProxy = myContext.getFrameProxy();
          try {
            assert frameProxy != null;
            frameProxy.setValue(myEvaluatedVariable, value);
          }
          catch (EvaluateException e) {
            LOG.error(e);
          }
        }

        @Override
        public Type getExpectedType() throws ClassNotLoadedException {
          try {
            return myEvaluatedVariable.getType();
          }
          catch (EvaluateException e) {
            LOG.error(e);
            return null;
          }
        }

        @Override
        public NodeDescriptorImpl getInspectItem(Project project) {
          return new LocalVariableDescriptorImpl(project, myEvaluatedVariable);
        }
      };
    }
    return modifier;
  }

  @Nullable
  private static PsiVariable resolveVariable(final StackFrameProxy frame,
                                             final String name,
                                             final Project project,
                                             final DebugProcess process) {
    return ApplicationManager.getApplication().runReadAction(new Computable<PsiVariable>() {
      @Override
      public PsiVariable compute() {
        PsiElement place = PositionUtil.getContextElement(new SimpleStackFrameContext(frame, process));
        if (place == null) return null;
        return JavaPsiFacade.getInstance(project).getResolveHelper().resolveReferencedVariable(name, place);
      }
    });
  }

  @Override
  public String toString() {
    return myLocalVariableName;
  }
}
