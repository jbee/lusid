package se.jbee.lusid;

import static java.util.stream.IntStream.range;

import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * A {@linkplain Coder} is the combination of an encoder and a decoder with a chosen configuration.
 *
 * <p>A configuration has 3 parts: the {@link Mode}, the desired minimum length and the used secret.
 *
 * <p>A source value(s) is concealed using a 64bit secret. Each 64bit value is simply XORed with the
 * secret before it is encoded using the {@link Mode}'s configuration of how to express the
 * resulting 64bits.
 *
 * <p>Decoding is simply the reverse process of turning the encoded characters back into bits and
 * again XOR the resulting value with the secret to get to the original value.
 *
 * <p>The main design criteria for the algorithm that turns bits into characters is to generate
 * characters sequences that are unique (for the input value), that are unlikely to be words of any
 * language, that appear "random" and avoid common subsequences for filler characters, and that
 * avoid accidentally leaking parts of the secret.
 *
 * <p>For maximum safety against creating unwanted words use the {@link Mode#XSAFE} which will only
 * use consonants.
 *
 * <p>Since the algorithm encodes bits it does not care what type of number is encoded. For ease of
 * implementation the algorithm is using {@code long}. Other number types are simply turned into
 * bit-equivalent long values.
 *
 * <p>Minimum length is always a target that cannot always be achieved. If a value requires fewer
 * characters filler characters are inserted. If a value requires more characters the resulting
 * string has to be longer than the target minimum length. The minimum length cannot exceed 20,
 * which is the maximum length needed to encode any number.
 *
 * <p>As a rule of thumb it can be assumed that a value requires as many characters as it would when
 * written as a decimal number. This is mostly true but depends on the exact bit representation. For
 * example, 0-31 require 1 character, 32-255 require 2 characters, 256-2047 require 3 characters and
 * so on.
 *
 * <p>The algorithm does not require heap allocation except to create the encoded result string.
 * Decoding can be implemented heap allocation free but currently does use a single allocation to
 * copy the input string into a character array for convenience. All in all both encoding and
 * decoding can be assumed very "cheap" operations similar to creating a {@link String} or similar
 * length.
 *
 * <p><b>Important!</b> To protect the secret one must never expose the {@link Coder} in a way that
 * allows a user to poke it with known number values. Values can only be protected if both the
 * original value(s) and the secret remain unknown to the outside. However, it is not a problem if
 * large volumes of encoded strings become public knowledge.
 *
 * <p>If a use case requires exposing or giving access to original values a dedicated {@link Coder}
 * instance must be used which is using a different secret. For such use cases the encoding does not
 * conceal the information which must be considered public. In such cases the encoding is purely
 * done for aesthetic reasons or convenience.
 *
 * @author Jan Bernitt
 */
public interface Coder {

  /**
   * The name used to read the secret from a system property or environment variable if no secret is
   * specified (=specified as zero).
   */
  String SECRET_PROPERTY = "lusid.secret";

  /*
  Creating new De/Encoders
   */

  /**
   * The secret used by the system coder is given by the system property or environment variable
   * named {@link #SECRET_PROPERTY}.
   *
   * <p>A minimum length of 8 is a good default as it allows to encode numbers up to several
   * millions which is enough for most common PKs ranges or common data values.
   *
   * @see #of(long, int, Mode)
   * @return The "system" default {@link Coder} with a minimum length of 8 and {@link Mode#MIXED}
   * @throws IllegalArgumentException if no secret is specified via {@link #SECRET_PROPERTY}
   * @throws NumberFormatException if the specified secret is not a valid number string
   */
  static Coder of8() {
    return of(8);
  }

  /**
   * @see #of(long, int, Mode)
   */
  static Coder of(int minLength) {
    return of(0L, minLength, Mode.MIXED);
  }

  /**
   * @see #of(long, int, Mode)
   */
  static Coder of(long secret, int minLength) {
    return of(secret, minLength, Mode.MIXED);
  }

  /**
   * @see #of(long, int, Mode)
   */
  static Coder of(int minLength, Mode mode) {
    return of(0L, minLength, mode);
  }

  /**
   * @param secret the secret 64bit sequence to use, 0 to load from {@link #SECRET_PROPERTY}
   * @param minLength target minimum length for generated ID strings; 1-20, any value outside the
   *     bounds is limited to the closest limit
   * @param mode configuration for the characters used to encode/decode bits with
   * @return an instance with the specified behaviour
   */
  static Coder of(long secret, int minLength, Mode mode) {
    return Lusid.coder(secret, Coder.SECRET_PROPERTY, minLength, mode);
  }

  /**
   * @param secretProperty name of the system property or environment variable that is used as
   *     secret number
   * @param minLength target minimum length for generated ID strings; 1-20, any value outside the
   *     bounds is limited to the closest limit
   * @param mode configuration for the characters used to encode/decode bits with
   * @return an instance with the specified behaviour
   */
  static Coder of(String secretProperty, int minLength, Mode mode) {
    return Lusid.coder(0L, secretProperty, minLength, mode);
  }

  /*
  Essential De/Encoding API - encode/decode pairs
   */

  /**
   * Requires at most 20 characters. As a rule of thumb a value requires about as many characters as
   * it would when written in decimal or 1 less.
   *
   * @param value any number
   * @return the encoded ID
   */
  String encodeLong(long value);

  /**
   * @param id an ID previously encoded with this {@link Coder}
   * @return the decoded value
   * @throws IllegalArgumentException in case the given ID wasn't valid. There is roughly a 7/8
   *     chance that changing a single character is detected. Change in multiple characters increase
   *     the chance further as expected.
   */
  long decodeLong(String id);

  /**
   * Encodes each value using {@link #encodeLong(long)} and joins the results with {@link
   * Mode#join()}. The minimum length applies to the entire resulting ID, potential padding is
   * distributed evenly to the individual numbers joined.
   *
   * @param values a list of arbitrary long values
   * @return An ID representing the values
   */
  String encodeLongs(long... values);

  long[] decodeLongs(String id);

  /**
   * The resulting ID is always 1 character per input character. Padding can be at most 9 additional
   * characters.
   *
   * @param value for example an enum constant name
   * @return the encoded ID for the given name
   * @throws IllegalArgumentException in case the given value has characters other than upper case
   *     ASCII letters and the underscore symbol
   */
  String encodeName(String value);

  String decodeName(String id);

  /**
   * The resulting ID always uses 2 characters per input UTF-8 byte. Padding can be at most 9
   * additional characters.
   *
   * @param value any string
   * @return the encoded ID for the string
   */
  String encodeText(String value);

  String decodeText(String id);

  /*
  Convenience De/Encoding API
   */

  /**
   * Requires at most 10 characters for positive values, 11 for large negative values. As a rule of
   * thumb a value requires about as many characters as it would when written in decimal or 1 less.
   *
   * @param value any number
   * @return the encoded ID
   */
  default String encodeInt(int value) {
    return encodeLong(value);
  }

  /**
   * Requires at most 20 characters. Most values will require 20 characters.
   *
   * @param value any number including NaN and infinity
   * @return the encoded ID
   */
  default String encodeDouble(double value) {
    return encodeLong(Double.doubleToRawLongBits(value));
  }

  /**
   * Requires at most 10 characters for positive values, 11 for negative values. Most values do
   * require 10/11 characters.
   *
   * @param value any number including NaN and infinity
   * @return the encoded ID
   */
  default String encodeFloat(float value) {
    return encodeInt(Float.floatToRawIntBits(value));
  }

  default int decodeInt(String id) {
    return (int) decodeLong(id);
  }

  default double decodeDouble(String id) {
    return Double.longBitsToDouble(decodeLong(id));
  }

  default float decodeFloat(String id) {
    return Float.intBitsToFloat(decodeInt(id));
  }

  default String encodeDoubles(double... values) {
    return encodeLongs(DoubleStream.of(values).mapToLong(Double::doubleToRawLongBits).toArray());
  }

  default double[] decodeDoubles(String id) {
    return LongStream.of(decodeLongs(id)).mapToDouble(Double::longBitsToDouble).toArray();
  }

  default String encodeInts(int... values) {
    return encodeLongs(IntStream.of(values).mapToLong(v -> v).toArray());
  }

  default int[] decodeInts(String id) {
    return LongStream.of(decodeLongs(id)).mapToInt(v -> (int) v).toArray();
  }

  /*
  Standard Encodings
   */

  /**
   * Modes are configurations for the de/encoding process that control which characters are used to
   * encode the information.
   *
   * <p>When encoding bits the encoder cycles through the {@link #tables}. Each table is chosen so
   * that a sequence of any character from one table followed by any character from the next table
   * is unlikely to result in a pronounceable word. The most extreme version of this is the {@link
   * #XSAFE} mode that only uses consonants. The other modes split consonants and vowels in a way
   * that is very unlikely to ever create meaningful syllables.
   *
   * <p>The used mode and its encoding are not considered secret. They can be shared and made public
   * without compromising the encoded value or the secret used.
   *
   * @param join character used to join or separate multiple values
   * @param flip character used to indicate a bit flipped value (always in 1st position)
   * @param pad1 character used for a single "filler" character (leftmost; after the flip)
   * @param padN character used to indicate 2+ "filler" characters (leftmost; after the flip)
   * @param tables 3bit encoding tables used; each must have 8 characters unique for the table,
   *     tables are used in order left to right; must be 4-13 tables; characters can reoccur in
   *     other tables but within the first 4 all characters must be unique; no table can contain any
   *     of the special characters for {@link #join}, {@link #flip}, {@link #pad1} or {@link #padN}
   */
  record Mode(char join, char flip, char pad1, char padN, List<String> tables) {
    /** Upper case letters and digits only */
    public static final Mode UPPER =
        new Mode('Q', 'Y', '9', '8', List.of("BCDFGJKL", "MNPSTVXZ", "01234567", "AEIOUHRW"));

    /** Lower case letters and digits only */
    public static final Mode LOWER =
        new Mode('q', 'y', '9', '8', List.of("mnpstvxz", "bcdfgjkl", "aeiouhrw", "01234567"));

    /** eXtra/eXtreme/maX safe - by only using consonants */
    public static final Mode XSAFE =
        new Mode('H', 'R', 'h', 'r', List.of("BCDFGJKL", "mnpstvxz", "bcdfgjkl", "MNPSTVXZ"));

    public static final List<String> SHAPE_TABLES =
        List.of("BCDFGJKL", "mnpstvxz", "bcdfgjkw", "MNPSTVXZ", "aeiuAEWU", "12345678");

    /** only characters that cannot be confused easily when reading in sans-serif */
    public static final Mode SHAPE = new Mode('H', 'R', 'h', 'r', SHAPE_TABLES);

    private static final List<String> MIXED_TABLES =
        List.of("BCDFGJKL", "mnpstvxz", "bcdfgjkl", "MNPSTVXZ", "aeiouhrw", "01234567", "AEIOUHRW");

    /** mixed case letters and digits */
    public static final Mode MIXED = new Mode('Q', 'y', '9', '8', MIXED_TABLES);

    /**
     * @throws IllegalArgumentException when the character configuration is inconsistent, this means
     *     it has too few or too characters, or it uses the same character more than once when it
     *     must be unique/distinct.
     */
    public Mode {
      if (tables.size() < 4)
        throw new IllegalArgumentException("At least 4 bit tables are required");
      if (tables.stream().anyMatch(t -> t.length() != 8))
        throw new IllegalArgumentException("Each bit table must have 8 symbols");
      if (tables.stream().anyMatch(t -> t.chars().distinct().count() != 8))
        throw new IllegalArgumentException("Each character in a table must be distinct (unique)");
      if (range(0, 4).mapToObj(tables::get).flatMapToInt(String::chars).distinct().count() != 32)
        throw new IllegalArgumentException(
            "Each character in the first 4 tables must be distinct (unique)");
      if (tables.stream().anyMatch(t -> t.indexOf(join) >= 0))
        throw new IllegalArgumentException("Table must not contain the join character");
      if (tables.stream().anyMatch(t -> t.indexOf(flip) >= 0))
        throw new IllegalArgumentException("Table must not contain the flip character");
      if (tables.stream().anyMatch(t -> t.indexOf(pad1) >= 0))
        throw new IllegalArgumentException("Table must not contain the pad1 character");
      if (tables.stream().anyMatch(t -> t.indexOf(padN) >= 0))
        throw new IllegalArgumentException("Table must not contain the padN character");
      if (IntStream.of(join, flip, pad1, padN).distinct().count() != 4)
        throw new IllegalArgumentException("join, flip, pad1, padN must be different characters");
    }
  }
}
