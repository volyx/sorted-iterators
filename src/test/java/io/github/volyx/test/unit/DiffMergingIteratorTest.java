package io.github.volyx.test.unit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import io.github.volyx.SortedIterators;
import org.junit.Assert;
import org.junit.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

@RunWith(JUnitPlatform.class)
public class DiffMergingIteratorTest {
	private static final Random RANDOM = new Random();
	public static final int MAX_VALUE = 100;

	@Test
	public void test() {

		final List<Integer> source = List.of(1, 2, 3, 4, 5);
		final List<Integer> target = List.of(1, 2, 4, 5);

		final Iterator<Integer> it = SortedIterators.diffSorted(source.iterator(), target.iterator(), Integer::compareTo);

		final List<Integer> result = ImmutableList.copyOf(it);

		assertEquals(List.of(3), result);
	}

	@Test
	public void testRandom() {
		int attempts = 0;
		while (attempts++ < 20) {

			List<Integer> one = IntStream.generate(() -> RANDOM.nextInt(MAX_VALUE)).limit(RANDOM.nextInt(MAX_VALUE)).boxed().sorted().collect(Collectors.toList());
			List<Integer> two = IntStream.generate(() -> RANDOM.nextInt(MAX_VALUE)).limit(RANDOM.nextInt(MAX_VALUE)).boxed().sorted().collect(Collectors.toList());

			final Iterator<Integer> it = SortedIterators.diffSorted(one.iterator(), two.iterator(), Integer::compareTo);

			final List<Integer> result = ImmutableList.copyOf(it);

			Set<Integer> uniq1 = new HashSet<>(one);
			Set<Integer> uniq2 = new HashSet<>(two);

			List<Integer> diff = new ArrayList<>(Sets.difference(uniq1, uniq2));

			Collections.sort(diff);

			Assert.assertEquals("one: " + one + " two: " + two, diff, result);
		}
	}
}