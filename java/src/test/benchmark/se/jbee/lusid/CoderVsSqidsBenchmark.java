package se.jbee.lusid;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.sqids.Sqids;

import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * Benchmark to compare average time for encoding and decoding numbers between 0 and 1mio
 * with Sqids and Lusid.
 *
 * @author Jan Bernitt
 */
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 2, time = 3)
@Measurement(
    iterations = 3,
    time = CoderAvgBenchmark.LOOP_COUNT / 100,
    timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class CoderVsSqidsBenchmark {

  private static final Coder MIXED_8 = Coder.of(42, 8);
  private static final Sqids SQIDS_8 = Sqids.builder().minLength(8).build();

  private static final Coder MIXED_1 = Coder.of(42, 1);
  private static final Sqids SQIDS_1 = Sqids.builder().minLength(1).build();

  public static final int LOOP_COUNT = 1_000_000;

  @Benchmark
  @OperationsPerInvocation(LOOP_COUNT)
  public void recodeLongLusid8(Blackhole bh) {
    for (int i = 0; i < LOOP_COUNT; i++) bh.consume(MIXED_8.decodeLong(MIXED_8.encodeLong(i)));
  }

  @Benchmark
  @OperationsPerInvocation(LOOP_COUNT)
  public void recodeLongSqids8(Blackhole bh) {
    for (int i = 0; i < LOOP_COUNT; i++) bh.consume(SQIDS_8.decode(SQIDS_8.encode(List.of((long) i))));
  }

  @Benchmark
  @OperationsPerInvocation(LOOP_COUNT)
  public void recodeLongLusid(Blackhole bh) {
    for (int i = 0; i < LOOP_COUNT; i++) bh.consume(MIXED_1.decodeLong(MIXED_1.encodeLong(i)));
  }

  @Benchmark
  @OperationsPerInvocation(LOOP_COUNT)
  public void recodeLongSqids(Blackhole bh) {
    for (int i = 0; i < LOOP_COUNT; i++) bh.consume(SQIDS_1.decode(SQIDS_1.encode(List.of((long) i))));
  }

  public static void main(String[] args) throws Exception {
    Options opt =
        new OptionsBuilder().include(CoderVsSqidsBenchmark.class.getSimpleName()).forks(1).build();
    new Runner(opt).run();
  }
}
