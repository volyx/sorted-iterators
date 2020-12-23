package io.github.volyx.benchmark;

import com.google.common.collect.Sets;
import io.github.volyx.SortedIterators;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@State(Scope.Thread)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class SortedIteratorBench {
    private TreeSet<Integer> set1;
    private TreeSet<Integer> set2;

    @Setup
    public void setup() {
        set1 = IntStream.range(1, 17_000_000).boxed().sorted().collect(Collectors.toCollection(TreeSet::new));
        set2 = IntStream.range(1, 17_000_000).boxed().sorted().collect(Collectors.toCollection(TreeSet::new));
        set1.removeIf(i -> i > 100 && i < 200);
        set2.removeIf(i -> i > 1000 && i < 2000);
    }

    @Benchmark
    public void testUnion(Blackhole blackhole) {
        final Iterator<Integer> iterator = new TreeSet<Integer>(Sets.union(set1, set2)).iterator();
        while (iterator.hasNext()) {
            blackhole.consume(iterator.next());
        }
    }

    @Benchmark
    public void testUnionIterator(Blackhole blackhole) {
        final Iterator<Integer> iterator = SortedIterators.union(List.of(set1.iterator(), set2.iterator()), Integer::compare);
        while (iterator.hasNext()) {
            blackhole.consume(iterator.next());
        }
    }

    @Benchmark
    public void testDifference(Blackhole blackhole) {
        final Iterator<Integer> iterator = new TreeSet<Integer>(Sets.symmetricDifference(set1, set2)).iterator();
        while (iterator.hasNext()) {
            blackhole.consume(iterator.next());
        }
    }

    @Benchmark
    public void testDifferenceIterator(Blackhole blackhole) {
        final Iterator<Integer> iterator = SortedIterators.difference(set1.iterator(), set2.iterator(), Integer::compare);
        while (iterator.hasNext()) {
            blackhole.consume(iterator.next());
        }
    }

    @Benchmark
    public void testIntersection(Blackhole blackhole) {
        final Iterator<Integer> iterator = new TreeSet<Integer>(Sets.intersection(set1, set2)).iterator();
        while (iterator.hasNext()) {
            blackhole.consume(iterator.next());
        }
    }

    @Benchmark
    public void testIntersectionIterator(Blackhole blackhole) {
        final Iterator<Integer> iterator = SortedIterators.intersection(set1.iterator(), set2.iterator(), Integer::compare);
        while (iterator.hasNext()) {
            blackhole.consume(iterator.next());
        }
    }

    @Benchmark
    public void testExclude(Blackhole blackhole) {
        set1.removeIf(set2::contains);
        final Iterator<Integer> iterator = set1.iterator();
        while (iterator.hasNext()) {
            blackhole.consume(iterator.next());
        }
    }

    @Benchmark
    public void testExcludeIterator(Blackhole blackhole) {
        final Iterator<Integer> iterator = SortedIterators.exclude(set1.iterator(), set2.iterator(), Integer::compare);
        while (iterator.hasNext()) {
            blackhole.consume(iterator.next());
        }
    }

    public static void main(String[] args) throws RunnerException, IOException {

        final ChainedOptionsBuilder optionsBuilder = new OptionsBuilder()
                .include(SortedIteratorBench.class.getSimpleName())
                .addProfiler(StackProfiler.class)
                .addProfiler(GCProfiler.class);

        String reportDir = System.getProperty("perfReportDir");
        if (reportDir != null) {
            String filePath = reportDir + "/" + SortedIteratorBench.class.getSimpleName() + new Date() + ".json";
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            } else {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            optionsBuilder
                    .resultFormat(ResultFormatType.JSON)
                    .result(filePath);
        }

        Options opt = optionsBuilder
                .build();

        new Runner(opt).run();
    }
}
