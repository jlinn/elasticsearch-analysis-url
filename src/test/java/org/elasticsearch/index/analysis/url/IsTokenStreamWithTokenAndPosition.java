package org.elasticsearch.index.analysis.url;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Joe Linn
 * 8/2/2015
 */
public class IsTokenStreamWithTokenAndPosition extends TypeSafeMatcher<TokenStream> {
    private static final Logger log = LoggerFactory.getLogger(IsTokenStreamWithTokenAndPosition.class);

    private final String token;
    private final int start;
    private final int end;

    private boolean foundToken;
    private int actualStart;
    private int actualEnd;

    public IsTokenStreamWithTokenAndPosition(String token, int start, int end) {
        this.token = token;
        this.start = start;
        this.end = end;
    }

    @Override
    protected boolean matchesSafely(TokenStream tokenizer) {
        CharTermAttribute termAttribute = tokenizer.getAttribute(CharTermAttribute.class);
        OffsetAttribute offset = tokenizer.getAttribute(OffsetAttribute.class);
        try {
            tokenizer.reset();
        } catch (IOException e) {
            log.error("Unable to reset tokenizer.", e);
            return false;
        }
        tokenizer.clearAttributes();
        try {
            while (tokenizer.incrementToken()) {
                if (termAttribute.toString().equals(token)) {
                    foundToken = true;
                    actualStart = offset.startOffset();
                    actualEnd = offset.endOffset();
                    if (actualStart == start && actualEnd == end) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            log.error("Unable to increment tokenizer.", e);
        }
        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("tokenizer containing token '")
                .appendText(token)
                .appendText("' starting at offset ")
                .appendValue(start)
                .appendText(" and ending at offset ")
                .appendValue(end);
    }


    @Override
    protected void describeMismatchSafely(TokenStream item, Description mismatchDescription) {
        if(!foundToken){
            mismatchDescription.appendText("tokenizer which did not contain token ").appendValue(token);
        } else {
            mismatchDescription.appendText("tokenizer containing token ")
                    .appendValue(token)
                    .appendText(" starting at offset ")
                    .appendValue(actualStart)
                    .appendText(" and ending at offset ")
                    .appendValue(actualEnd);
        }
    }

    @Factory
    public static IsTokenStreamWithTokenAndPosition hasTokenAtOffset(String token, int start, int end) {
        return new IsTokenStreamWithTokenAndPosition(token, start, end);
    }
}
