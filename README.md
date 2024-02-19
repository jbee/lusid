# Locally Unique Short Identifiers

This is a small library that allows to generate unique string IDs from numbers. 
These IDs are short, unique, URL-safe, and very unlikely to include words.
Each number corresponds to a unique string which can be decoded back into the original number.

## Usage
Encoding and decoding is done with `Coder` instance

```java
Coder coder = Coder.of(6, Mode.MIXED); // 6 characters, upper and lower case
String id = coder.encodeLong(42L); // = lR7wZ8 (depends on secret)
long value = coder.decodeLong(id); // = 42
```

A `Coder` has three configuration properties
* a minimum target length (1-20)
* a `Mode` (a configuration of the used characters)
* a 64 bit secret

The secret is either passed to `Coder.of` or, when omitted (like above), 
it is loaded from the `lusid.secret` system property or environment variable. 

## Properties
* 🆔 Generates unique IDs for any `long`, `int`, `double`, `float` numbers
* ⚡ Low overhead, algorithm is fast and mostly allocation free
* 🔧 Easy to create custom modes for specific target patterns
* 📏 integer numbers require less or as many characters as the number written in decimal
* 🔢 Can generate an ID for multiple numbers
* 🛑 High likelihood of identifying ID input errors (typos)
* 📢 The character mapping can be public without compromising the number or secret
* 🛡️ No amount of encoded IDs will help to disclose the original numbers or secret 
 

## API 
Demo of all the `Coder` API methods
```java
Coder coder = Coder.of(8); // minimum 8 characters mixed case

// single numbers
long lvalue = coder.decodeLong(coder.encodeLong(42L)); // = 42
int ivalue = coder.decodeInt(coder.encodeInt(13)); // = 13
double dvalue = coder.decodeDouble(coder.encodeDouble(0.5d)); // = 0.5
float fvalue = coder.decodeFloat(coder.encodeFloat(33.3f)); // = 33.3

// multiple numbers
long[] lvalues = coder.decodeLongs(coder.encodeLongs(1L,2L)); // = [1,2]
int[] ivalues = coder.decodeInts(coder.encodeInts(3,6,9)); // = [3,6,9]
double[] dvalues = coder.decodeDoubles(coder.encodeDoubles(0.5d,55.789d)); // = [0.5,55.789]
```

## Modes
The below table demonstrates IDs the different `Mode`s with minimum length 6:
```
 Value   MIXED   UPPER   LOWER   XSAFE
 
      1  VGH5h8  HU5VJ8  54hjv8  VTjvJr
     12  XU6r8k  R4XK86  6ukx8r  XgxKrk
    123  k5eS8s  6VCO8S  rjn38f  kvCSrs
   1234  r8NcsC  K8E1SC  x81efn  KrNcsC 
  12345  8LW5uS  8W7VGO  87wjt3  rZlvGS
 123456  5R9wZj  V69LW5  jr9z7h  vkhLZj
1234567 GR2oSct U6PFO1T 4rds3eg TkpFSct
```

## Algorithm
The algorithm works on bit level using `long`s. 
The 64bits of a `long` value are split in high `int` and low `int` value
which are encoded the same way concatenating the result but ignoring leading zeros.

The 32 bits of each `int` are encoded in 10 groups of 3 and 1 group of 2:
```
character        9   8   7   6   5   4   3   2   1   0  off
value           000 000 000 101 011 001 100 010 001 110 10 
secret          100 101 001 100 110 010 001 110 000 101 00
OXR             --- --- --- 001 101 011 101 100 001 011 10
character index              =1  =5  =3  =5  =4  =1  =3 --
table index                  0   3   2   1   0   3   2  (2)
UPPER character              C   H   3   V   G   E   3             
```
Each 3 bit group is XORed with the secret to get the character index on the mapping table.
Hence, each character mapping table (`Mode.tables`) must have 8 distinct characters to encode 3 bits, 0-7. 
Leading zeros in the value are not encoded. 
Tables are cycled through right to left starting with the offset given by the lowest 2 bits, 0-3. 
Therefore, there must be at least 4 tables to cycle through. 
Here we assume 4 tables being used.

If the resulting character sequence is shorter than the target minimum length
padding is added on the left. For a single missing character the `Mode.pad1` is added.
For 2 or more missing characters `Mode.padN` is added left most followed by
the number of additional padding bytes left to the 2nd length encoding character.

### Padding
Assuming the example from above should be padded to different minimum length, again using `Mode.UPPER`.

```
character        9   8   7   6   5   4   3   2   1   0  off
UPPER character              C   H   3   V   G   E   3             
secret          100 101 001 100 110 010 001 110 000 101 00
character index              =1  =5  =3  =5  =4  =1  =3 --
table index          2   1   0   3   2   1   0   3   2  (2)
padding 1                9   C   H   3   V   G   E   3
padding 2            8   N   C   H   3   V   G   E   3
padding 3        8   4   S   C   H   3   V   G   E   3
```
The `N` encodes zero additional padding bytes (that follow the length `N`).
It is encoded as `N` because the count is XORed with the secret for the position, here 1.
The table used continuous the cycle so N encodes the character at index 1 in the table at index 1.

If there are padding characters right to the padding count this cycles through the existing
results for value XOR secret right to left but encodes the value with the table belonging to
the padding character's position. In the example we get the `S` because the right most XORed
value was 3, the table for the padding character is 1, so the character at index 3 in the table at index 1.

To not always lead padded IDs with the padding indicator (`Mode.pad1` or `Mode.padN`) the character
finally switches place with the character at index resulting from bit-count of value XOR secret (full 32bit) modulo
the ID target length.

### Flipping
As negative numbers usually have leading 1s not leading zeros it is often preferable to do a bitwise flip and
encode the flipped number instead. This always takes place before any of the above. If the number is flipped
the `Mode.flip` marker character is prepended. So it is always the leftmost character for a single number.

### Joining
When multiple numbers are encoded each number is encoded as described above.
The parts are then joined (or seperated) by the `Mode.join` character.
The padding to reach the target length is equally distributed on the individual numbers.
