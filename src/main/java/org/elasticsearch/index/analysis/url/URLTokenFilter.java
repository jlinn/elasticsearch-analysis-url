package org.elasticsearch.index.analysis.url;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.elasticsearch.common.base.Strings;
import org.elasticsearch.index.analysis.URLPart;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * Joe Linn
 * 1/17/2015
 */
public final class URLTokenFilter extends TokenFilter {
    public static final String NAME = "url";

    private final URLPart part;

    private final boolean urlDeocde;

    private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);

    public URLTokenFilter(TokenStream input, URLPart part) {
        this(input, part, false);
    }

    public URLTokenFilter(TokenStream input, URLPart part, boolean urlDecode) {
        super(input);
        this.part = part;
        this.urlDeocde = urlDecode;
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
            String partString;
            switch (part) {
                case PROTOCOL:
                    partString = url.getProtocol();
                    break;
                case HOST:
                    partString = url.getHost();
                    break;
                case PORT:
                    partString = String.valueOf(url.getPort());
                    break;
                case PATH:
                    partString = url.getPath();
                    break;
                case REF:
                    partString = url.getRef();
                    break;
                case QUERY:
                    partString = url.getQuery();
                    break;
                case WHOLE:
                default:
                    partString = url.toString();
            }
            if (urlDeocde) {
                partString = URLDecoder.decode(partString, "UTF-8");
            }
            termAttribute.append(partString);
            return true;
        }
        return false;
    }
}
