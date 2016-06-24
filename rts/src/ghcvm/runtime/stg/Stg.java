package ghcvm.runtime.stg;

import java.util.ListIterator;

import ghcvm.runtime.exception.StgException;
import static ghcvm.runtime.stg.StgTSO.WhatNext.ThreadRunGHC;
import static ghcvm.runtime.stg.StgTSO.WhatNext.ThreadKilled;
import static ghcvm.runtime.stg.StgContext.ReturnCode.ThreadBlocked;
import static ghcvm.runtime.stg.StgContext.ReturnCode.ThreadFinished;

public class Stg {
    public static StgClosure mkWeak = new StgClosure() {
            @Override
            public void enter(StgContext context) {
                Capability cap = context.myCapability;
                StgClosure key = context.R(2);
                StgClosure value = context.R(3);
                StgClosure finalizer = context.R(4);
                StgWeak weak = new StgWeak(key, value, finalizer);
                cap.weakPtrList.add(weak);
                context.R(1, weak);
            }
        };

    public static StgClosure returnToSched = new StgClosure() {
            @Override
            public void enter(StgContext context) {
                Capability cap = context.myCapability;
                StgTSO tso = context.currentTSO;
                cap.threadPaused(tso);
                throw StgException.stgReturnException;
            }
        };

    public static StgClosure returnToSchedButFirst = new StgClosure() {
            @Override
            public void enter(StgContext context) {
                Capability cap = context.myCapability;
                StgTSO tso = context.currentTSO;
                cap.threadPaused(tso);
                StgClosure first = context.R(2);
                first.enter(context);
            }
        };

    public static StgClosure noDuplicate = new StgClosure() {
            @Override
            public void enter(StgContext context) {
                if (Capability.nCapabilities != 1) {
                    Capability cap = context.myCapability;
                    StgTSO tso = context.currentTSO;
                    ListIterator<StackFrame> sp = tso.sp;
                    sp.add(new NoDuplicateFrame());
                    cap.threadPaused(tso);
                    if (tso.whatNext == ThreadKilled) {
                        threadFinished.enter(context);
                    } else {
                        StackFrame top = tso.stack.peek();
                        if (top.getClass() == NoDuplicateFrame.class) {
                            sp.previous();
                            sp.remove();
                        }
                        throw StgException.stackReloadException;
                    }
                }
            }
        };

    public static StgClosure threadFinished = new StgClosure() {
            @Override
            public void enter(StgContext context) {
                context.ret = ThreadFinished;
                throw StgException.stgReturnException;
            }
        };

    public static StgClosure block_noregs = new StgClosure() {
            @Override
            public void enter(StgContext context) {
                StgTSO tso = context.currentTSO;
                tso.whatNext = ThreadRunGHC;
                context.ret = ThreadBlocked;
                returnToSched.enter(context);
            }
        };
}