package com.textcraft.compiler;

import com.textcraft.exception.CompilerException;
import com.textcraft.model.CompileResult;

/** OOP: Interface (Strategy) - run code remotely (Judge0) or locally. */
public interface CodeRunner {
    CompileResult run(Language l, String code, String stdin) throws CompilerException;

    /** Stop a run that is in progress (called from the UI thread). */
    void cancel();
}
