package se.jbee.lusid;

import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import se.jbee.lusid.Coder.Mode;

/** Tests the {@link Coder#encodeLong(long)} and {@link Coder#decodeLong(String)} method pair. */
class CoderLongTest {

  @BeforeAll
  static void setUp() {
    System.setProperty(Coder.SECRET_PROPERTY, "80000089");
  }

  private static final List<Mode> MODES = List.of(Mode.MIXED, Mode.LOWER, Mode.UPPER, Mode.XSAFE);

  @ParameterizedTest
  @ValueSource(longs = {0, 1, 12, 123, 1234, 12345, 123456, 1234567, 12345678})
  void testLong8(long value) {
    assertEncodesInLength(8, 8, value);
  }

  @Test
  void testLong8_Large() {
    assertEncodesInLength(8, 9, 123456789L);
    assertEncodesInLength(8, 10, 1234567890L);
    assertEncodesInLength(8, 11, 12345678901L);
    assertEncodesInLength(8, 11, 123456789012L);
    assertEncodesInLength(8, 13, 1234567890123L);
    assertEncodesInLength(8, 14, 12345678901234L);
    assertEncodesInLength(8, 15, 123456789012345L);
    assertEncodesInLength(8, 16, 1234567890123456L);
    assertEncodesInLength(8, 17, 12345678901234567L);
    assertEncodesInLength(8, 18, 123456789012345678L);
    assertEncodesInLength(8, 19, 1234567890123456789L);
    assertEncodesInLength(8, 19, ~(1L << 63 | 1L << 62 | 1L << 61));
    assertEncodesInLength(8, 20, ~(1L << 63 | 1L << 62 | 1L << 61) + 1);
    assertEncodesInLength(8, 20, Long.MAX_VALUE);
  }

  @Test
  void testLong8_Negative() {
    assertEncodesInLength(8, 8, -1);
  }

  @ParameterizedTest
  @ValueSource(longs = {-0, -1, -12, -123, -1234, -12345, -123456, -1234567})
  void testLong8_Negative(long value) {
    assertEncodesInLength(8, 8, value);
  }

  @Test
  void testLong8_LargeNegative() {
    assertEncodesInLength(8, 9, -12345678L);
    assertEncodesInLength(8, 10, -123456789L);
    assertEncodesInLength(8, 11, -1234567890L);
    assertEncodesInLength(8, 12, -12345678901L);
    assertEncodesInLength(8, 12, -123456789012L);
    assertEncodesInLength(8, 14, -1234567890123L);
    assertEncodesInLength(8, 15, -12345678901234L);
    assertEncodesInLength(8, 16, -123456789012345L);
    assertEncodesInLength(8, 17, -1234567890123456L);
    assertEncodesInLength(8, 18, -12345678901234567L);
    assertEncodesInLength(8, 19, -123456789012345678L);
    assertEncodesInLength(8, 20, -1234567890123456789L);
    assertEncodesInLength(8, 20, ~(1L << 61));
    assertEncodesInLength(8, 20, Long.MIN_VALUE);
  }

  @ParameterizedTest
  @ValueSource(longs = {0, 5, 55, 555, 5555, 5_5555, 55_5555, 555_5555, 5555_5555})
  void testLong(long value) {
    assertEncodesInLength(1, String.valueOf(value).length(), value);
  }

  @Test
  void testLong_2Mil_NoPadding() {
    Coder coder = Coder.of(0L, 1);
    range(0, 1000_000).parallel().forEach(n -> assertEncodesMax20PosNeg(coder, n));
  }

  @Test
  void testLong_2Mil_Padding() {
    Coder coder = Coder.of(0L, 6);
    range(0, 1000_000).parallel().forEach(n -> assertEncodesMax20PosNeg(coder, n));
  }

  @Test
  void testLong_2Mil_IntToLongOverflow() {
    Coder coder = Coder.of(0L, 6);
    LongStream.range(Integer.MAX_VALUE - 500_000, Integer.MAX_VALUE + 500_000L)
        .parallel()
        .forEach(n -> assertEncodesMax20PosNeg(coder, n));
  }

  @Test
  void testLong_2Mil_NegLongToPosOverflow() {
    Coder coder = Coder.of(0L, 6);
    range(0, 2000_000)
        .parallel()
        .forEach(n -> assertEncodesMax20(coder, Long.MIN_VALUE + n - 1000_000L));
  }

  @Test
  void testLong_2Mil_PosLongToNegOverflow() {
    Coder coder = Coder.of(0L, 6);
    range(0, 2000_000)
        .parallel()
        .forEach(n -> assertEncodesMax20(coder, Long.MAX_VALUE - n + 1000_000L));
  }

  private static void assertEncodesInLength(int minLength, int expectedLength, long actualValue) {
    for (Mode m : MODES)
      assertEncodesInLength(Coder.of(0L, minLength, m), expectedLength, actualValue);
  }

  private static void assertEncodesInLength(Coder coder, int expectedLength, long actualValue) {
    assertEquals(
        expectedLength, assertEncodesMax20(coder, actualValue), "unexpected encoding length");
  }

  private static int assertEncodesMax20(Coder coder, long actualValue) {
    String id = coder.encodeLong(actualValue);
    assertTrue(id.length() <= 20, "ID too long");
    assertEquals(actualValue, coder.decodeLong(id), "decoding error");
    return id.length();
  }

  private static void assertEncodesMax20PosNeg(Coder coder, long actualValue) {
    assertEncodesMax20(coder, actualValue);
    assertEncodesMax20(coder, -actualValue);
  }
}
