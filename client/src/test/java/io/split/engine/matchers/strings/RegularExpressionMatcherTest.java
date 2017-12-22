package io.split.engine.matchers.strings;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public class RegularExpressionMatcherTest {
    @Parameterized.Parameters(name = "{index}: ({1} =~ /{0}/) == {2}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"abc", "abc", true},
                {"abc", "zabcd", true},
                {"abc", "bc", false},
                {"abc", "ab", false},
                {"^abc", "abc", true},
                {"^abc", "abcbdc", true},
                {"^abc", "abcabc", true},
                {"^abc", "zabcabc", false},
                {"abc$", "abcabc", true},
                {"abc$", "zabcabc", true},
                {"abc$", "abcabcz", false},
                {"a|b", "abcabcz", true},
                {"a|b", "zczcz", false},
                {"^abc|abc$", "abcabc", true},
                {"^abc|abc$", "zabcab", false},
                {"ab{2,4}c", "abbc", true},
                {"ab{2,4}c", "abbbc", true},
                {"ab{2,4}c", "abbbbc", true},
                {"ab{2,4}c", "abc", false},
                {"ab{2,4}c", "abzbbc", false},
                {"ab{2,4}c", "abbbbbbbbbbc", false},
                {"ab{2,}c", "abbc", true},
                {"ab{2,}c", "abbbc", true},
                {"ab{2,}c", "abbbbc", true},
                {"ab{2,}c", "abc", false},
                {"ab{2,}c", "abzbbc", false},
                {"ab{2,}c", "abbbbbbbbbbc", true},
                {"ab*c", "ac", true},
                {"ab*c", "abc", true},
                {"ab*c", "abbc", true},
                {"ab*c", "abbbc", true},
                {"ab*c", "ab", false},
                {"ab*c", "bc", false},
                {"ab+c", "ac", false},
                {"ab+c", "abc", true},
                {"ab+c", "abbc", true},
                {"ab+c", "abbbc", true},
                {"ab+c", "ab", false},
                {"ab+c", "bc", false},
                {"ab?c", "ac", true},
                {"ab?c", "abc", true},
                {"ab?c", "abbc", false},
                {"ab?c", "abbbc", false},
                {"ab?c", "ab", false},
                {"ab?c", "bc", false},
                {"a.c", "abc", true},
                {"a.c", "adc", true},
                {"a.c", "azc", true},
                {"a.c", "xdc", false},
                {"a.c", "ac", false},
                {"a\\.c", "abc", false},
                {"a\\.c", "adc", false},
                {"a\\.c", "azc", false},
                {"a\\.c", "xdc", false},
                {"a\\.c", "ac", false},
                {"a\\.c", "a.c", true},
                {"[abc]", "a", true},
                {"[abc]", "b", true},
                {"[abc]", "c", true},
                {"[abc]", "z", false},
                {"[abc]", "ab", true},
                {"[abc]", "ac", true},
                {"[Aa]bc", "a", false},
                {"[Aa]bc", "b", false},
                {"[Aa]bc", "c", false},
                {"[Aa]bc", "z", false},
                {"[Aa]bc", "ab", false},
                {"[Aa]bc", "ac", false},
                {"[Aa]bc", "abc", true},
                {"[Aa]bc", "Abc", true},
                {"[abc]+", "a", true},
                {"[abc]+", "aba", true},
                {"[abc]+", "abba", true},
                {"[abc]+", "acbabcacaa", true},
                {"[abc]+", "axbaxcaxax", true},
                {"[abc]+", "xxzyxzyxyx", false},
                {"[^abc]+", "acbaccacaa", false},
                {"[^abc]+", "acbacaaa", false},
                {"[^abc]+", "aa", false},
                {"[^abc]+", "xzy", true},
                {"\\d\\d", "11", true},
                {"\\d\\d", "a1", false},
                {"\\d\\d", "1b1a1", false},
                {"\\d\\d", "1a1", false},
                {"\\w+", "foo", true},
                {"\\w+", "12bar8", true},
                {"\\w+", "foo_1", true},
                {"\\w+", "foo-1", true},
                {"\\w+", "foo- 1", true},
                {"\\w+", "foo- %$1", true},
                {"\\w+", "%$", false},
                {"\\W+", "foo", false},
                {"\\W+", "12bar8", false},
                {"\\W+", "foo_1", false},
                {"\\W+", "foo-1", true},
                {"\\W+", "foo_ 1", true},
                {"\\W+", "foo1", false},
                {"\\W+", "%$", true},
                {"100\\s*mk", "100mk", true},
                {"100\\s*mk", "100    mk", true},
                {"100\\s*mk", "100  X  mk", false},
                {"abc\\b", "abc!", true},
                {"abc\\b", "abcd", false},
                {"perl\\B", "perlert", true},
                {"perl\\B", "perl stuff", false},
                {"(abc){3}", "abcabcabc", true},
                {"(abc){3}", "abcacabc", false},
                {"(abc){3}", "abc", false},
                {"^[a-z0-9_-]{3,16}$", "my-us3r_n4m3", true},
                {"^[a-z0-9_-]{3,16}$", "commonusername", true},
                {"^[a-z0-9_-]{3,16}$", "n0", false},
                {"^[a-z0-9_-]{3,16}$", "th1s1s-wayt00_l0ngt0beausername", false},
                {"^[a-z0-9-]+$", "my-title-here", true},
                {"^[a-z0-9-]+$", "my_title_here", false},
                {"^([a-z0-9_\\.-]+)@([\\da-z\\.-]+)\\.([a-z\\.]{2,6})$", "john@doe.com", true},
                {"^([a-z0-9_\\.-]+)@([\\da-z\\.-]+)\\.([a-z\\.]{2,6})$", "john@doe.something", false},
                {"^([a-z0-9_\\.-]+)@([\\da-z\\.-]+)\\.([a-z\\.]{2,6})$", "johndoe.sg", false},
                {"^(https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w \\.-]*)*\\/?$", "http://split.io/about", true},
                {"^(https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w \\.-]*)*\\/?$", "http://google.com/some/file!.html", false},
                {"^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$", "73.60.124.136", true},
                {"^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$", "256.60.124.136", false},
                {"^\\d+$", "123", true},
                {"^\\d+$", "4323", true},
                {"^\\d+$", "4566663", true},
                {"^\\d+$", "-10", false},
                {"^\\d+$", "456.666.3", false},
                {"^-\\d+$", "4566663", false},
                {"^-\\d+$", "-10", true},
                {"^-\\d+$", "456.666.3", false},
                {"^-?\\d+$", "3534", true},
                {"^-?\\d+$", "-3534", true},
                {"^-?\\d+$", "35.34", false},
                {"^-?\\d+$", "-35.34", false},
                {"^\\d*\\.?\\d+$", "12.3", true},
                {"^\\d*\\.?\\d+$", "-12.3", false},
                {"^-\\d*\\.?\\d+$", "12.3", false},
                {"^-\\d*\\.?\\d+$", "-12.3", true},
                {"^-?\\d*\\.?\\d+$", "12.3", true},
                {"^-?\\d*\\.?\\d+$", "-12.3", true},
                {"^-?\\d*\\.?\\d+$", "-1a2.a3", false},
                {"^(19|20)\\d{2}$", "1900", true},
                {"^(19|20)\\d{2}$", "2005", true},
                {"^(19|20)\\d{2}$", "1810", false},
                {"^([1-9]|0[1-9]|[12][0-9]|3[01])\\D([1-9]|0[1-9]|1[012])\\D(19[0-9][0-9]|20[0-9][0-9])$", "11/11/2011", true},
                {"^([1-9]|0[1-9]|[12][0-9]|3[01])\\D([1-9]|0[1-9]|1[012])\\D(19[0-9][0-9]|20[0-9][0-9])$", "13/13/2011", false},
        });
    }


    private String pattern;
    private String string;
    private boolean result;

    public RegularExpressionMatcherTest(String pattern, String string, boolean result) {
        this.pattern = pattern;
        this.string = string;
        this.result = result;
    }

    @Test
    public void test() {
        RegularExpressionMatcher matcher = new RegularExpressionMatcher(pattern);
        assertThat(matcher.match(string, null, null, null), is(result));
    }
}