package io.github.volyx.test.unit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import io.github.volyx.SortedIterators;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class SortedIteratorsTest {

	private static final Random RANDOM = new Random();
	private static final int MAX_VALUE = 100;


	@Test
	public void mergeSorted() {

		final List<Integer> one = List.of(1, 2, 3, 3, 5, 5, 6, 8, 8).stream().distinct().collect(Collectors.toList());
		final List<Integer> two = List.of(1, 4, 5, 5, 5, 6, 8, 9).stream().distinct().collect(Collectors.toList());

		final UnmodifiableIterator<Integer> it = SortedIterators.mergeSorted(List.of(one.iterator(), two.iterator()), Integer::compare);

		final ImmutableList<Integer> result = ImmutableList.copyOf(it);

		List<Integer> expected = new ArrayList<>();
		expected.addAll(one);
		expected.addAll(two);
		Collections.sort(expected);

		assertEquals(expected, result);
	}

	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	@Test
	public void diffSortedBoth() {
		int attempts = 0;
		while (attempts++ < 100) {
			List<Integer> one = IntStream.generate(() -> RANDOM.nextInt(MAX_VALUE)).limit(RANDOM.nextInt(MAX_VALUE)).boxed().sorted().collect(Collectors.toList());
			List<Integer> two = IntStream.generate(() -> RANDOM.nextInt(MAX_VALUE)).limit(RANDOM.nextInt(MAX_VALUE)).boxed().sorted().collect(Collectors.toList());

//			one = List.<Integer>of(8, 9).stream().sorted().collect(Collectors.toList());
//			two = List.<Integer>of(4, 5).stream().sorted().collect(Collectors.toList());

			System.out.println("one = " + one);
			System.out.println("two = " + two);
			final List<Integer> removeListExpected = Sets.difference(new HashSet<>(one), new HashSet<>(two)).stream().sorted().collect(Collectors.toList());
			final List<Integer> addListExpected = Sets.difference(new HashSet<>(two), new HashSet<>(one)).stream().sorted().collect(Collectors.toList());

			final List<Integer> addList = new ArrayList<>();
			final Consumer<Integer> addConsumer = addList::add;

			final List<Integer> removeList = new ArrayList<>();
			final Consumer<Integer> removeConsumer = removeList::add;

			SortedIterators.diffSortedBoth(one.iterator(), two.iterator(), Integer::compare, addConsumer, removeConsumer);

			assertEquals("add list: one " + one + " two " + two + " remove " + removeList, addListExpected, addList);
			assertEquals("remove list: one " + one + " two " + two, removeListExpected, removeList);
		}
	}
}