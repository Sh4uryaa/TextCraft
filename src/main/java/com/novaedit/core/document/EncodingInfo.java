package com.novaedit.core.document;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Character encoding metadata for documents.
 */
public record EncodingInfo(Charset charset, boolean hasBom, String name) {

    public static final EncodingInfo UTF_8 = new EncodingInfo(StandardCharsets.UTF_8, false, "UTF-8");
    public static final EncodingInfo UTF_8_BOM = new EncodingInfo(StandardCharsets.UTF_8, true, "UTF-8 with BOM");
    public static final EncodingInfo UTF_16LE = new EncodingInfo(StandardCharsets.UTF_16LE, false, "UTF-16 LE");
    public static final EncodingInfo UTF_16BE = new EncodingInfo(StandardCharsets.UTF_16BE, false, "UTF-16 BE");
    public static final EncodingInfo ISO_8859_1 = new EncodingInfo(StandardCharsets.ISO_8859_1, false, "ISO-8859-1");
    public static final EncodingInfo WINDOWS_1252 = new EncodingInfo(Charset.forName("windows-1252"), false, "Windows-1252");

    public static EncodingInfo of(Charset charset) {
        if (charset.equals(StandardCharsets.UTF_8)) return UTF_8;
        if (charset.equals(StandardCharsets.UTF_16LE)) return UTF_16LE;
        if (charset.equals(StandardCharsets.UTF_16BE)) return UTF_16BE;
        if (charset.equals(StandardCharsets.ISO_8859_1)) return ISO_8859_1;
        return new EncodingInfo(charset, false, charset.displayName());
    }

    public static EncodingInfo of(String charsetName) {
        try {
            return of(Charset.forName(charsetName));
        } catch (Exception e) {
            return UTF_8;
        }
    }
}
