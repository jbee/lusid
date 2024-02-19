package se.jbee.lusid;

import static java.lang.Long.parseLong;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.List;

/**
 * Implementation of the <i>Locally Unique Short Identifier</i> encoder/decoder algorithm.
 *
 * @author Jan Bernitt
 * @param secret the used secret
 * @param minLength the used minimum length
 * @param join character used to combine/join/split multiple values
 * @param flip character used to indicate a bit flipped value (encoded as marker + flipped number)
 * @param pad1 character used to fill a single padding character
 * @param padN character used to indicate multiple filler characters
 * @param tables encoding tables used, each must have 8 unique characters, at least 4 tables are
 *     required, tables are used in order left to right
 */
record Lusid(
    long secret, int minLength, char join, char flip, char pad1, char padN, List<char[]> tables)
    implements Coder {

  /** The largest positive number that can be expressed in 19 characters. */
  private static final long MAX_19 = ~((1L << 63) | (1L << 62) | (1L << 61));

  static Coder coder(long secret, int minLength, Mode mode) {
    minLength = max(1, min(20, minLength));
    if (secret == 0L) secret = parseSecretProperty();
    secret = secretEnhance(secret);
    List<char[]> tables = mode.tables().stream().limit(13).map(String::toCharArray).toList();
    return new Lusid(secret, minLength, mode.join(), mode.flip(), mode.pad1(), mode.padN(), tables);
  }

  private static long parseSecretProperty() {
    String secretStr = System.getProperty(SECRET_PROPERTY, System.getenv(SECRET_PROPERTY));
    if (secretStr == null || secretStr.isEmpty())
      throw new IllegalArgumentException(
          "Secret must be defined for system property or environment variable named: "
              + SECRET_PROPERTY);
    return parseLong(secretStr);
  }

  @Override
  public String encodeLongs(long... values) {
    if (values.length == 0) return "";
    if (values.length == 1) return encodeLong(values[0]);
    int dataLength = 0;
    for (long v : values) dataLength += encodingMinLength(v);
    dataLength += values.length - 1; // for the join characters
    int padAvgLength = 0;
    int padLength0 = 0;
    if (dataLength < minLength) {
      padAvgLength = (minLength - dataLength) / values.length;
      padLength0 = ((minLength - dataLength) % values.length) + padAvgLength;
    }
    StringBuilder id = new StringBuilder(max(minLength, dataLength));
    id.append(encodeLong(values[0], padLength0 + encodingMinLength(values[0])));
    for (int i = 1; i < values.length; i++)
      id.append(join).append(encodeLong(values[i], padAvgLength + encodingMinLength(values[i])));
    return id.toString();
  }

  @Override
  public String encodeLong(long value) {
    return encodeLong(value, minLength);
  }

  private String encodeLong(long value, int minLength) {
    boolean doFlip = isFlipPreferable(value);
    if (doFlip) value = ~value;
    int lowValue = lowInt(value);
    int highValue = highInt(value);
    int offset = doFlip ? 1 : 0;
    char[] id;
    if (minLength <= 10 && highValue == 0) {
      int dataLength = encodingDataLength(lowValue);
      int length = max(minLength, dataLength);
      int padLength = max(0, length - dataLength);
      if (doFlip && padLength > 0) length--;
      id = new char[offset + length];
      encode(lowValue, lowInt(secret), id, offset, length, dataLength);
    } else {
      int dataLength = encodingDataLength(highValue) + 10;
      int length = max(minLength, dataLength);
      int padLength = max(0, length - dataLength);
      if (doFlip && padLength > 0) length--;
      id = new char[offset + length];
      encode(lowValue, lowInt(secret), id, id.length - 10, 10, encodingDataLength(lowValue));
      encode(highValue, highInt(secret), id, offset, length - 10, encodingDataLength(highValue));
    }
    if (doFlip) {
      id[0] = flip;
    }
    return new String(id);
  }

  /**
   * The flip marker character is an optimisation to avoid having to encode the high bits that all
   * negative numbers have. Instead, for very large negative numbers it is better to just bit encode
   * as is to not pay with an extra marker character.
   *
   * <p>OBS! bit-flip is used over negation because there is a flipped value for any bit combination
   * but there is not a positive number for the largest negative number.
   */
  private boolean isFlipPreferable(long value) {
    if (value >= 0) return false;
    long flipped = ~value;
    return flipped <= MAX_19;
  }

  @Override
  public long[] decodeLongs(String id) {
    if (id.isEmpty()) return new long[0];
    if (id.indexOf(join) < 0) return new long[] {decodeLong(id)};
    int count = 1 + (int) id.chars().filter(c -> c == join).count();
    long[] values = new long[count];
    int start = 0;
    char[] chars = id.toCharArray();
    for (int i = 0; i < count; i++) {
      int end = id.indexOf(join, start);
      values[i] = decodeLong(chars, start, (end < 0 ? chars.length : end) - start);
      start = end + 1;
    }
    return values;
  }

  @Override
  public long decodeLong(String id) {
    return decodeLong(id.toCharArray(), 0, id.length());
  }

  private long decodeLong(char[] id, int offset, int length) {
    return id[0] == flip ? ~decode(id, offset + 1, length - 1) : decode(id, offset, length);
  }

  private long decode(char[] id, int offset, int length) {
    if (length <= 10) return decode(id, offset, length, lowInt(secret));
    int highLength = length - 10;
    long high = decode(id, offset, highLength, highInt(secret));
    long low = decode(id, offset + highLength, 10, lowInt(secret));
    return (high << 32) | low;
  }

  private void encode(int value, int secret, char[] id, int offset, int length, int dataLength) {
    int padLength = max(0, length - dataLength);
    final int secVal = value ^ secret;
    int tableOffset = secVal & 0b11; // lowest 2 bits are start table offset
    int tableCount = tables.size();
    int idIndex = offset + length - 1;
    // encode data backed characters
    for (int i = 0; i < dataLength; i++)
      id[idIndex--] = tables.get(tableOffset++ % tableCount)[(secVal >>> (2 + (3 * i))) & 0b111];
    if (padLength == 0) return;
    // encode padding
    if (padLength == 1) {
      id[offset] = pad1;
    } else {
      for (int i = 0; i < padLength - 2; i++) {
        int padVal = secVal >>> (2 + (3 * i) % dataLength);
        id[idIndex--] = tables.get(tableOffset++ % tableCount)[padVal & 0b111];
      }
      int padSecret = (secret >>> (2 + 3 * (length - 1))) & 0b111;
      // 2: the pad indicator and the pad length
      id[offset + 1] = tables.get(tableOffset % tableCount)[padSecret ^ (padLength - 2)];
      id[offset] = padN;
    }
    // swap pad marker to a different position
    int padIndex = offset + Long.bitCount(secVal) % length;
    swap(id, offset, padIndex);
  }

  /** The minimum length required for data + flip symbols */
  private static int encodingMinLength(long value) {
    if (value < 0) return 1 + encodingMinLength(~value);
    int l = encodingDataLength(lowInt(value));
    if (highInt(value) == 0) return l;
    return encodingDataLength(highInt(value)) + l;
  }

  /** The minimum length required to encode the value */
  private static int encodingDataLength(int value) {
    int zeroBits = Integer.numberOfLeadingZeros(value);
    int dataBits = 32 - zeroBits - 2;
    int dataLength = dataBits / 3;
    if (dataBits % 3 > 0) dataLength++;
    return max(1, dataLength);
  }

  private long decode(char[] id, int offset, int length, int secret) {
    int tableOffset0;
    int tableCount = tables.size();

    // was there padding?
    int padIndex = decodePadIndex(id, offset, length);
    if (padIndex >= 0) {
      // un-swap
      swap(id, offset, padIndex);

      // offset of the left most symbol must be found
      tableOffset0 = decodeTableOffset(id, offset, length);

      // pad length?
      if (pad1 == id[offset]) {
        offset++;
        length--;
      } else {
        int padSecret = (secret >>> (2 + 3 * (length - 1))) & 0b111;
        int padEncoded =
            decodeTableIndex(tables.get((tableOffset0 + length - 2) % tableCount), id[offset + 1]);
        int padLength = (padSecret ^ padEncoded) + 2; // 2: the pad indicator and the pad length
        offset += padLength;
        length -= padLength;
      }
    } else {
      tableOffset0 = decodeTableOffset(id, offset, length);
    }
    // decoding the data symbols
    // OBS! must be long because we might set the highest int bit, and we don't want negative
    // extension
    long value = 0;
    for (int i = 0; i < length; i++) {
      if (i > 0) value <<= 3;
      int tripletSecret = (secret >>> (2 + 3 * (length - 1 - i))) & 0b111;
      int tripletEncoded =
          decodeTableIndex(
              tables.get((tableOffset0 + length - 1 - i) % tableCount), id[offset + i]);
      value |= tripletSecret ^ tripletEncoded;
    }
    // restoring lowest 2bits from table offset
    value <<= 2;
    value |= (tableOffset0 ^ secret) & 0b11;
    return value;
  }

  private int decodeTableOffset(char[] id, int offset, int length) {
    int i0 = offset + length - 1;
    char s0 = id[i0];
    for (int i = 0; i < 4; i++) if (isTableSymbol(tables.get(i), s0)) return i;
    throw new IllegalArgumentException(
        "Unexpected offset: `%s` (at %d in %s)".formatted(s0, i0, new String(id)));
  }

  private int decodePadIndex(char[] id, int offset, int length) {
    for (int i = 0; i < length; i++) if (isPadSymbol(id[offset + i])) return offset + i;
    return -1;
  }

  private static int decodeTableIndex(char[] table, char s) {
    for (int i = 0; i < table.length; i++) if (table[i] == s) return i;
    throw new IllegalArgumentException(
        "Unexpected symbol: `%s` (expected one of %s)".formatted(s, new String(table)));
  }

  private static boolean isTableSymbol(char[] table, char s) {
    for (char c : table) if (c == s) return true;
    return false;
  }

  private boolean isPadSymbol(char s) {
    return pad1 == s || padN == s;
  }

  private void swap(char[] id, int i1, int i2) {
    char tmp = id[i1];
    id[i1] = id[i2];
    id[i2] = tmp;
  }

  /** Makes sure the secret has a good mix of 1 and 0 throughout the 64 bits. */
  static long secretEnhance(long secret) {
    if (secret < 0)
      // avoid lots of leading 1s
      return secretEnhance(-secret);
    if (Long.numberOfLeadingZeros(secret) >= 32)
      // avoid lots of leading zeros by mirroring the low int on the high int (without it becoming
      // negative)
      return secretEnhance(secret | (Long.reverse(secret) >>> 1));
    long nibbleMask = 0b1111;
    long shiftedSecret = secret;
    long filledSecret = 0;
    int ones = 3; // just a good value to use should the first nibble be 0
    for (int i = 0; i < 16; i++) {
      long nibble = shiftedSecret & nibbleMask;
      filledSecret <<= 4;
      if (nibble == 0L) nibble = ones;
      filledSecret |= nibble;
      shiftedSecret >>= 4;
      int onesNew = Long.bitCount(nibble);
      ones = ones == 1 && onesNew == 1 ? 5 : onesNew;
    }
    return filledSecret;
  }

  private static int lowInt(long value) {
    return (int) value;
  }

  private static int highInt(long value) {
    return (int) (value >>> 32);
  }
}
