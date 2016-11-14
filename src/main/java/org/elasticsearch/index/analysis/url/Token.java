package org.elasticsearch.index.analysis.url;

import com.google.common.base.Objects;
import org.elasticsearch.index.analysis.URLPart;

/**
 * @author Joe Linn
 *         8/14/2016
 */
class Token {
    private final String token;
    private final URLPart part;
    private final int start;
    private final int end;

    public Token(String token, URLPart part, int start, int end) {
        this.token = token;
        this.part = part;
        this.start = start;
        this.end = end;
    }

    public String getToken() {
        return token;
    }

    public URLPart getPart() {
        return part;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Token)) {
            return false;
        }
        Token that = (Token) obj;
        return this.start == that.start
                && this.end == that.end
                && Objects.equal(this.token, that.token)
                && Objects.equal(this.part, that.part);
    }

    @Override
    public int hashCode() {
        int result = token != null ? token.hashCode() : 0;
        result = 31 * result + part.hashCode();
        result = 31 * result + start;
        result = 31 * result + end;
        return result;
    }


    @Override
    public String toString() {
        return "Token{" +
                "token='" + token + '\'' +
                ", part=" + part +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
