package com.novaedit.io;

import com.novaedit.core.document.EncodingInfo;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Detects file encoding using juniversalchardet.
 */
public class EncodingDetector {

    public static EncodingInfo detect(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return EncodingInfo.UTF_8;
        }

        // Check for BOMs first
        if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            return EncodingInfo.UTF_8_BOM;
        }
        if (bytes.length >= 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
            return EncodingInfo.UTF_16LE;
        }
        if (bytes.length >= 2 && bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
            return EncodingInfo.UTF_16BE;
        }

        UniversalDetector detector = new UniversalDetector(null);
        detector.handleData(bytes, 0, bytes.length);
        detector.dataEnd();

        String encoding = detector.getDetectedCharset();
        detector.reset();

        if (encoding != null) {
            try {
                return EncodingInfo.of(Charset.forName(encoding));
            } catch (Exception ignored) {}
        }

        // Default to UTF-8
        return EncodingInfo.UTF_8;
    }

    public static boolean isBinary(byte[] bytes) {
        if (bytes == null) return false;
        int checkLen = Math.min(bytes.length, 8000);
        int nullCount = 0;
        for (int i = 0; i < checkLen; i++) {
            if (bytes[i] == 0) {
                nullCount++;
                if (nullCount > 2) {
                    return true;
                }
            }
        }
        return false;
    }
}
