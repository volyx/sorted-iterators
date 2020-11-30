package io.github.volyx;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.UnmodifiableIterator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

public final class SortedIterators {

	private SortedIterators() {}

	public static <T> UnmodifiableIterator<T> diffSorted(Iterator<T> sourceIterator,
														 Iterator<T> targetIterator,
														 Comparator<T> comparator) {
		checkNotNull(sourceIterator, "sourceIterator");
		checkNotNull(targetIterator, "targetIterator");
		checkNotNull(comparator, "comparator");

		if (!targetIterator.hasNext()) {
			return Iterators.filter(sourceIterator, new DeDuplicatePredicate<>());
		}

		return new DiffMergingIterator<>(sourceIterator, targetIterator, comparator);
	}

	public static <T> UnmodifiableIterator<T> mergeSorted(
			Iterable<? extends Iterator<T>> iterators, Comparator<T> comparator) {
		checkNotNull(iterators, "iterators");
		checkNotNull(comparator, "comparator");

		final List<Iterator<T>> iteratorList = new ArrayList<>();

		for (Iterator<T> iterator : iterators) {
			iteratorList.add(Iterators.filter(Iterators.filter(iterator, new CheckSortedPredicate<>(comparator)), new DeDuplicatePredicate<>()));
		}

		return Iterators.mergeSorted(iteratorList, comparator);
	}


	public static <T> void diffSortedBoth(Iterator<T> sourceIterator, Iterator<T> targetIterator, Comparator<T> comparator, Consumer<T> addConsumer, Consumer<T> removeConsumer) {
		if (!sourceIterator.hasNext() && !targetIterator.hasNext()) {
			return;
		}
		// 10
		//
		if (sourceIterator.hasNext() && !targetIterator.hasNext()) {
			Iterator<T> it = Iterators.filter(sourceIterator, new DeDuplicatePredicate<>());
			while (it.hasNext()) {
				removeConsumer.accept(it.next());
			}
 			return;
		}

		if (!sourceIterator.hasNext() && targetIterator.hasNext()) {
			Iterator<T> it = Iterators.filter(targetIterator, new DeDuplicatePredicate<>());
			while (it.hasNext()) {
				addConsumer.accept(it.next());
			}
			return;
		}

		differenceSortedBoth(Iterators.peekingIterator(Iterators.filter(sourceIterator, new DeDuplicatePredicate<>())),
				Iterators.peekingIterator(Iterators.filter(targetIterator, new DeDuplicatePredicate<>())),
				comparator,
				new DeDuplicateConsumer<>(addConsumer),
				new DeDuplicateConsumer<>(removeConsumer));
	}

	private static <T> void differenceSortedBoth(PeekingIterator<T> sourceIterator,
												 PeekingIterator<T> targetIterator,
												 Comparator<T> comparator,
												 Consumer<T> addConsumer,
												 Consumer<T> removeConsumer) {
		T line1 = sourceIterator.next();
		T line2 = targetIterator.next();
		int compare;
		if (!sourceIterator.hasNext() && !targetIterator.hasNext()) {
			compare = comparator.compare(line1, line2);
			if (compare != 0) {
				addConsumer.accept(line2);
				removeConsumer.accept(line1);
			}
			return;
		}

		//	2
		// 	4, 6, 7, 9

		T lastEqual = null;

		while (sourceIterator.hasNext() || targetIterator.hasNext()) {

			compare = comparator.compare(line1, line2);
			if (compare == 0) {
				lastEqual = line1;
				if (targetIterator.hasNext()) {
					line2 = targetIterator.next();
				} else {
					while (sourceIterator.hasNext()) {
						line1 = sourceIterator.next();
						removeConsumer.accept(line1);
					}
					break;
				}
				if (sourceIterator.hasNext()) {
					line1 = sourceIterator.next();
				} else {
					addConsumer.accept(line2);
					while (targetIterator.hasNext()) {
						line2 = targetIterator.next();
						addConsumer.accept(line2);
					}
					break;
				}

				if (!sourceIterator.hasNext() && !targetIterator.hasNext()) {
					compare = comparator.compare(line1, line2);
					if (compare != 0) {
						addConsumer.accept(line2);
						removeConsumer.accept(line1);
					}
				}
 			} else if (compare < 0) {
				removeConsumer.accept(line1);
				if (sourceIterator.hasNext()) {
					line1 = sourceIterator.next();
				} else {
					addConsumer.accept(line2);
					while (targetIterator.hasNext()) {
						line2 = targetIterator.next();
						addConsumer.accept(line2);
					}
				}
			} else {
				addConsumer.accept(line2);
				if (targetIterator.hasNext()) {
					line2 = targetIterator.next();
				} else {
					removeConsumer.accept(line1);
					while (sourceIterator.hasNext()) {
						line1 = sourceIterator.next();
						removeConsumer.accept(line1);
					}
				}
			}
		}

		compare = comparator.compare(line1, line2);

		if (compare != 0) {
			if (lastEqual != null && comparator.compare(line1, lastEqual) == 0) {

			} else {
				removeConsumer.accept(line1);
			}
			if (lastEqual != null && comparator.compare(line2, lastEqual) == 0) {

			} else {
				addConsumer.accept(line2);
			}
		}
	}
}
