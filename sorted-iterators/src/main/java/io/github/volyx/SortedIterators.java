package io.github.volyx;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;

public final class SortedIterators {

    private SortedIterators() {
    }

    public static <T> SortedIteratorsBuilder<T> builder() {
        return new SortedIteratorsBuilder<>();
    }

    public static <T> Iterator<T> union(
            Iterable<? extends Iterator<T>> iterators, Comparator<T> comparator) {
        checkNotNull(iterators, "iterators");
        checkNotNull(comparator, "comparator");

        final List<Iterator<T>> iteratorList = new ArrayList<>();

        for (Iterator<T> iterator : iterators) {
            iteratorList.add(Iterators.filter(Iterators.filter(iterator, new CheckSortedPredicate<>(comparator)), new DeDuplicatePredicate<>()));
        }

        final UnmodifiableIterator<T> mergedIterator = Iterators.mergeSorted(iteratorList, comparator);
        return Iterators.filter(Iterators.filter(mergedIterator, new CheckSortedPredicate<>(comparator)), new DeDuplicatePredicate<>());
    }

    public static <T> Iterator<T> intersection(Iterator<T> sourceIterator, Iterator<T> targetIterator,
                                               Comparator<T> comparator) {
        checkNotNull(sourceIterator, "sourceIterator");
        checkNotNull(targetIterator, "targetIterator");
        checkNotNull(comparator, "comparator");

        if (!sourceIterator.hasNext()) {
            return ImmutableList.<T>of().iterator();
        }

        if (!targetIterator.hasNext()) {
            return ImmutableList.<T>of().iterator();
        }

        return new IntersectionIterator<>(sourceIterator, targetIterator, comparator);
    }

    public static <T> Iterator<T> exclude(Iterator<T> sourceIterator,
                                          Iterator<T> targetIterator,
                                          Comparator<T> comparator) {
        checkNotNull(sourceIterator, "sourceIterator");
        checkNotNull(targetIterator, "targetIterator");
        checkNotNull(comparator, "comparator");

        if (!sourceIterator.hasNext()) {
            return ImmutableList.<T>of().iterator();
        }

        if (!targetIterator.hasNext()) {
            return Iterators.filter(sourceIterator, new DeDuplicatePredicate<>());
        }

        return new DiffMergingIterator<>(sourceIterator, targetIterator, comparator);
    }

    public static <T> Iterator<T> difference(Iterator<T> sourceIterator,
                                             Iterator<T> targetIterator,
                                             Comparator<T> comparator) {
        checkNotNull(sourceIterator, "sourceIterator");
        checkNotNull(targetIterator, "targetIterator");
        checkNotNull(comparator, "comparator");

        if (!sourceIterator.hasNext()) {
            return Iterators.filter(targetIterator, new DeDuplicatePredicate<>());
        }

        if (!targetIterator.hasNext()) {
            return Iterators.filter(sourceIterator, new DeDuplicatePredicate<>());
        }

        return new DifferenceIterator<>(
                Iterators.filter(sourceIterator, new DeDuplicatePredicate<>()),
                Iterators.filter(targetIterator, new DeDuplicatePredicate<>()),
                comparator);
    }

    public static <T> void differenceConsumer(Iterator<T> sourceIterator, Iterator<T> targetIterator, Comparator<T> comparator, Consumer<T> addConsumer, Consumer<T> removeConsumer) {
        if (!sourceIterator.hasNext() && !targetIterator.hasNext()) {
            return;
        }
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

    private static <T> void differenceSortedBoth(Iterator<T> sourceIterator,
                                                 Iterator<T> targetIterator,
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

    public static <T> void symmetricDifference(Iterator<T> sourceIterator,
                                               Iterator<T> targetIterator,
                                               Comparator<T> comparator,
                                               Consumer<T> mergeConsumer) {

        differenceConsumer(sourceIterator, targetIterator, comparator, mergeConsumer, mergeConsumer);
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

    private static class DiffMergingIterator<T> implements Iterator<T> {

        private final Iterator<? extends T> sourceIterator;
        private final Iterator<? extends T> targetIterator;
        private final Comparator<? super T> itemComparator;

        private T line1 = null;
        private T line2;

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

    private static class DifferenceIterator<T> implements Iterator<T> {

        private final Iterator<? extends T> sourceIterator;
        private final Iterator<? extends T> targetIterator;
        private final Comparator<? super T> itemComparator;


        private T val1 = null;
        private T lastVal1 = null;
        private T val2 = null;
        private T lastVal2 = null;
        private T nextValue = null;

        public DifferenceIterator(
                Iterator<? extends T> sourceIterator,
                Iterator<? extends T> targetIterator,
                final Comparator<? super T> itemComparator) {

            this.sourceIterator = Iterators.peekingIterator(Iterators.filter(Iterators.filter(sourceIterator, new CheckSortedPredicate<>(itemComparator)), new DeDuplicatePredicate<>()));
            this.targetIterator = Iterators.peekingIterator(Iterators.filter(Iterators.filter(targetIterator, new CheckSortedPredicate<>(itemComparator)), new DeDuplicatePredicate<>()));
            this.itemComparator = itemComparator;


            val1 = sourceIterator.hasNext() ? sourceIterator.next() : null;
            val2 = targetIterator.hasNext() ? targetIterator.next() : null;

            adjust();
        }

        public boolean hasNext() {
            if (nextValue == null) return false;
            return true;
        }

        public T next() {
            T toRet = nextValue;
            adjust();
            return toRet;
        }

        /*
            1, 3, 4, 6, 7
            4

              1, 3, 6, 7
         */
        private void adjust() {
            nextValue = null;
            while (true) {
                if (val1 == null && val2 == null) break;
                final int compare;
                if (val1 == null) {
                    compare = itemComparator.compare(lastVal1, val2);
                    if (compare == 0) {
                        lastVal2 = val2;
                        val2 = targetIterator.hasNext() ? targetIterator.next() : null;
                    } else if (compare < 0) {
                        nextValue = val2;
                        lastVal2 = val2;
                        val2 = targetIterator.hasNext() ? targetIterator.next() : null;
                        break;
                    } else {
                        throw new UnsupportedOperationException();
                    }
                } else if (val2 == null) {
                    compare = itemComparator.compare(val1, lastVal2);
                    if (compare == 0) {
                        lastVal1 = val1;
                        val1 = sourceIterator.hasNext() ? sourceIterator.next() : null;
                    } else if (compare > 0) {
                        nextValue = val1;
                        lastVal1 = val1;
                        val1 = sourceIterator.hasNext() ? sourceIterator.next() : null;
                        break;
                    } else {
                        throw new UnsupportedOperationException();
                    }
                } else {
                    compare = itemComparator.compare(val1, val2);
                    if (compare == 0) {
                        lastVal1 = val1;
                        val1 = sourceIterator.hasNext() ? sourceIterator.next() : null;
                        lastVal2 = val2;
                        val2 = targetIterator.hasNext() ? targetIterator.next() : null;
                    } else {
                        if (compare < 0) { // val1 < val2
                            nextValue = val1;
                            lastVal1 = val1;
                            val1 = sourceIterator.hasNext() ? sourceIterator.next() : null;
                            break;
                        } else {
                            nextValue = val2;
                            lastVal2 = val2;
                            val2 = targetIterator.hasNext() ? targetIterator.next() : null;
                            break;
                        }
                    }
                }
            }
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

    static final class DeDuplicatePredicate<T> implements Predicate<T> {

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

    private static class IntersectionIterator<T> implements Iterator<T> {


        private final Iterator<? extends T> sourceIterator;
        private final Iterator<? extends T> targetIterator;
        private final Comparator<? super T> itemComparator;

        private T nextVal = null;

        public IntersectionIterator(
                Iterator<? extends T> sourceIterator,
                Iterator<? extends T> targetIterator,
                final Comparator<? super T> itemComparator) {

            this.sourceIterator = Iterators.peekingIterator(Iterators.filter(Iterators.filter(sourceIterator, new CheckSortedPredicate<>(itemComparator)), new DeDuplicatePredicate<>()));
            this.targetIterator = Iterators.peekingIterator(Iterators.filter(Iterators.filter(targetIterator, new CheckSortedPredicate<>(itemComparator)), new DeDuplicatePredicate<>()));
            this.itemComparator = itemComparator;

            adjust();
        }

        public boolean hasNext() {
            if (nextVal == null) return false;
            return true;
        }

        public T next() {
            T toRet = nextVal;
            adjust();
            return toRet;
        }

        private void adjust() {
            nextVal = null;
            T val1 = sourceIterator.hasNext() ? sourceIterator.next() : null;
            T val2 = targetIterator.hasNext() ? targetIterator.next() : null;
            while (true) {
                if (val1 == null || val2 == null) break;
                if (val1 == val2) {
                    nextVal = val1;
                    break;
                } else if (itemComparator.compare(val1, val2) < 0) { // val1 < val2
                    if (sourceIterator.hasNext()) {
                        val1 = sourceIterator.next();
                    } else break;
                } else {
                    if (targetIterator.hasNext()) {
                        val2 = targetIterator.next();
                    } else break;
                }
            }
        }
    }

    public static class SortedIteratorsBuilder<T> {

        private Comparator<T> comparator;
        private final List<Iterator<T>> unionIterators = new ArrayList<>();
        private final List<Iterator<T>> excludeIterators = new ArrayList<>();
        private final List<Iterator<T>> intersectIterators = new ArrayList<>();
        private final List<Iterator<T>> differenceIterators = new ArrayList<>();

        public SortedIteratorsBuilder<T> comparator(Comparator<T> comparator) {
            this.comparator = comparator;
            return this;
        }

        public SortedIteratorsBuilder<T> union(Iterator<T> unionIterator) {
            this.unionIterators.add(unionIterator);
            return this;
        }

        public SortedIteratorsBuilder<T> exclude(Iterator<T> excludeIterator) {
            this.excludeIterators.add(excludeIterator);
            return this;
        }

        public SortedIteratorsBuilder<T> intersect(Iterator<T> intersectIterator) {
            this.intersectIterators.add(intersectIterator);
            return this;
        }

        public SortedIteratorsBuilder<T> difference(Iterator<T> differenceIterator) {
            this.differenceIterators.add(differenceIterator);
            return this;
        }

        public Iterator<T> build() {
            Iterator<T> it = SortedIterators.union(unionIterators, comparator);

            if (!excludeIterators.isEmpty()) {
                it = SortedIterators.exclude(it, SortedIterators.union(excludeIterators, comparator), comparator);
            }

            if (!intersectIterators.isEmpty()) {
                Iterator<T> iterator = intersectIterators.get(0);

                for (int i = 1; i < intersectIterators.size(); i++) {
                    iterator = SortedIterators.intersection(iterator, intersectIterators.get(i), comparator);
                }
                it = SortedIterators.intersection(it, iterator, comparator);
            }

            if (!differenceIterators.isEmpty()) {
                Iterator<T> iterator = differenceIterators.get(0);

                for (int i = 1; i < differenceIterators.size(); i++) {
                    iterator = SortedIterators.difference(iterator, intersectIterators.get(i), comparator);
                }
                it = SortedIterators.difference(it, iterator, comparator);
            }

            return it;
        }
    }
}
