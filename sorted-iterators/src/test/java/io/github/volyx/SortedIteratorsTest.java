package io.github.volyx;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

class SortedIteratorsTest {

    private static final Random RANDOM = new Random();
    private static final int MAX_VALUE = 100;

    @Test
    void mergeSorted() {

        final List<Integer> one = List.of(1, 2, 3, 3, 5, 5, 6, 8, 8);
        final List<Integer> two = List.of(1, 4, 5, 5, 5, 6, 8, 9);

        final Iterator<Integer> it = SortedIterators.union(List.of(one.iterator(), two.iterator()), Integer::compare);

        final ImmutableList<Integer> result = ImmutableList.copyOf(it);

        List<Integer> expected = new ArrayList<>();
        expected.addAll(one);
        expected.addAll(two);
        Collections.sort(expected);
        expected = expected.stream().filter(new SortedIterators.DeDuplicatePredicate<>()).collect(Collectors.toList());

        assertEquals(expected, result);
    }

    @Timeout(value = 30)
    @Test
    void diffSortedBoth() {
        int attempts = 0;
        while (attempts++ < 100) {
            List<Integer> one = IntStream.generate(() -> RANDOM.nextInt(MAX_VALUE)).limit(RANDOM.nextInt(MAX_VALUE)).boxed().sorted().collect(Collectors.toList());
            List<Integer> two = IntStream.generate(() -> RANDOM.nextInt(MAX_VALUE)).limit(RANDOM.nextInt(MAX_VALUE)).boxed().sorted().collect(Collectors.toList());

//			one = List.<Integer>of(8, 9).stream().sorted().collect(Collectors.toList());
//			two = List.<Integer>of(4, 5).stream().sorted().collect(Collectors.toList());

            final List<Integer> removeListExpected = Sets.difference(new HashSet<>(one), new HashSet<>(two)).stream().sorted().collect(Collectors.toList());
            final List<Integer> addListExpected = Sets.difference(new HashSet<>(two), new HashSet<>(one)).stream().sorted().collect(Collectors.toList());

            final List<Integer> addList = new ArrayList<>();
            final Consumer<Integer> addConsumer = addList::add;

            final List<Integer> removeList = new ArrayList<>();
            final Consumer<Integer> removeConsumer = removeList::add;

            SortedIterators.differenceConsumer(one.iterator(), two.iterator(), Integer::compare, addConsumer, removeConsumer);

            assertEquals("add list: one " + one + " two " + two + " remove " + removeList, addListExpected, addList);
            assertEquals("remove list: one " + one + " two " + two, removeListExpected, removeList);
        }
    }


    @Test
    void differenceTest() {
        int attempts = 0;
        while (attempts++ < 100) {
            List<Integer> one = IntStream.generate(() -> RANDOM.nextInt(MAX_VALUE)).limit(RANDOM.nextInt(MAX_VALUE)).boxed().sorted().collect(Collectors.toList());
            List<Integer> two = IntStream.generate(() -> RANDOM.nextInt(MAX_VALUE)).limit(RANDOM.nextInt(MAX_VALUE)).boxed().sorted().collect(Collectors.toList());

            final List<Integer> expected = Sets.symmetricDifference(new HashSet<>(two), new HashSet<>(one)).stream().sorted().collect(Collectors.toList());

            final Iterator<Integer> it = SortedIterators.difference(one.iterator(), two.iterator(), Integer::compare);

            final List<Integer> real = ImmutableList.copyOf(it);

            assertEquals("one " + one + " two " + two + " expected " + expected, expected, real);
        }
    }


    @Test
    void testExclude() {

        final List<Integer> source = List.of(1, 2, 3, 4, 5);
        final List<Integer> target = List.of(1, 2, 4, 5);

        final Iterator<Integer> it = SortedIterators.exclude(source.iterator(), target.iterator(), Integer::compareTo);

        final List<Integer> result = ImmutableList.copyOf(it);

        assertEquals(List.of(3), result);
    }

    @Test
    void testRandom() {
        int attempts = 0;
        while (attempts++ < 200) {

            List<Integer> one = IntStream.generate(() -> RANDOM.nextInt(MAX_VALUE)).limit(RANDOM.nextInt(MAX_VALUE)).boxed().sorted().collect(Collectors.toList());
            List<Integer> two = IntStream.generate(() -> RANDOM.nextInt(MAX_VALUE)).limit(RANDOM.nextInt(MAX_VALUE)).boxed().sorted().collect(Collectors.toList());

            final List<Integer> removeList = new ArrayList<>();
            final List<Integer> addList = new ArrayList<>();

            final Consumer<Integer> removeConsumer = removeList::add;
            final Consumer<Integer> addConsumer = addList::add;
            SortedIterators.differenceConsumer(one.iterator(), two.iterator(), Integer::compareTo, addConsumer, removeConsumer);

            final List<Integer> removeResult = ImmutableList.copyOf(SortedIterators.exclude(one.iterator(), two.iterator(), Integer::compareTo));
            final List<Integer> addResult = ImmutableList.copyOf(SortedIterators.exclude(two.iterator(), one.iterator(), Integer::compareTo));

            Set<Integer> uniq1 = new HashSet<>(one);
            Set<Integer> uniq2 = new HashSet<>(two);

            List<Integer> diff = new ArrayList<>(Sets.difference(uniq1, uniq2));

            Collections.sort(diff);

            Assert.assertEquals("one: " + one + " two: " + two, diff, removeResult);
            Assert.assertEquals("one: " + one + " two: " + two, removeList, removeResult);
            Assert.assertEquals("one: " + one + " two: " + two, addList, addResult);
        }
    }

    @Test
    void testSymmetricDifferenceRandom() {

        int attempt = 0;

        while (attempt++ < 20) {
            List<Integer> one = IntStream.generate(() -> RANDOM.nextInt(MAX_VALUE)).limit(RANDOM.nextInt(MAX_VALUE)).boxed().sorted().collect(Collectors.toList());
            List<Integer> two = IntStream.generate(() -> RANDOM.nextInt(MAX_VALUE)).limit(RANDOM.nextInt(MAX_VALUE)).boxed().sorted().collect(Collectors.toList());

            Set<Integer> uniq1 = new HashSet<>(one);
            Set<Integer> uniq2 = new HashSet<>(two);

            List<Integer> diff = new ArrayList<>(Sets.symmetricDifference(uniq1, uniq2));

            Collections.sort(diff);

            List<Integer> addList = new ArrayList<>();
            Consumer<Integer> addConsumer = addList::add;

            SortedIterators.symmetricDifference(one.iterator(), two.iterator(), Integer::compareTo, addConsumer);

            Collections.sort(addList);

            Assert.assertNotNull(addList);

            Assert.assertEquals("one: " + one + " two: " + two + " diff: " + diff, diff.size(), addList.size());

            Assert.assertEquals("one: " + one + " two: " + two + " diff: " + diff, diff, addList);
        }
    }


    @Test
    void testSymmetricDifferenceIterativeRandom() {

        int attempt = 0;

        while (attempt++ < 100) {
            List<Integer> one = IntStream.generate(() -> RANDOM.nextInt(10)).limit(RANDOM.nextInt(10)).boxed().sorted().collect(Collectors.toList());
            List<Integer> two = IntStream.generate(() -> RANDOM.nextInt(10)).limit(RANDOM.nextInt(10)).boxed().sorted().collect(Collectors.toList());

            Set<Integer> uniq1 = new HashSet<>(one);
            Set<Integer> uniq2 = new HashSet<>(two);

            List<Integer> diff = new ArrayList<>(Sets.symmetricDifference(uniq1, uniq2));

            Collections.sort(diff);

            List<Integer> addList = ImmutableList.copyOf(SortedIterators.union(
                    List.of(
                            SortedIterators.exclude(one.iterator(), two.iterator(), Integer::compareTo),
                            SortedIterators.exclude(two.iterator(), one.iterator(), Integer::compareTo)
                    ),
                    Integer::compareTo
            ));

            Assert.assertNotNull(addList);

            Assert.assertEquals("one: " + one + " two: " + two + " diff: " + diff, diff, addList);
        }
    }

    @Test
    void testIntersectionRandom() {

        int attempt = 0;

        while (attempt++ < 200) {
            List<Integer> one = IntStream.generate(() -> RANDOM.nextInt(MAX_VALUE)).limit(RANDOM.nextInt(MAX_VALUE)).boxed().sorted().collect(Collectors.toList());
            List<Integer> two = IntStream.generate(() -> RANDOM.nextInt(MAX_VALUE)).limit(RANDOM.nextInt(MAX_VALUE)).boxed().sorted().collect(Collectors.toList());

            Set<Integer> uniq1 = new HashSet<>(one);
            Set<Integer> uniq2 = new HashSet<>(two);

            List<Integer> intersection = new ArrayList<>(Sets.intersection(uniq1, uniq2));

            Collections.sort(intersection);

            final Iterator<Integer> it = SortedIterators.intersection(one.iterator(), two.iterator(), Integer::compareTo);

            final ImmutableList<Integer> result = ImmutableList.copyOf(it);

            Assert.assertEquals("one: " + one + " two: " + two + " intersection: " + intersection, intersection.size(), result.size());

            Assert.assertEquals("one: " + one + " two: " + two + " intersection: " + intersection, intersection, result);
        }
    }

    @Test
    void testExcludeRandom() {

        int attempt = 0;

        while (attempt++ < 200) {
            List<Integer> one = IntStream.generate(() -> RANDOM.nextInt(MAX_VALUE)).limit(RANDOM.nextInt(MAX_VALUE)).boxed().sorted().collect(Collectors.toList());
            List<Integer> two = IntStream.generate(() -> RANDOM.nextInt(MAX_VALUE)).limit(RANDOM.nextInt(MAX_VALUE)).boxed().sorted().collect(Collectors.toList());

            final Set<Integer> uniq1 = new HashSet<>(one);
            final Set<Integer> uniq2 = new HashSet<>(two);

            uniq1.removeIf(uniq2::contains);

            final List<Integer> excluded = new ArrayList<>(uniq1);

            Collections.sort(excluded);

            final Iterator<Integer> it = SortedIterators.exclude(one.iterator(), two.iterator(), Integer::compareTo);

            final ImmutableList<Integer> result = ImmutableList.copyOf(it);

            Assert.assertEquals("one: " + one + " two: " + two + " excluded: " + excluded, excluded.size(), result.size());

            Assert.assertEquals("one: " + one + " two: " + two + " excluded: " + excluded, excluded, result);
        }
    }

    @Test
    void testBuilder() {
        final Iterator<Integer> iterator = SortedIterators.<Integer>builder()
                .comparator(Integer::compareTo)
                .union(List.of(1, 2, 3).iterator())
                .union(List.of(2, 4).iterator())
                .union(List.of(7, 8).iterator())
                .exclude(List.of(2, 3).iterator())
                .exclude(List.of(2, 7).iterator())
                .intersect(List.of(1, 8).iterator())
                .difference(List.of(8, 9).iterator())
                .build();


        final ImmutableList<Integer> result = ImmutableList.copyOf(iterator);

        Assert.assertEquals(List.of(1, 9), result);

    }
}