package com.textcraft.voice;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Uses the operating system voices through ProcessBuilder: PowerShell (Windows), say (macOS),
 * espeak (Linux). The text goes in through standard input, never in the command line,
 * so quotes in the document cannot break out of the command.
 */
public class SystemTtsService implements TextToSpeechService {
    private static final Logger LOG = Logger.getLogger(SystemTtsService.class.getName());

    private volatile Process process;
    private volatile boolean paused;

    private static String os() {
        return System.getProperty("os.name", "").toLowerCase();
    }

    @Override
    public synchronized void speak(String text, double speed) {
        stop();
        if (text == null || text.trim().isEmpty()) return;
        List<String> cmd = new ArrayList<>();
        double s = Math.max(0.5, Math.min(2.0, speed));
        if (os().contains("win")) {
            int rate = (int) Math.round((s - 1.0) * 10); // SpeechSynthesizer.Rate is -10..10
            cmd.addAll(Arrays.asList("powershell", "-NoProfile", "-Command",
                    "Add-Type -AssemblyName System.Speech; $s = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
                            + "$s.Rate = " + rate + "; $s.Speak([Console]::In.ReadToEnd())"));
        } else if (os().contains("mac")) {
            cmd.addAll(Arrays.asList("say", "-r", String.valueOf((int) (175 * s))));
        } else {
            cmd.addAll(Arrays.asList("espeak", "-s", String.valueOf((int) (160 * s)), "--stdin"));
        }
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            process = p;
            paused = false;
            Thread feeder = new Thread(() -> {
                try (OutputStream out = p.getOutputStream()) {
                    out.write(text.getBytes(StandardCharsets.UTF_8));
                } catch (IOException ignored) {
                    // stopped while writing
                }
            }, "tts-feed");
            feeder.setDaemon(true);
            feeder.start();
            Thread drain = new Thread(() -> {
                try {
                    p.getInputStream().readAllBytes(); // keep the pipe empty so the process can finish
                } catch (IOException ignored) {
                    // ended
                }
            }, "tts-drain");
            drain.setDaemon(true);
            drain.start();
        } catch (IOException e) {
            LOG.warning("Text-to-speech not available: " + e.getMessage());
            throw new IllegalStateException("No system voice found (" + cmd.get(0) + " is not installed).", e);
        }
    }

    @Override
    public synchronized void pause() {
        Process p = process;
        if (p == null || !p.isAlive()) return;
        if (os().contains("win")) { // no portable pause on Windows: stop instead
            stop();
            return;
        }
        try {
            String sig = paused ? "-CONT" : "-STOP";
            new ProcessBuilder("kill", sig, String.valueOf(p.pid())).start().waitFor();
            paused = !paused;
        } catch (IOException e) {
            LOG.warning("Could not pause speech: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public synchronized void stop() {
        Process p = process;
        if (p != null) {
            if (paused) {
                try {
                    new ProcessBuilder("kill", "-CONT", String.valueOf(p.pid())).start().waitFor();
                } catch (IOException | InterruptedException ignored) {
                    // destroyForcibly below still ends it
                }
            }
            p.destroyForcibly();
        }
        process = null;
        paused = false;
    }

    @Override
    public boolean isSpeaking() {
        Process p = process;
        return p != null && p.isAlive();
    }
}
