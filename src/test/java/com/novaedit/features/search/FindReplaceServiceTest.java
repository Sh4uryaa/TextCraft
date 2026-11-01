package com.novaedit.features.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FindReplaceServiceTest {

    private final FindReplaceService service = new FindReplaceService();

    @Test
    @DisplayName("Literal case-insensitive find all")
    void testFindAllCaseInsensitive() {
        String text = "The cat sat on the mat. What a cat!";
        List<SearchResult> results = service.findAll(text, new FindOptions("cat", false, false, false));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).start()).isEqualTo(4);
        assertThat(results.get(1).start()).isEqualTo(31);
    }

    @Test
    @DisplayName("Case-sensitive find all")
    void testFindAllCaseSensitive() {
        String text = "Java java JAVA";
        List<SearchResult> results = service.findAll(text, new FindOptions("java", true, false, false));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).start()).isEqualTo(5);
    }

    @Test
    @DisplayName("Whole word search prevents partial token match")
    void testWholeWord() {
        String text = "test testing tested test";
        List<SearchResult> results = service.findAll(text, new FindOptions("test", false, true, false));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).start()).isEqualTo(0);
        assertThat(results.get(1).start()).isEqualTo(20);
    }

    @Test
    @DisplayName("Regex pattern search and groups")
    void testRegexSearch() {
        String text = "foo123 and bar456 and baz";
        List<SearchResult> results = service.findAll(text, new FindOptions("[a-z]+[0-9]+", false, false, true));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).text()).isEqualTo("foo123");
        assertThat(results.get(1).text()).isEqualTo("bar456");
    }

    @Test
    @DisplayName("Find next and previous with wrap-around")
    void testNextPrevious() {
        String text = "alpha beta alpha gamma alpha";
        FindOptions opts = new FindOptions("alpha", true, false, false);

        // find next from 10
        SearchResult next = service.findNext(text, 10, opts);
        assertThat(next.start()).isEqualTo(11);

        // find next beyond last -> wraps to first
        SearchResult wrap = service.findNext(text, 25, opts);
        assertThat(wrap.start()).isEqualTo(0);

        // find prev from 15
        SearchResult prev = service.findPrevious(text, 15, opts);
        assertThat(prev.start()).isEqualTo(11);

        // find prev before first -> wraps to last
        SearchResult wrapPrev = service.findPrevious(text, 0, opts);
        assertThat(wrapPrev.start()).isEqualTo(23);
    }

    @Test
    @DisplayName("Replace all occurrences")
    void testReplaceAll() {
        String text = "Hello foo, welcome foo!";
        String replaced = service.replaceAll(text, new FindOptions("foo", true, false, false), "world");
        assertThat(replaced).isEqualTo("Hello world, welcome world!");
    }
}
