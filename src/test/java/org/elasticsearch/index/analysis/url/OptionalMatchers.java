package org.elasticsearch.index.analysis.url;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.Optional;

/**
 * @author Joe Linn
 *         6/25/2016
 */
public class OptionalMatchers {
    public static Matcher<Optional<?>> isPresent() {
        return new PresenceMatcher();
    }


    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static class PresenceMatcher extends TypeSafeMatcher<Optional<?>> {

        @Override
        protected boolean matchesSafely(Optional<?> optional) {
            return optional.isPresent();
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("is <Present>");
        }


        @Override
        protected void describeMismatchSafely(Optional<?> item, Description mismatchDescription) {
            mismatchDescription.appendText("was <Empty>");
        }
    }


    public static Matcher<Optional<?>> isEmpty() {
        return new EmptyMatcher();
    }


    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static class EmptyMatcher extends PresenceMatcher {
        @Override
        protected boolean matchesSafely(Optional<?> optional) {
            return !super.matchesSafely(optional);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("is <Empty>");
        }

        @SuppressWarnings("OptionalGetWithoutIsPresent")
        @Override
        protected void describeMismatchSafely(Optional<?> item, Description mismatchDescription) {
            mismatchDescription.appendText("had value ")
                    .appendValue(item.get());
        }
    }


    public static <T> Matcher<Optional<T>> hasValue(Matcher<? super T> matcher) {
        return new HasValue<>(matcher);
    }


    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static class HasValue<T> extends TypeSafeMatcher<Optional<T>> {
        private final Matcher<? super T> matcher;


        private HasValue(Matcher<? super T> matcher) {
            this.matcher = matcher;
        }


        @Override
        protected boolean matchesSafely(Optional<T> tOptional) {
            return tOptional.isPresent() && matcher.matches(tOptional.get());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("has value that is ");
            matcher.describeTo(description);
        }


        @Override
        protected void describeMismatchSafely(Optional<T> item, Description mismatchDescription) {
            if (item.isPresent()) {
                mismatchDescription.appendText("value ")
                        .appendValue(item.get());
                matcher.describeTo(mismatchDescription);
            } else {
                mismatchDescription.appendText("was <Empty>");
            }
        }
    }
}
