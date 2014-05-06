package com.noradltd.easybuilder;

import static org.junit.Assert.assertThat;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class FloatCloseToMatcher extends TypeSafeMatcher<Float> {

	private Float given, epsilon;

	private FloatCloseToMatcher(Float g, Float e) {
		given = g;
		epsilon = e;
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("is not close to " + given);
	}

	@Override
	protected boolean matchesSafely(Float item) {
		assertThat(item.doubleValue(), org.hamcrest.Matchers.closeTo(given, epsilon));
		return true;
	}

	@Factory
	public static <T> Matcher<Float> closeTo(float given, float epsilon) {
		return new FloatCloseToMatcher(given, epsilon);
	}

}
