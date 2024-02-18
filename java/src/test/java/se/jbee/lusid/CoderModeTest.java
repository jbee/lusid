package se.jbee.lusid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests the {@link Coder.Mode} validation. */
class CoderModeTest {

  @Test
  void testMode_TableLength() {
    assertIllegalMode(
        "Each bit table must have 8 symbols",
        () ->
            new Coder.Mode( // 2nd table is 9 characters long
                'Q', 'y', '9', '8', List.of("BCDFGJKL", "mnpstvxzq", "bcdfgjkl", "MNPSTVXZ")));
  }

  @Test
  void testMode_TableCount() {
    assertIllegalMode(
        "At least 4 bit tables are required",
        () ->
            new Coder.Mode( // only 3 tables
                'Q', 'y', '9', '8', List.of("BCDFGJKL", "mnpstvxz", "bcdfgjkl")));
  }

  @Test
  void testMode_TableUnique() {
    assertIllegalMode(
        "Each character in a table must be distinct (unique)",
        () ->
            new Coder.Mode( // 2nd table has duplicate: mm
                'Q', 'y', '9', '8', List.of("BCDFGJKL", "mmpstvxz", "bcdfgjkl", "MNPSTVXZ")));
  }

  @Test
  void testMode_TableFirst4Unique() {
    assertIllegalMode(
        "Each character in the first 4 tables must be distinct (unique)",
        () ->
            new Coder.Mode( // 2nd and 4th table have Z
                'Q', 'y', '9', '8', List.of("BCDFGJKL", "mnpstvxZ", "bcdfgjkl", "MNPSTVXZ")));
  }

  @Test
  void testMode_TableContainsJoin() {
    assertIllegalMode(
        "Table must not contain the join character",
        () ->
            new Coder.Mode( // 1st table contains B
                'B', 'y', '9', '8', List.of("BCDFGJKL", "mnpstvxz", "bcdfgjkl", "MNPSTVXZ")));
  }

  @Test
  void testMode_TableContainsFlip() {
    assertIllegalMode(
        "Table must not contain the flip character",
        () ->
            new Coder.Mode( // 2nd table contains m
                'Q', 'm', '9', '8', List.of("BCDFGJKL", "mnpstvxz", "bcdfgjkl", "MNPSTVXZ")));
  }

  @Test
  void testMode_TableContainsPad1() {
    assertIllegalMode(
        "Table must not contain the pad1 character",
        () ->
            new Coder.Mode( // 3rd table contains c
                'Q', 'y', 'c', '8', List.of("BCDFGJKL", "mnpstvxz", "bcdfgjkl", "MNPSTVXZ")));
  }

  @Test
  void testMode_TableContainsPadN() {
    assertIllegalMode(
        "Table must not contain the padN character",
        () ->
            new Coder.Mode( // 4th table contains X
                'Q', 'y', '9', 'X', List.of("BCDFGJKL", "mnpstvxz", "bcdfgjkl", "MNPSTVXZ")));
  }

  @Test
  void testMode_JoinFlipPadCollision() {
    assertIllegalMode(
        "join, flip, pad1, padN must be different characters",
        () ->
            new Coder.Mode( // flip and pad1 are both Q
                'Q', 'y', 'Q', '8', List.of("BCDFGJKL", "mnpstvxz", "bcdfgjkl", "MNPSTVXZ")));
  }

  private static void assertIllegalMode(String expected, Executable executable) {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, executable);
    assertEquals(expected, ex.getMessage());
  }
}
