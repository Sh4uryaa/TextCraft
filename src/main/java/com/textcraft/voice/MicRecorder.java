package com.textcraft.voice;

import com.textcraft.exception.VoiceException;
import java.io.ByteArrayOutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

/** Push-to-talk recorder: 16 kHz, 16-bit, mono, signed, little-endian. Stops at 60 s or on stop(). */
public class MicRecorder {
    public static final int MAX_SECONDS = 60;
    private static final AudioFormat FORMAT = new AudioFormat(16000f, 16, 1, true, false);

    private volatile boolean stopRequested;

    public void stop() {
        stopRequested = true;
    }

    /** Blocks until stop() is called or 60 s pass; call it from a worker thread. */
    public byte[] record() throws VoiceException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
        if (!AudioSystem.isLineSupported(info)) throw new VoiceException("No microphone was found on this PC.");
        stopRequested = false;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info)) {
            line.open(FORMAT);
            line.start();
            byte[] buf = new byte[4096];
            long maxBytes = (long) MAX_SECONDS * 16000 * 2;
            while (!stopRequested && out.size() < maxBytes) {
                int n = line.read(buf, 0, buf.length);
                if (n > 0) out.write(buf, 0, n);
            }
            line.stop();
        } catch (LineUnavailableException | IllegalArgumentException | SecurityException e) {
            throw new VoiceException("Cannot use the microphone: " + e.getMessage(), e);
        }
        return out.toByteArray();
    }

    /** Length of a recording in seconds (rounded up). */
    public static int seconds(byte[] pcm) {
        return (int) Math.ceil(pcm.length / (16000.0 * 2));
    }
}
