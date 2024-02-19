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
                  shape.encodeLong(v)
              ));
    }
    assertEquals(
        """
              1  VGH5h8  HU5VJ8  54hjv8  VTjvJr  VtJ6Er
             12  XU6r8k  R4XK86  6ukx8r  XgxKrk  XG7Wrk
            123  k5eS8s  6VCO8S  rjn38f  kvCSrs  k6eSrs
           1234  r8NcsC  K8E1SC  x81efn  KrNcsC  WrNcsC
          12345  8LW5uS  8W7VGO  87wjt3  rZlvGS  rzL6AS
         123456  5R9wZj  V69LW5  jr9z7h  vkhLZj  6KhUZj
        1234567 GR2oSct U6PFO1T 4rds3eg TkpFSct tK3uSct
        """,
        table.toString());
  }
}
