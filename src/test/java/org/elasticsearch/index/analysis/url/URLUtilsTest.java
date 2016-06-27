package org.elasticsearch.index.analysis.url;

import org.elasticsearch.index.analysis.URLPart;
import org.junit.Test;

import static org.elasticsearch.index.analysis.url.OptionalMatchers.hasValue;
import static org.elasticsearch.index.analysis.url.OptionalMatchers.isEmpty;
import static org.elasticsearch.index.analysis.url.URLUtils.getPart;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Joe Linn
 *         6/25/2016
 */
public class URLUtilsTest {
    private static final String URL_1 = "http://foo.bar.com/baz/bat.html#whee?bob=loblaw&this=that";
    private static final String URL_2 = "foo.bar.com/baz/bat.html#whee?bob=loblaw&this=that";
    private static final String URL_3 = "/baz/bat.html#whee?bob=loblaw&this=that";
    private static final String URL_4 = "/baz/bat.html?bob=loblaw&this=that";

    @Test
    public void testGetProtocol() {
        final URLPart part = URLPart.PROTOCOL;
        assertThat(getPart(URL_1, part), hasValue(equalTo("http")));
        assertThat(getPart(URL_2, part), isEmpty());
    }


    @Test
    public void testGetHost() {
        final URLPart part = URLPart.HOST;
        assertThat(getPart(URL_1, part), hasValue(equalTo("foo.bar.com")));
        assertThat(getPart(URL_2, part), hasValue(equalTo("foo.bar.com")));
    }


    @Test
    public void testGetPort() {
        final URLPart part = URLPart.PORT;
        assertThat(getPart(URL_1, part), hasValue(equalTo("80")));
        assertThat(getPart(URL_2, part), isEmpty());
    }


    @Test
    public void testGetPath() {
        final URLPart part = URLPart.PATH;
        assertThat(getPart(URL_1, part), hasValue(equalTo("/baz/bat.html")));
        assertThat(getPart(URL_2, part), hasValue(equalTo("/baz/bat.html")));
        assertThat(getPart(URL_3, part), hasValue(equalTo("/baz/bat.html")));
    }


    @Test
    public void testGetRef() {
        final URLPart part = URLPart.REF;
        assertThat(getPart(URL_1, part), hasValue(equalTo("whee")));
        assertThat(getPart(URL_2, part), hasValue(equalTo("whee")));
        assertThat(getPart(URL_3, part), hasValue(equalTo("whee")));
    }


    @Test
    public void testGetQuery() {
        final URLPart part = URLPart.QUERY;
        assertThat(getPart(URL_1, part), hasValue(equalTo("bob=loblaw&this=that")));
        assertThat(getPart(URL_2, part), hasValue(equalTo("bob=loblaw&this=that")));
        assertThat(getPart(URL_3, part), hasValue(equalTo("bob=loblaw&this=that")));
        assertThat(getPart(URL_4, part), hasValue(equalTo("bob=loblaw&this=that")));
    }
}