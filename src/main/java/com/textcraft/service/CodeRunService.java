package com.textcraft.service;

import com.textcraft.app.AppConfig;
import com.textcraft.auth.SessionManager;
import com.textcraft.compiler.CodeRunner;
import com.textcraft.compiler.Judge0Runner;
import com.textcraft.compiler.Language;
import com.textcraft.compiler.LocalProcessRunner;
import com.textcraft.dao.CompileHistoryDAO;
import com.textcraft.db.DatabaseConnection;
import com.textcraft.exception.CompilerException;
import com.textcraft.exception.DatabaseException;
import com.textcraft.model.CompileResult;
import com.textcraft.model.User;
import java.util.logging.Logger;

/**
 * OOP: Facade + Strategy - code.runner=judge0 (default), local, or auto
 * (Judge0 first, then the local tools when Judge0 cannot be reached). Run on a worker thread.
 */
public class CodeRunService {
    private static final Logger LOG = Logger.getLogger(CodeRunService.class.getName());

    private final CodeRunner judge0;
    private final CodeRunner local = new LocalProcessRunner();
    private volatile CodeRunner active;

    public CodeRunService() {
        AppConfig cfg = AppConfig.get();
        this.judge0 = new Judge0Runner(cfg.get("judge0.url", "https://ce.judge0.com"), cfg.get("judge0.authToken", ""));
    }

    public CompileResult run(Language lang, String code, String stdin) throws CompilerException {
        String mode = AppConfig.get().get("code.runner", "auto").toLowerCase();
        CompileResult result;
        if ("local".equals(mode)) {
            result = runWith(local, lang, code, stdin);
        } else if ("judge0".equals(mode)) {
            result = runWith(judge0, lang, code, stdin);
        } else {
            try {
                result = runWith(judge0, lang, code, stdin);
            } catch (CompilerException e) {
                if (!isReachabilityProblem(e) || !LocalProcessRunner.isAvailable(lang)) throw e;
                LOG.info("Judge0 not reachable, using local " + lang.label());
                result = runWith(local, lang, code, stdin);
            }
        }
        log(lang, code, stdin, result);
        return result;
    }

    private static boolean isReachabilityProblem(CompilerException e) {
        String m = String.valueOf(e.getMessage()).toLowerCase();
        return m.contains("offline") || m.contains("could not reach") || m.contains("busy");
    }

    private CompileResult runWith(CodeRunner r, Language lang, String code, String stdin) throws CompilerException {
        active = r;
        try {
            return r.run(lang, code, stdin);
        } finally {
            active = null;
        }
    }

    public void cancel() {
        CodeRunner r = active;
        if (r != null) r.cancel();
    }

    private void log(Language lang, String code, String stdin, CompileResult r) {
        User u = SessionManager.getInstance().getUser();
        if (u == null || !SessionManager.getInstance().isDatabaseAvailable()) return;
        try {
            new CompileHistoryDAO(DatabaseConnection.getInstance()).save(new CompileHistoryDAO.Entry(
                    u.getId(), lang.label(), code, stdin, r.combinedOutput(), r.getStatus(), r.getTimeMs()));
        } catch (DatabaseException e) {
            LOG.warning("Could not log the run: " + e.getMessage());
        }
    }
}
