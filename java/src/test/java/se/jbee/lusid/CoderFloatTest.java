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

/** Tests the {@link Coder#encodeFloat(float)} and {@link Coder#decodeFloat(String)} method pair. */
class CoderFloatTest {

  @BeforeAll
  static void setUp() {
    System.setProperty(Coder.SECRET_PROPERTY, "-89632478");
  }

  private static final List<Mode> MODES =
      List.of(Mode.MIXED, Mode.LOWER, Mode.UPPER, Mode.XSAFE, Mode.SHAPE);

  @ParameterizedTest
  @ValueSource(
      floats = {
        1f,
        12f,
        123f,
        1234f,
        12345f,
        123456f,
        1234567f,
        12345678f,
        123456789f,
        1234567890f,
        12345678901f,
        123456789012f,
        1234567890123f,
        12345678901234f,
        123456789012345f,
        1234567890123456f,
        12345678901234567f,
        123456789012345678f,
        1234567890123456789f,
        Float.MAX_VALUE
      })
  void testFloat8(float value) {
    assertEncodesInLength(8, 10, value);
  }

  @ParameterizedTest
  @ValueSource(
      floats = {
        -1f,
        -12f,
        -123f,
        -1234f,
        -12345f,
        -123456f,
        -1234567f,
        -12345678f,
        -123456789f,
        -1234567890f,
        -12345678901f,
        -123456789012f,
        -1234567890123f,
        -12345678901234f,
        -123456789012345f,
        -1234567890123456f,
        -12345678901234567f,
        -123456789012345678f,
        -1234567890123456789f
      })
  void testFloat8_Negative(float value) {
    assertEncodesInLength(8, 11, value);
  }

  @Test
  void testFloat() {
    assertEncodesInLength(1, 1, 0);
    assertEncodesInLength(8, 10, 5);
    assertEncodesInLength(8, 10, 55);
    assertEncodesInLength(8, 10, 555);
    assertEncodesInLength(8, 10, 5555);
    assertEncodesInLength(8, 10, 5_5555);
    assertEncodesInLength(8, 10, 55_5555);
    assertEncodesInLength(8, 10, 555_5555);
    assertEncodesInLength(8, 10, 5555_5555);
  }

  @Test
  void testFloat_Special() {
    assertEncodesInLength(8, 8, Float.MIN_VALUE);
    assertEncodesInLength(8, 10, Float.POSITIVE_INFINITY);
    assertEncodesInLength(8, 8, Float.NEGATIVE_INFINITY);
    assertEncodesInLength(8, 10, Float.NaN);
  }

  @Test
  void testFloat_2Mil_NoPadding() {
    Coder coder = Coder.of(0L, 1);
    range(0, 1000_000).parallel().forEach(n -> assertEncodesMax11PosNeg(coder, n));
  }

  @Test
  void testFloat_2Mil_Padding() {
    Coder coder = Coder.of(0L, 6);
    range(0, 1000_000).parallel().forEach(n -> assertEncodesMax11PosNeg(coder, n));
  }

  @Test
  void testFloat_2Mil_IntToLongOverflow() {
    Coder coder = Coder.of(0L, 6);
    LongStream.range(Integer.MAX_VALUE - 500_000, Integer.MAX_VALUE + 500_000L)
        .parallel()
        .forEach(n -> assertEncodesMax11PosNeg(coder, n));
  }

  @Test
  void testFloat_2Mil_NegLongToPosOverflow() {
    Coder coder = Coder.of(0L, 6);
    range(0, 2000_000)
        .parallel()
        .forEach(n -> assertEncodesMax11(coder, Long.MIN_VALUE + n - 1000_000L));
  }

  @Test
  void testFloat_2Mil_PosLongToNegOverflow() {
    Coder coder = Coder.of(0L, 6);
    range(0, 2000_000)
        .parallel()
        .forEach(n -> assertEncodesMax11(coder, Long.MAX_VALUE - n + 1000_000L));
  }

  @Test
  void testFloat_2Mil_Fractions() {
    Coder coder = Coder.of(0L, 6);
    for (long n = 0; n < 2000_000L; n++) assertEncodesMax11(coder, 1f / n);
  }

  private static void assertEncodesInLength(int minLength, int expectedLength, float actualValue) {
    for (Mode m : MODES)
      assertEncodesInLength(Coder.of(0L, minLength, m), expectedLength, actualValue);
  }

  private static void assertEncodesInLength(Coder coder, int expectedLength, float actualValue) {
    assertEquals(
        expectedLength, assertEncodesMax11(coder, actualValue), "unexpected encoding length");
  }

  private static int assertEncodesMax11(Coder coder, float actualValue) {
    String id = coder.encodeFloat(actualValue);
    assertTrue(id.length() <= 11, "ID too long");
    assertEquals(actualValue, coder.decodeFloat(id), "decoding error");
    return id.length();
  }

  private static void assertEncodesMax11PosNeg(Coder coder, float actualValue) {
    assertEncodesMax11(coder, actualValue);
    assertEncodesMax11(coder, -actualValue);
  }
}
