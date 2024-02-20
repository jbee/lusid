package se.jbee.lusid;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests the array encode/decode method pairs.
 *
 * <ul>
 *   <li>{@link Coder#encodeLongs(long...)} and {@link Coder#decodeLongs(String)}
 *   <li>{@link Coder#encodeInts(int...)} and {@link Coder#decodeInts(String)}
 *   <li>{@link Coder#encodeDoubles(double...)} and {@link Coder#decodeDoubles(String)}
 * </ul>
 */
class CoderArrayTest {

  @BeforeAll
  static void setUp() {
    System.setProperty(Coder.SECRET_PROPERTY, "5555555");
  }

  private static final List<Coder.Mode> MODES =
      List.of(
          Coder.Mode.MIXED, Coder.Mode.LOWER, Coder.Mode.UPPER, Coder.Mode.XSAFE, Coder.Mode.SHAPE);

  @Test
  void testLongs() {
    assertEncodesInMax(5, 1L, 2L, 3L);
    assertEncodesInMax(8, 11L, 22L, 33L);
    assertEncodesInMax(13, 111L, 222L, 333L, 444L);
  }

  @Test
  void testInts() {
    assertEncodesInMax(5, 1, 2, 3);
    assertEncodesInMax(8, 11, 22, 33);
    assertEncodesInMax(13, 111, 222, 333, 444);
  }

  @Test
  void testDoubles() {
    assertEncodesInMax(62, 1d, 2d, 3d);
    assertEncodesInMax(62, 11d, 22d, 33d);
    assertEncodesInMax(83, 111d, 222d, 333d, 444d);
  }

  private static void assertEncodesInMax(int maxLength, long... actualValues) {
    MODES.forEach(mode -> assertEncodesInMax(mode, maxLength, actualValues));
  }

  private static void assertEncodesInMax(Coder.Mode mode, int maxLength, long... actualValues) {
    Coder coder = Coder.of(0L, 1, mode);
    String id = coder.encodeLongs(actualValues);
    assertArrayEquals(actualValues, coder.decodeLongs(id), "decoding error");
    assertMaxLength(maxLength, id);
  }

  private static void assertEncodesInMax(int maxLength, int... actualValues) {
    MODES.forEach(mode -> assertEncodesInMax(mode, maxLength, actualValues));
  }

  private static void assertEncodesInMax(Coder.Mode mode, int maxLength, int... actualValues) {
    Coder coder = Coder.of(0L, 1, mode);
    String id = coder.encodeInts(actualValues);
    assertArrayEquals(actualValues, coder.decodeInts(id), "decoding error");
    assertMaxLength(maxLength, id);
  }

  private static void assertEncodesInMax(int maxLength, double... actualValues) {
    MODES.forEach(mode -> assertEncodesInMax(mode, maxLength, actualValues));
  }

  private static void assertEncodesInMax(Coder.Mode mode, int maxLength, double... actualValues) {
    Coder coder = Coder.of(0L, 1, mode);
    String id = coder.encodeDoubles(actualValues);
    assertArrayEquals(actualValues, coder.decodeDoubles(id), "decoding error");
    assertMaxLength(maxLength, id);
  }

  private static void assertMaxLength(int maxLength, String id) {
    if (id.length() > maxLength) assertEquals(maxLength, id.length(), "ID too long");
  }
}
