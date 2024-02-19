package se.jbee.lusid;

import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import se.jbee.lusid.Coder.Mode;

/** Tests the {@link Coder#encodeInt(int)} and {@link Coder#decodeInt(String)} method pair. */
class CoderIntTest {

  @BeforeAll
  static void setUp() {
    System.setProperty(Coder.SECRET_PROPERTY, "42424242");
  }

  private static final List<Mode> MODES =
      List.of(Mode.MIXED, Mode.LOWER, Mode.UPPER, Mode.XSAFE, Mode.SHAPE);

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 12, 123, 1234, 12345, 123456, 1234567, 12345678})
  void testInt8(int value) {
    assertEncodesInLength(8, 8, value);
  }

  @Test
  void testInt8_Large() {
    assertEncodesInLength(8, 9, 123456789);
    assertEncodesInLength(8, 10, 1234567890);
    assertEncodesInLength(8, 10, Integer.MAX_VALUE);
  }

  @ParameterizedTest
  @ValueSource(ints = {-0, -1, -12, -123, -1234, -12345, -123456, -1234567})
  void testInt8_Negative(int value) {
    assertEncodesInLength(8, 8, value);
  }

  @Test
  void testInt8_LargeNegative() {
    assertEncodesInLength(8, 9, -12345678);
    assertEncodesInLength(8, 10, -123456789);
    assertEncodesInLength(8, 11, -1234567890);
    assertEncodesInLength(8, 11, Integer.MIN_VALUE);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 5, 55, 555, 5555, 5_5555, 55_5555, 555_5555, 5555_5555})
  void testInt(int value) {
    assertEncodesInLength(1, String.valueOf(value).length(), value);
  }

  @Test
  void testInt_2Mil_NoPadding() {
    Coder coder = Coder.of(0L, 1);
    range(0, 1000_000).parallel().forEach(n -> assertEncodesMax11PosNeg(coder, n));
  }

  @Test
  void testInt_2Mil_Padding() {
    Coder coder = Coder.of(0L, 6);
    range(0, 1000_000).parallel().forEach(n -> assertEncodesMax11PosNeg(coder, n));
  }

  @Test
  void testInt_2Mil_NegLongToPosOverflow() {
    Coder coder = Coder.of(0L, 6);
    range(0, 2000_000)
        .parallel()
        .forEach(n -> assertEncodesMax11(coder, Integer.MIN_VALUE + n - 1000_000));
  }

  @Test
  void testInt_2Mil_PosLongToNegOverflow() {
    Coder coder = Coder.of(0L, 6);
    range(0, 2000_000)
        .parallel()
        .forEach(n -> assertEncodesMax11(coder, Integer.MAX_VALUE - n + 1000_000));
  }

  private static void assertEncodesInLength(int minLength, int expectedLength, int actualValue) {
    for (Mode m : MODES)
      assertEncodesInLength(Coder.of(0L, minLength, m), expectedLength, actualValue);
  }

  private static void assertEncodesInLength(Coder coder, int expectedLength, int actualValue) {
    assertEquals(
        expectedLength, assertEncodesMax11(coder, actualValue), "unexpected encoding length");
  }

  private static int assertEncodesMax11(Coder coder, int actualValue) {
    String id = coder.encodeInt(actualValue);
    assertTrue(id.length() <= 11, "ID too long");
    assertEquals(actualValue, coder.decodeInt(id), "decoding error");
    return id.length();
  }

  private static void assertEncodesMax11PosNeg(Coder coder, int actualValue) {
    assertEncodesMax11(coder, actualValue);
    assertEncodesMax11(coder, -actualValue);
  }
}
