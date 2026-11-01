package com.novaedit.syntax;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LexerTest {

    @Test
    @DisplayName("JavaLexer identifies keywords, strings, comments, numbers, types")
    void testJavaLexer() {
        JavaLexer lexer = new JavaLexer();
        String code = "public class App {\n" +
                      "    // main method\n" +
                      "    public static void main(String[] args) {\n" +
                      "        int count = 42;\n" +
                      "        System.out.println(\"Hello\");\n" +
                      "    }\n" +
                      "}";

        List<Token> tokens = lexer.tokenize(code);
        assertThat(tokens).isNotEmpty();

        assertThat(tokens).anyMatch(t -> t.type() == TokenType.KEYWORD);
        assertThat(tokens).anyMatch(t -> t.type() == TokenType.COMMENT);
        assertThat(tokens).anyMatch(t -> t.type() == TokenType.TYPE);
        assertThat(tokens).anyMatch(t -> t.type() == TokenType.NUMBER);
        assertThat(tokens).anyMatch(t -> t.type() == TokenType.STRING);
    }

    @Test
    @DisplayName("JsonLexer tokenizes keys, strings, numbers, keywords")
    void testJsonLexer() {
        JsonLexer lexer = new JsonLexer();
        String json = "{\n  \"name\": \"NovaEdit\",\n  \"version\": 1,\n  \"active\": true\n}";

        List<Token> tokens = lexer.tokenize(json);
        assertThat(tokens).isNotEmpty();
        assertThat(tokens).anyMatch(t -> t.type() == TokenType.ATTRIBUTE); // key
        assertThat(tokens).anyMatch(t -> t.type() == TokenType.STRING);
        assertThat(tokens).anyMatch(t -> t.type() == TokenType.NUMBER);
        assertThat(tokens).anyMatch(t -> t.type() == TokenType.KEYWORD);
    }

    @Test
    @DisplayName("XmlLexer tokenizes tags, attributes, strings, comments")
    void testXmlLexer() {
        XmlLexer lexer = new XmlLexer();
        String xml = "<!-- config -->\n<project version=\"1.0\">\n    <name>NovaEdit</name>\n</project>";

        List<Token> tokens = lexer.tokenize(xml);
        assertThat(tokens).isNotEmpty();
        assertThat(tokens).anyMatch(t -> t.type() == TokenType.COMMENT);
        assertThat(tokens).anyMatch(t -> t.type() == TokenType.TAG);
        assertThat(tokens).anyMatch(t -> t.type() == TokenType.ATTRIBUTE);
        assertThat(tokens).anyMatch(t -> t.type() == TokenType.STRING);
    }

    @Test
    @DisplayName("MarkdownLexer tokenizes headings, bold, italic, links")
    void testMarkdownLexer() {
        MarkdownLexer lexer = new MarkdownLexer();
        String md = "# NovaEdit\n**Bold text** and *italic* and [link](https://example.com)\n> quote";

        List<Token> tokens = lexer.tokenize(md);
        assertThat(tokens).isNotEmpty();
        assertThat(tokens).anyMatch(t -> t.type() == TokenType.HEADING);
        assertThat(tokens).anyMatch(t -> t.type() == TokenType.BOLD);
        assertThat(tokens).anyMatch(t -> t.type() == TokenType.ITALIC);
        assertThat(tokens).anyMatch(t -> t.type() == TokenType.LINK);
    }

    @Test
    @DisplayName("TokenizerRegistry detects language by extension")
    void testTokenizerRegistry() {
        assertThat(TokenizerRegistry.detect(Path.of("App.java")).getLanguageId()).isEqualTo("java");
        assertThat(TokenizerRegistry.detect(Path.of("package.json")).getLanguageId()).isEqualTo("json");
        assertThat(TokenizerRegistry.detect(Path.of("pom.xml")).getLanguageId()).isEqualTo("xml");
        assertThat(TokenizerRegistry.detect(Path.of("index.html")).getLanguageId()).isEqualTo("xml");
        assertThat(TokenizerRegistry.detect(Path.of("README.md")).getLanguageId()).isEqualTo("markdown");
        assertThat(TokenizerRegistry.detect(Path.of("notes.txt")).getLanguageId()).isEqualTo("plain");
    }
}
