package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.Tokenizer;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.url.URLTokenizer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Joe Linn
 * 8/1/2015
 */
public class URLTokenizerFactory extends AbstractTokenizerFactory {
    private List<URLPart> parts;
    private boolean urlDecode;
    private boolean tokenizeHost;
    private boolean tokenizePath;
    private boolean tokenizeQuery;
    private boolean allowMalformed;
    private boolean tokenizeMalformed;


    public URLTokenizerFactory(IndexSettings indexSettings, Environment environment, String name, Settings settings) {
        super(indexSettings, settings, name);
        List<String> parts = settings.getAsList("part");
        if (parts != null && parts.size() > 0) {
            this.parts = parts.stream()
                    .map(URLPart::fromString)
                    .collect(Collectors.toList());
        }
        this.urlDecode = settings.getAsBoolean("url_decode", false);
        this.tokenizeHost = settings.getAsBoolean("tokenize_host", true);
        this.tokenizePath = settings.getAsBoolean("tokenize_path", true);
        this.tokenizeQuery = settings.getAsBoolean("tokenize_query", true);
        this.allowMalformed = settings.getAsBoolean("allow_malformed", false);
        this.tokenizeMalformed = settings.getAsBoolean("tokenize_malformed", false);
    }


    @Override
    public Tokenizer create() {
        URLTokenizer tokenizer = new URLTokenizer();
        tokenizer.setParts(parts);
        tokenizer.setUrlDecode(urlDecode);
        tokenizer.setTokenizeHost(tokenizeHost);
        tokenizer.setTokenizePath(tokenizePath);
        tokenizer.setTokenizeQuery(tokenizeQuery);
        tokenizer.setAllowMalformed(allowMalformed);
        tokenizer.setTokenizeMalformed(tokenizeMalformed);
        return tokenizer;
    }
}
