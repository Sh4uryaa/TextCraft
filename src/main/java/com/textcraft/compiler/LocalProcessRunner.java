package com.textcraft.compiler;

import com.textcraft.exception.CompilerException;
import com.textcraft.model.CompileResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Optional runner that uses python / g++ / javac installed on this PC (no internet needed). */
public class LocalProcessRunner implements CodeRunner {
    private static final long TIMEOUT_SECONDS = 10;

    private volatile Process current;
    private volatile boolean cancelled;

    /** True when the language's tool can be started. */
    public static boolean isAvailable(Language l) {
        String cmd = l == Language.PYTHON && isWindows() ? "python" : l.localCommand();
        try {
            Process p = new ProcessBuilder(cmd, l == Language.JAVA ? "-version" : "--version")
                    .redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            return p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    @Override
    public void cancel() {
        cancelled = true;
        Process p = current;
        if (p != null) p.destroyForcibly();
    }

    @Override
    public CompileResult run(Language lang, String code, String stdin) throws CompilerException {
        cancelled = false;
        Path dir = null;
        long start = System.currentTimeMillis();
        try {
            dir = Files.createTempDirectory("textcraft-run");
            CompileResult r = new CompileResult();
            List<String> runCmd;
            switch (lang) {
                case PYTHON:
                    Files.write(dir.resolve("main.py"), code.getBytes(StandardCharsets.UTF_8));
                    runCmd = Arrays.asList(isWindows() ? "python" : "python3", "main.py");
                    break;
                case CPP:
                    Files.write(dir.resolve("main.cpp"), code.getBytes(StandardCharsets.UTF_8));
                    CompileResult c = exec(dir, Arrays.asList("g++", "-O1", "-o", "prog", "main.cpp"), "");
                    if (!c.isSuccess()) return compileFailed(c, start);
                    runCmd = Arrays.asList(dir.resolve(isWindows() ? "prog.exe" : "prog").toString());
                    break;
                case JAVA:
                    String cls = publicClassName(code);
                    Files.write(dir.resolve(cls + ".java"), code.getBytes(StandardCharsets.UTF_8));
                    CompileResult j = exec(dir, Arrays.asList("javac", cls + ".java"), "");
                    if (!j.isSuccess()) return compileFailed(j, start);
                    runCmd = Arrays.asList("java", "-cp", ".", cls);
                    break;
                default:
                    throw new CompilerException("Unsupported language " + lang);
            }
            r = exec(dir, runCmd, stdin == null ? "" : stdin);
            r.setTimeMs(System.currentTimeMillis() - start);
            return r;
        } catch (IOException e) {
            throw new CompilerException("Could not run " + lang.label() + " on this PC: " + e.getMessage()
                    + " (is it installed and on the PATH?)", e);
        } finally {
            if (dir != null) deleteQuietly(dir);
        }
    }

    private static CompileResult compileFailed(CompileResult c, long start) {
        CompileResult r = new CompileResult();
        r.setCompileOutput(c.getStderr().isEmpty() ? c.getStdout() : c.getStderr());
        r.setStatus("Compilation Error");
        r.setSuccess(false);
        r.setTimeMs(System.currentTimeMillis() - start);
        return r;
    }

    /** Runs one process with a timeout; stdout/stderr are read on helper threads so big outputs cannot block it. */
    private CompileResult exec(Path dir, List<String> cmd, String stdin) throws IOException, CompilerException {
        Process p = new ProcessBuilder(new ArrayList<>(cmd)).directory(dir.toFile()).start();
        current = p;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        Thread t1 = pump(p.getInputStream(), out);
        Thread t2 = pump(p.getErrorStream(), err);
        try (OutputStream in = p.getOutputStream()) {
            in.write(stdin.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            // the program may exit without reading its input
        }
        try {
            boolean done = p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!done) {
                p.destroyForcibly();
                throw new CompilerException("Run timed out after " + TIMEOUT_SECONDS + " seconds.");
            }
            t1.join(1000);
            t2.join(1000);
            if (cancelled) throw new CompilerException("Run stopped.");
        } catch (InterruptedException e) {
            p.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new CompilerException("Run was interrupted.", e);
        } finally {
            current = null;
        }
        CompileResult r = new CompileResult();
        r.setStdout(new String(out.toByteArray(), StandardCharsets.UTF_8));
        r.setStderr(new String(err.toByteArray(), StandardCharsets.UTF_8));
        r.setSuccess(p.exitValue() == 0);
        r.setStatus(p.exitValue() == 0 ? "Accepted" : "Exit code " + p.exitValue());
        return r;
    }

    private static Thread pump(InputStream in, ByteArrayOutputStream sink) {
        Thread t = new Thread(() -> {
            byte[] buf = new byte[4096];
            try {
                int n;
                while ((n = in.read(buf)) > 0) {
                    synchronized (sink) {
                        if (sink.size() < 1_000_000) sink.write(buf, 0, n); // cap output at 1 MB
                    }
                }
            } catch (IOException ignored) {
                // process ended
            }
        }, "run-output");
        t.setDaemon(true);
        t.start();
        return t;
    }

    /** Java needs the file name to match the public class; default to Main. */
    static String publicClassName(String code) {
        Matcher m = Pattern.compile("public\\s+(?:final\\s+)?class\\s+(\\w+)").matcher(code);
        return m.find() ? m.group(1) : "Main";
    }

    private static void deleteQuietly(Path dir) {
        try (java.util.stream.Stream<Path> s = Files.walk(dir)) {
            s.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
            // temp files; the OS will clean them up eventually
        }
    }
}
