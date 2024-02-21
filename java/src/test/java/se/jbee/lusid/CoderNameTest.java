package se.jbee.lusid;

import org.junit.jupiter.api.Test;

import static java.lang.Math.max;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests the {@link Coder#encodeName(String)} and {@link Coder#decodeName(String)} method pair. */
class CoderNameTest {

  @Test
  void testName_Empty() {
    Coder coder = Coder.of(67L, 9);
    assertName(coder, 0, "");
  }

  @Test
  void testName() {
    Coder coder = Coder.of(67L, 9);
    String expected = "HELLO_WORLD";
    for (int i = 1; i < expected.length(); i++)
      assertName(coder, max(9, i), expected.substring(0, i));
  }

  @Test
  void testName_MaxPadding() {
    Coder coder = Coder.of(67L, 12);
    assertName(coder, 2 + 9, "NO");
  }

  @Test
  void testName_IllegalCharacter() {
    Coder coder = Coder.of(67L, 9);
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> coder.encodeName("No"));
    assertEquals("Not a name character: o at index 1", ex.getMessage());
  }

  private void assertName(Coder coder, int expectedLength, String expected) {
    String id = coder.encodeName(expected);
    assertEquals(expectedLength, id.length());
    assertEquals(expected, coder.decodeName(id));
  }
}
