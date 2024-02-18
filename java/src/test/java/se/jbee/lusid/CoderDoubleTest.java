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

/**
 * Tests the {@link Coder#encodeDouble(double)} and {@link Coder#decodeDouble(String)} method pair.
 */
class CoderDoubleTest {

  @BeforeAll
  static void setUp() {
    System.setProperty(Coder.SECRET_PROPERTY, "1234567");
  }

  private static final List<Mode> MODES = List.of(Mode.MIXED, Mode.LOWER, Mode.UPPER, Mode.XSAFE);

  @ParameterizedTest
  @ValueSource(
      doubles = {
        1d,
        12d,
        123d,
        1234d,
        12345d,
        123456d,
        1234567d,
        12345678d,
        123456789d,
        1234567890d,
        12345678901d,
        123456789012d,
        1234567890123d,
        12345678901234d,
        123456789012345d,
        1234567890123456d,
        12345678901234567d,
        123456789012345678d,
        1234567890123456789d,
        Double.MAX_VALUE
      })
  void testDouble8(double value) {
    assertEncodesInLength(8, 20, value);
  }

  @ParameterizedTest
  @ValueSource(
      doubles = {
        -1d,
        -12d,
        -123d,
        -1234d,
        -12345d,
        -123456d,
        -1234567d,
        -12345678d,
        -123456789d,
        -1234567890d,
        -12345678901d,
        -123456789012d,
        -1234567890123d,
        -12345678901234d,
        -123456789012345d,
        -1234567890123456d,
        -12345678901234567d,
        -123456789012345678d,
        -1234567890123456789d
      })
  void testDouble8_Negative(double value) {
    assertEncodesInLength(8, 20, value);
  }

  @Test
  void testDouble() {
    assertEncodesInLength(1, 1, 0d);
    assertEncodesInLength(8, 20, 5);
    assertEncodesInLength(8, 20, 55);
    assertEncodesInLength(8, 20, 555);
    assertEncodesInLength(8, 20, 5555);
    assertEncodesInLength(8, 20, 5_5555);
    assertEncodesInLength(8, 20, 55_5555);
    assertEncodesInLength(8, 20, 555_5555);
    assertEncodesInLength(8, 20, 5555_5555);
  }

  @Test
  void testDouble_Special() {
    assertEncodesInLength(8, 8, Double.MIN_VALUE);
    assertEncodesInLength(8, 20, Double.POSITIVE_INFINITY);
    assertEncodesInLength(8, 17, Double.NEGATIVE_INFINITY);
    assertEncodesInLength(8, 20, Double.NaN);
  }

  @Test
  void testDouble_2Mil_NoPadding() {
    Coder coder = Coder.of(0L, 1);
    range(0, 1000_000).parallel().forEach(n -> assertEncodesMax20PosNeg(coder, n));
  }

  @Test
  void testDouble_2Mil_Padding() {
    Coder coder = Coder.of(0L, 6);
    range(0, 1000_000).parallel().forEach(n -> assertEncodesMax20PosNeg(coder, n));
  }

  @Test
  void testDouble_2Mil_IntToLongOverflow() {
    Coder coder = Coder.of(0L, 6);
    LongStream.range(Integer.MAX_VALUE - 500_000, Integer.MAX_VALUE + 500_000L)
        .parallel()
        .forEach(n -> assertEncodesMax20PosNeg(coder, n));
  }

  @Test
  void testDouble_2Mil_NegLongToPosOverflow() {
    Coder coder = Coder.of(0L, 6);
    range(0, 2000_000)
        .parallel()
        .forEach(n -> assertEncodesMax20(coder, Long.MIN_VALUE + n - 1000_000L));
  }

  @Test
  void testDouble_2Mil_PosLongToNegOverflow() {
    Coder coder = Coder.of(0L, 6);
    range(0, 2000_000)
        .parallel()
        .forEach(n -> assertEncodesMax20(coder, Long.MAX_VALUE - n + 1000_000L));
  }

  @Test
  void testDouble_2Mil_Fractions() {
    Coder coder = Coder.of(0L, 6);
    range(0, 2000_000).parallel().forEach(n -> assertEncodesMax20(coder, 1d / n));
  }

  private static void assertEncodesInLength(int minLength, int expectedLength, double actualValue) {
    for (Mode m : MODES)
      assertEncodesInLength(Coder.of(0L, minLength, m), expectedLength, actualValue);
  }

  private static void assertEncodesInLength(Coder coder, int expectedLength, double actualValue) {
    assertEquals(
        expectedLength, assertEncodesMax20(coder, actualValue), "unexpected encoding length");
  }

  private static int assertEncodesMax20(Coder coder, double actualValue) {
    String id = coder.encodeDouble(actualValue);
    assertTrue(id.length() <= 20, "ID too long");
    assertEquals(actualValue, coder.decodeDouble(id), "decoding error");
    return id.length();
  }

  private static void assertEncodesMax20PosNeg(Coder coder, double actualValue) {
    assertEncodesMax20(coder, actualValue);
    assertEncodesMax20(coder, -actualValue);
  }
}
