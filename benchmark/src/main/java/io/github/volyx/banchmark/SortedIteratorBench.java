package io.github.volyx.banchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.ClassloaderProfiler;
import org.openjdk.jmh.profile.DTraceAsmProfiler;
import org.openjdk.jmh.profile.LinuxPerfProfiler;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class SortedIteratorBench {

    /*
     * This sample serves as the profiler overview.
     *
     * JMH has a few very handy profilers that help to understand your benchmarks. While
     * these profilers are not the substitute for full-fledged external profilers, in many
     * cases, these are handy to quickly dig into the benchmark behavior. When you are
     * doing many cycles of tuning up the benchmark code itself, it is important to have
     * a quick turnaround for the results.
     *
     * Use -lprof to list the profilers. There are quite a few profilers, and this sample
     * would expand on a handful of most useful ones. Many profilers have their own options,
     * usually accessible via -prof <profiler-name>:help.
     *
     * Since profilers are reporting on different things, it is hard to construct a single
     * benchmark sample that will show all profilers in action. Therefore, we have a couple
     * of benchmarks in this sample.
     */

    /*
     * ================================ MAPS BENCHMARK ================================
     */

    @State(Scope.Thread)
    @Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
    @Fork(3)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public static class Maps {
        private Map<Integer, Integer> map;

        @Param({"hashmap", "treemap"})
        private String type;

        private int begin;
        private int end;

        @Setup
        public void setup() {
            switch (type) {
                case "hashmap":
                    map = new HashMap<>();
                    break;
                case "treemap":
                    map = new TreeMap<>();
                    break;
                default:
                    throw new IllegalStateException("Unknown type: " + type);
            }

            begin = 1;
            end = 256;
            for (int i = begin; i < end; i++) {
                map.put(i, i);
            }
        }

        @Benchmark
        public void test(Blackhole bh) {
            for (int i = begin; i < end; i++) {
                bh.consume(map.get(i));
            }
        }

        /*
         * ============================== HOW TO RUN THIS TEST: ====================================
         *
         * You can run this test:
         *
         * a) Via the command line:
         *    $ mvn clean install
         *    $ java -jar target/benchmarks.jar JMHSample_35.*Maps -prof stack
         *    $ java -jar target/benchmarks.jar JMHSample_35.*Maps -prof gc
         *
         * b) Via the Java API:
         *    (see the JMH homepage for possible caveats when running from IDE:
         *      http://openjdk.java.net/projects/code-tools/jmh/)
         */

        public static void main(String[] args) throws RunnerException {
            Options opt = new OptionsBuilder()
                    .include(SortedIteratorBench.Maps.class.getSimpleName())
                    .addProfiler(StackProfiler.class)
//                    .addProfiler(GCProfiler.class)
                    .build();

            new Runner(opt).run();
        }
    }
}
