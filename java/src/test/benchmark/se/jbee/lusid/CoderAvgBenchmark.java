package se.jbee.lusid;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Measures the average time per operation to encode and decode values for a range of different
 * values, here -1mio to +1mio. This is done to "simulate" a more realistic usage than encoding and
 * decoding the same value over and over again.
 *
 * @author Jan Bernitt
 * @author Christian Stein
 */
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 2, time = 3)
@Measurement(
    iterations = 3,
    time = CoderAvgBenchmark.LOOP_COUNT / 100,
    timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class CoderAvgBenchmark {

  private static final Coder MIXED = Coder.of(42, 8);

  public static final int HIGH_VAL = 1_000_000;
  public static final int LOW_VAL = -HIGH_VAL;
  public static final int LOOP_COUNT = 2 * HIGH_VAL;

  private static final String[] LONG_IDS =
      IntStream.range(LOW_VAL, HIGH_VAL).mapToObj(MIXED::encodeLong).toArray(String[]::new);

  private static final String[] INT_IDS =
      IntStream.range(LOW_VAL, HIGH_VAL).mapToObj(MIXED::encodeInt).toArray(String[]::new);

  private static final String[] DOUBLE_IDS =
      IntStream.range(LOW_VAL, HIGH_VAL).mapToObj(MIXED::encodeDouble).toArray(String[]::new);

  private static final String[] FLOAT_IDS =
      IntStream.range(LOW_VAL, HIGH_VAL).mapToObj(MIXED::encodeFloat).toArray(String[]::new);

  @Benchmark
  @OperationsPerInvocation(LOOP_COUNT)
  public void encodeLong(Blackhole bh) {
    for (int i = LOW_VAL; i < HIGH_VAL; i++) bh.consume(MIXED.encodeLong(i));
  }

  @Benchmark
  @OperationsPerInvocation(LOOP_COUNT)
  public void decodeLong(Blackhole bh) {
    for (String id : LONG_IDS) bh.consume(MIXED.decodeLong(id));
  }

  @Benchmark
  @OperationsPerInvocation(LOOP_COUNT)
  public void recodeLong(Blackhole bh) {
    for (int i = LOW_VAL; i < HIGH_VAL; i++) bh.consume(MIXED.decodeLong(MIXED.encodeLong(i)));
  }

  @Benchmark
  @OperationsPerInvocation(LOOP_COUNT)
  public void encodeInt(Blackhole bh) {
    for (int i = LOW_VAL; i < HIGH_VAL; i++) bh.consume(MIXED.encodeInt(i));
  }

  @Benchmark
  @OperationsPerInvocation(LOOP_COUNT)
  public void decodeInt(Blackhole bh) {
    for (String id : INT_IDS) bh.consume(MIXED.decodeInt(id));
  }

  @Benchmark
  @OperationsPerInvocation(LOOP_COUNT)
  public void recodeInt(Blackhole bh) {
    for (int i = LOW_VAL; i < HIGH_VAL; i++) bh.consume(MIXED.decodeInt(MIXED.encodeInt(i)));
  }

  @Benchmark
  @OperationsPerInvocation(LOOP_COUNT)
  public void encodeDouble(Blackhole bh) {
    for (int i = LOW_VAL; i < HIGH_VAL; i++) bh.consume(MIXED.encodeDouble(i));
  }

  @Benchmark
  @OperationsPerInvocation(LOOP_COUNT)
  public void decodeDouble(Blackhole bh) {
    for (String id : DOUBLE_IDS) bh.consume(MIXED.decodeDouble(id));
  }

  @Benchmark
  @OperationsPerInvocation(LOOP_COUNT)
  public void recodeDouble(Blackhole bh) {
    for (int i = LOW_VAL; i < HIGH_VAL; i++) bh.consume(MIXED.decodeDouble(MIXED.encodeDouble(i)));
  }

  @Benchmark
  @OperationsPerInvocation(LOOP_COUNT)
  public void encodeFloat(Blackhole bh) {
    for (int i = LOW_VAL; i < HIGH_VAL; i++) bh.consume(MIXED.encodeFloat(i));
  }

  @Benchmark
  @OperationsPerInvocation(LOOP_COUNT)
  public void decodeFloat(Blackhole bh) {
    for (String id : FLOAT_IDS) bh.consume(MIXED.decodeFloat(id));
  }

  @Benchmark
  @OperationsPerInvocation(LOOP_COUNT)
  public void recodeFloat(Blackhole bh) {
    for (int i = LOW_VAL; i < HIGH_VAL; i++) bh.consume(MIXED.decodeFloat(MIXED.encodeFloat(i)));
  }

  public static void main(String[] args) throws Exception {
    org.openjdk.jmh.Main.main(args);
  }
}
