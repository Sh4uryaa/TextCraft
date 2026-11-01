package com.novaedit.features.format;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FormatTest {

    private final IndentService indentService = new IndentService();
    private final BracketMatcher bracketMatcher = new BracketMatcher();
    private final CommentToggler commentToggler = new CommentToggler();

    @Test
    @DisplayName("Indent computation after open brace increases indentation")
    void testIndentAfterBrace() {
        String prev = "    public void run() {";
        String indent = indentService.computeNewLineIndent(prev, 4, true);
        assertThat(indent).isEqualTo("        ");
    }

    @Test
    @DisplayName("Block indent and outdent operations")
    void testBlockIndentOutdent() {
        String code = "line 1\nline 2";
        String indented = indentService.indentBlock(code, 4, true);
        assertThat(indented).isEqualTo("    line 1\n    line 2");

        String outdented = indentService.outdentBlock(indented, 4, true);
        assertThat(outdented).isEqualTo("line 1\nline 2");
    }

    @Test
    @DisplayName("BracketMatcher finds matching parenthesis, curly braces, and brackets")
    void testBracketMatcher() {
        String code = "if ((x > 0) && (y < [10])) { return true; }";

        // Matching for the outer '(' at index 3
        int matchParen = bracketMatcher.findMatchingBracket(code, 4);
        assertThat(matchParen).isEqualTo(25);

        // Matching for '{' at index 27
        int matchBrace = bracketMatcher.findMatchingBracket(code, 28);
        assertThat(matchBrace).isEqualTo(42);

        // Matching for '[' at index 20
        int matchSquare = bracketMatcher.findMatchingBracket(code, 21);
        assertThat(matchSquare).isEqualTo(23);
    }

    @Test
    @DisplayName("CommentToggler comments and uncomments lines")
    void testCommentToggler() {
        String code = "int a = 1;\nint b = 2;";
        String commented = commentToggler.toggleLineComment(code, "//");
        assertThat(commented).isEqualTo("// int a = 1;\n// int b = 2;");

        String uncommented = commentToggler.toggleLineComment(commented, "//");
        assertThat(uncommented).isEqualTo("int a = 1;\nint b = 2;");
    }

    @Test
    @DisplayName("Sort lines and trim trailing whitespace")
    void testSortAndTrim() {
        String lines = "banana\nApple\ncherry";
        String sorted = commentToggler.sortLines(lines, true);
        assertThat(sorted).isEqualTo("Apple\nbanana\ncherry");

        String untrimmed = "hello   \nworld\t\t";
        String trimmed = commentToggler.trimTrailingWhitespace(untrimmed);
        assertThat(trimmed).isEqualTo("hello\nworld");
    }
}
