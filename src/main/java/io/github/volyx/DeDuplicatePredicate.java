package io.github.volyx;

import com.google.common.base.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

public final class DeDuplicatePredicate<T> implements Predicate<T> {

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
