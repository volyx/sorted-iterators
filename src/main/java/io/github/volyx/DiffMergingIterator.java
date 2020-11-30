package io.github.volyx;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.UnmodifiableIterator;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

final class DiffMergingIterator<T> extends UnmodifiableIterator<T> {

    private final PeekingIterator<? extends T> sourceIterator;
    private final PeekingIterator<? extends T> targetIterator;
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

        if (targetIterator.hasNext()) {
            line2 = this.targetIterator.peek();
        }
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
