package com.textcraft.model;

/** Result of one code run (stdout, stderr, compile errors, status, time). */
public class CompileResult {
    private String stdout = "";
    private String stderr = "";
    private String compileOutput = "";
    private String status = "";
    private boolean success;
    private long timeMs;

    public String getStdout() { return stdout; }
    public void setStdout(String stdout) { this.stdout = stdout == null ? "" : stdout; }
    public String getStderr() { return stderr; }
    public void setStderr(String stderr) { this.stderr = stderr == null ? "" : stderr; }
    public String getCompileOutput() { return compileOutput; }
    public void setCompileOutput(String compileOutput) { this.compileOutput = compileOutput == null ? "" : compileOutput; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public long getTimeMs() { return timeMs; }
    public void setTimeMs(long timeMs) { this.timeMs = timeMs; }

    /** All text a user would want to see in the history log. */
    public String combinedOutput() {
        StringBuilder sb = new StringBuilder();
        if (!compileOutput.isEmpty()) sb.append(compileOutput).append('\n');
        sb.append(stdout);
        if (!stderr.isEmpty()) sb.append('\n').append(stderr);
        return sb.toString();
    }
}
