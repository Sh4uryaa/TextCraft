package com.novaedit.features.diff;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Service for computing diffs and side-by-side file comparisons using java-diff-utils.
 */
public class DiffService {

    public enum ChangeType {
        INSERT, DELETE, CHANGE, EQUAL
    }

    public record DiffLine(ChangeType type, int originalLineNumber, int revisedLineNumber, String text) {}

    public Patch<String> computePatch(String originalText, String revisedText) {
        List<String> originalLines = toLines(originalText);
        List<String> revisedLines = toLines(revisedText);
        return DiffUtils.diff(originalLines, revisedLines);
    }

    public List<AbstractDelta<String>> getDeltas(String originalText, String revisedText) {
        Patch<String> patch = computePatch(originalText, revisedText);
        return patch.getDeltas();
    }

    private List<String> toLines(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(text.split("\n", -1));
    }
}
