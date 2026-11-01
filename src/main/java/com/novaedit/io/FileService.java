package com.novaedit.io;

import com.novaedit.core.document.Document;
import com.novaedit.core.document.EncodingInfo;
import com.novaedit.core.document.LineEnding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Service for robust reading and writing of documents with encoding preservation
 * and large-file safety checks.
 */
public class FileService {
    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    public static final long MAX_RECOMMENDED_SIZE = 50 * 1024 * 1024; // 50 MB

    public static class FileLoadResult {
        public final Document document;
        public final boolean isBinary;
        public final boolean isLargeFile;

        public FileLoadResult(Document document, boolean isBinary, boolean isLargeFile) {
            this.document = document;
            this.isBinary = isBinary;
            this.isLargeFile = isLargeFile;
        }
    }

    public FileLoadResult loadFile(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("File does not exist: " + path);
        }

        long size = Files.size(path);
        boolean isLarge = size > 20 * 1024 * 1024; // > 20 MB triggers large-file mode
        if (size > MAX_RECOMMENDED_SIZE) {
            log.warn("File {} is very large ({} MB)", path.getFileName(), size / (1024 * 1024));
        }

        byte[] bytes = Files.readAllBytes(path);
        boolean binary = EncodingDetector.isBinary(bytes);

        EncodingInfo encoding = EncodingDetector.detect(bytes);
        int offset = encoding.hasBom() ? (encoding.name().contains("16") ? 2 : 3) : 0;
        String content = new String(bytes, offset, bytes.length - offset, encoding.charset());

        Document doc = new Document(content, path);
        doc.setEncodingInfo(encoding);
        doc.setLineEnding(LineEnding.detect(content));
        doc.setDirty(false);

        return new FileLoadResult(doc, binary, isLarge);
    }

    public void saveFile(Document document, Path targetPath) throws IOException {
        if (document == null || targetPath == null) {
            throw new IllegalArgumentException("Document and path must not be null");
        }

        String text = document.getText();
        LineEnding ending = document.getLineEnding();
        if (ending == LineEnding.CRLF) {
            text = text.replace("\r\n", "\n").replace("\n", "\r\n");
        } else if (ending == LineEnding.LF) {
            text = text.replace("\r\n", "\n");
        }

        EncodingInfo encoding = document.getEncodingInfo();
        byte[] contentBytes = text.getBytes(encoding.charset());

        if (encoding.hasBom()) {
            byte[] bom;
            if (encoding.equals(EncodingInfo.UTF_8_BOM)) {
                bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
            } else if (encoding.equals(EncodingInfo.UTF_16LE)) {
                bom = new byte[]{(byte) 0xFF, (byte) 0xFE};
            } else {
                bom = new byte[]{(byte) 0xFE, (byte) 0xFF};
            }
            byte[] total = new byte[bom.length + contentBytes.length];
            System.arraycopy(bom, 0, total, 0, bom.length);
            System.arraycopy(contentBytes, 0, total, bom.length, contentBytes.length);
            Files.write(targetPath, total);
        } else {
            Files.write(targetPath, contentBytes);
        }

        document.setFilePath(targetPath);
        document.setDirty(false);
        log.info("Saved file {} using {}", targetPath, encoding.name());
    }
}
