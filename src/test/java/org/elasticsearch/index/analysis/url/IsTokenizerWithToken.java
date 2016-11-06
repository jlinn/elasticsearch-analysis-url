package org.elasticsearch.index.analysis.url;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
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
public class IsTokenizerWithToken extends TypeSafeMatcher<Tokenizer> {
    private static final Logger log = LoggerFactory.getLogger(IsTokenizerWithToken.class);

    private final String token;


    public IsTokenizerWithToken(String token) {
        this.token = token;
    }


    @Override
    protected boolean matchesSafely(Tokenizer tokenizer) {
        CharTermAttribute termAttribute = tokenizer.getAttribute(CharTermAttribute.class);
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
                    return true;
                }
            }
        } catch (IOException e) {
            log.error("Unable to increment tokenizer.", e);
        }
        return false;
    }


    @Override
    public void describeTo(Description description) {
        description.appendText("tokenized the string '").appendText(token).appendText("'");
    }


    @Factory
    public static IsTokenizerWithToken hasToken(String token){
        return new IsTokenizerWithToken(token);
    }
}
