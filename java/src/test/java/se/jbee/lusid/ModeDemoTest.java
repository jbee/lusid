package se.jbee.lusid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ModeDemoTest {

  @BeforeAll
  static void setUp() {
    System.setProperty(Coder.SECRET_PROPERTY, "234987");
  }

  @Test
  void demo() {
    Coder mixed = Coder.of(6, Coder.Mode.MIXED);
    Coder upper = Coder.of(6, Coder.Mode.UPPER);
    Coder lower = Coder.of(6, Coder.Mode.LOWER);
    Coder xsafe = Coder.of(6, Coder.Mode.XSAFE);
    Coder shape = Coder.of(6, Coder.Mode.SHAPE);

    long[] values = {1, 12, 123, 1234, 12345, 123456, 1234567};
    StringBuilder table = new StringBuilder();
    for (long v : values) {
      table.append(
          "%7s %7s %7s %7s %7s %7s\n"
              .formatted(
                  v,
                  mixed.encodeLong(v),
                  upper.encodeLong(v),
                  lower.encodeLong(v),
                  xsafe.encodeLong(v),
                  shape.encodeLong(v)));
    }
    assertEquals(
        """
              1  VJH5h8  HH5VJ8  55hjv8  VVjvJr  VvJ6Er
             12  XH6r8k  R5XK86  6hkx8r  XjxKrk  XJ7Wrk
            123  k4eS8s  6TCO8S  rgn38f  ktCSrs  k5eSrs
           1234  w8NcsC  L8E1SC  z81efn  LrNcsC  UrNcsC
          12345  8KW5uS  8R7VGO  86wjt3  rXlvGS  rxL6AS
         123456  5R9wZj  V69LW5  jr9z7h  vkhLZj  6KhUZj
        1234567 GR2oSct U6PFO1T 4rds3eg TkpFSct tK3uSct
        """,
        table.toString());
  }
}
