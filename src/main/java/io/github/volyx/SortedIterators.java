package io.github.volyx;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.UnmodifiableIterator;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
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

	public static void symmetricDifference(File source, File target, Consumer<String> addConsumer) {

		try (BufferedReader reader1 = new BufferedReader(new FileReader(source));
			 BufferedReader reader2 = new BufferedReader(new FileReader(target))) {

			String line1 = reader1.readLine();
			String line2 = reader2.readLine();

			String prevLine1 = "";
			String prevLine2 = "";

			while (line1 != null || line2 != null) {
				if (line1 == null) {
					addConsumer.accept(line2);
					prevLine2 = line2;
					line2 = reader2.readLine();
					continue;
				} else if (line2 == null) {
					addConsumer.accept(line1);
					prevLine1 = line1;
					line1 = reader1.readLine();
					continue;
				}

				// remove 01
				//
				// 01234_
				// __2_45

				line1 = line1.toUpperCase(Locale.ENGLISH);
				line2 = line2.toUpperCase(Locale.ENGLISH);

				Preconditions.checkArgument(line1.compareTo(prevLine1) >= 0, "sourceBufferedReader is not sorted");
				Preconditions.checkArgument(line2.compareTo(prevLine2) >= 0, "targetBufferedReader is not sorted");

				prevLine1 = line1;
				prevLine2 = line2;

				final int compare = line1.compareTo(line2);
				if (compare == 0) {
					line1 = reader1.readLine();
					line2 = reader2.readLine();
				} else if (compare < 0) {
					addConsumer.accept(line1);
					prevLine1 = line1;
					line1 = reader1.readLine();
				} else {
					addConsumer.accept(line2);
					prevLine2 = line2;
					line2 = reader2.readLine();
				}
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private static class DeDuplicateConsumer<T> implements Consumer<T> {
		private T prev;
		private final Consumer<T> delegate;

		public DeDuplicateConsumer(Consumer<T> delegate) {
			this.delegate = delegate;
		}

		@Override
		public void accept(T t) {

			if (prev == null) {
				delegate.accept(t);
				prev = t;
			} else {
				if (!Objects.equals(prev, t)) {
					prev = t;
					delegate.accept(t);
				}
			}

		}

		@Override
		public Consumer<T> andThen(Consumer<? super T> after) {
			return delegate.andThen(after);
		}
	}

	private static class DiffMergingIterator<T> extends UnmodifiableIterator<T> {

		private final Iterator<? extends T> sourceIterator;
		private final Iterator<? extends T> targetIterator;
		private final Comparator<? super T> itemComparator;

		private T line1 = null;
		private T line2 = null;

		public DiffMergingIterator(
				Iterator<? extends T> sourceIterator,
				Iterator<? extends T> targetIterator,
				final Comparator<? super T> itemComparator) {

			this.sourceIterator = Iterators.peekingIterator(Iterators.filter(Iterators.filter(sourceIterator, new CheckSortedPredicate<>(itemComparator)), new DeDuplicatePredicate<>()));
			this.targetIterator = Iterators.peekingIterator(Iterators.filter(Iterators.filter(targetIterator, new CheckSortedPredicate<>(itemComparator)), new DeDuplicatePredicate<>()));
			this.itemComparator = itemComparator;

			line2 = this.targetIterator.next();
		}

		@Override
		public boolean hasNext() {
			// (1, 2, 3, 4, 5);
			// (1, 2, 4, 5);

			if (line1 != null) {
				return true;
			}

			while (sourceIterator.hasNext()) {
				line1 = sourceIterator.next();
				int compare = itemComparator.compare(line1, line2);
				if (compare == 0) {
					//nothing
				} else if (compare < 0) {
					return true;
				} else {
					while (targetIterator.hasNext()) {
						line2 = targetIterator.next();
						if (itemComparator.compare(line2, line1) < 0) {

						} else {
							break;
						}
					}
					compare = itemComparator.compare(line1, line2);
					if (compare < 0) {
						return true;
					} else if (!targetIterator.hasNext() && compare > 0) {
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public T next() {

			if (line1 == null) throw new NoSuchElementException("no element");
			T local = line1;
			line1 = null;
			return local;
		}
	}

	private static final class CheckSortedPredicate<T> implements Predicate<T> {

		private final Comparator<? super T> itemComparator;

		CheckSortedPredicate(Comparator<? super T> itemComparator) {
			this.itemComparator = itemComparator;
		}

		private T prev;
		@Override
		public boolean apply(@Nullable T t) {
			if (prev == null) {
				prev = t;
			} else {
				int compare = itemComparator.compare(prev, t);
				if (compare > 0) {
					throw new IllegalStateException("sorted predicate failed");
				}
			}
			return true;
		}

	}

	private static final class DeDuplicatePredicate<T> implements Predicate<T> {

		private T prev;

		@Override
		public boolean apply(@Nullable T t) {
			if (prev == null) {
				prev = t;
				return true;
			} else {
				if (!Objects.equals(prev, t)) {
					prev = t;
					return true;
				}
			}
			return false;
		}
	}
}
