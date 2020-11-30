package io.github.volyx;

import com.google.common.base.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Comparator;

final class CheckSortedPredicate<T> implements Predicate<T> {

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
