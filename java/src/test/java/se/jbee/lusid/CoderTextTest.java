package se.jbee.lusid;

import static java.lang.Math.max;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/** Tests the {@link Coder#encodeText(String)} and {@link Coder#decodeText(String)} method pair. */
class CoderTextTest {

  @Test
  void testText_Empty() {
    Coder coder = Coder.of(67L, 9);
    assertText(coder, 0, "");
  }

  @Test
  void testText() {
    Coder coder = Coder.of(67L, 9);
    String expected = "Hello world! \uD83C\uDF89";
    assertText(coder, 34, expected);
    StringBuilder str = new StringBuilder();
    // need to iterate this in code points to not split the 2 char symbol
    expected.codePoints().forEach(cp -> assertText(coder, -1, str.append(cp).toString()));
  }

  @Test
  void testText_MaxPadding() {
    Coder coder = Coder.of(67L, 14);
    assertText(coder, 4 + 9, "NO");
  }

  private void assertText(Coder coder, int expectedLength, String expected) {
    String id = coder.encodeText(expected);
    if (expectedLength >= 0) assertEquals(expectedLength, id.length());
    String actual = coder.decodeText(id);
    assertEquals(expected, actual);
  }
}
