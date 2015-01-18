package org.elasticsearch.index.analysis.url;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.elasticsearch.common.base.Strings;
import org.elasticsearch.index.analysis.URLPart;

import java.io.IOException;
import java.net.URL;

/**
 * Joe Linn
 * 1/17/2015
 */
public final class URLTokenFilter extends TokenFilter {
    public static final String NAME = "url";

    private final URLPart part;

    private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);

    public URLTokenFilter(TokenStream input, URLPart part) {
        super(input);
        this.part = part;
    }

    @Override
    public boolean incrementToken() throws IOException {
        if(input.incrementToken()){
            final String urlString = termAttribute.toString();
            termAttribute.setEmpty();
            if (Strings.isNullOrEmpty(urlString) || urlString.equals("null")) {
                return false;
            }
            URL url = new URL(urlString);
            switch (part) {
                case PROTOCOL:
                    termAttribute.append(url.getProtocol());
                    break;
                case HOST:
                    termAttribute.append(url.getHost());
                    break;
                case PORT:
                    termAttribute.append(String.valueOf(url.getPort()));
                    break;
                case PATH:
                    termAttribute.append(url.getPath());
                    break;
                case REF:
                    termAttribute.append(url.getRef());
                    break;
                case QUERY:
                    termAttribute.append(url.getQuery());
                    break;
                case WHOLE:
                default:
                    termAttribute.append(url.toString());
            }
            return true;
        }
        return false;
    }
}
