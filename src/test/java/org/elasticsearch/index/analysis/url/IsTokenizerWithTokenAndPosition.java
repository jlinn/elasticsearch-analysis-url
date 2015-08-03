package org.elasticsearch.index.analysis.url;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;

import java.io.IOException;

/**
 * Joe Linn
 * 8/2/2015
 */
public class IsTokenizerWithTokenAndPosition extends TypeSafeMatcher<Tokenizer> {
    private static final Logger log = Logger.getLogger(IsTokenizerWithTokenAndPosition.class);

    private final String token;
    private final int start;
    private final int end;

    private boolean foundToken;
    private int actualStart;
    private int actualEnd;

    public IsTokenizerWithTokenAndPosition(String token, int start, int end) {
        this.token = token;
        this.start = start;
        this.end = end;
    }

    @Override
    protected boolean matchesSafely(Tokenizer tokenizer) {
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
    protected void describeMismatchSafely(Tokenizer item, Description mismatchDescription) {
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
    public static IsTokenizerWithTokenAndPosition hasTokenAtOffset(String token, int start, int end) {
        return new IsTokenizerWithTokenAndPosition(token, start, end);
    }
}
