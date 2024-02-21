# Locally Unique Short Identifiers

This is a small library that allows to generate unique string IDs from numbers. 
These IDs are short, unique, URL-safe, and very unlikely to include words.
Each number corresponds to a unique string which can be decoded back into the original number.

Here _Locally_ refers to the fact that the encoded ID is unique 
for a given input number in combination with the secret and mode (character mapping) used.

## Usage
Encoding and decoding is done via a `Coder` instance

```java
Coder coder = Coder.of(6, Mode.MIXED); // 6 characters, upper and lower case
String id = coder.encodeLong(42L);     // = "lR7wZ8" (depends on secret)
long value = coder.decodeLong(id);     // = 42
```

A `Coder` has three configuration properties
* a minimum target length (1-20)
* a `Mode` (a configuration of the used characters)
* a 64 bit secret

The secret is either passed to directly to `Coder.of` or, when omitted (like above), 
it is loaded from the `lusid.secret` system property or environment variable.
The name of an alternative property can also be passed as argument.

## Properties
* 🆔 Generates unique IDs for any `long`, `int`, `double`, `float` number(s)
* 🆔 Generates unique IDs for `enum` names (upper case letters + `_`)
* ⚡ Low overhead, algorithm is fast and mostly allocation free
* 🤬 Very unlikely to result in an actual (3+ letter) word (of any language) 
* 🔧 Easy to create custom modes for specific target patterns
* 📏 integer numbers require less or as many characters as the number written in decimal
* ⛔ High likelihood of identifying ID input errors (typos)
* 📢 The character mapping (mode) can be public without compromising security
* 🛡️ No amount of encoded IDs will help to disclose the original numbers or secret 
 

## API 
The `Coder` API methods to encode and decode different types of values:
```java
Coder coder = Coder.of(8); // minimum 8 characters mixed case

// single numbers
long lvalue = coder.decodeLong(coder.encodeLong(42L));        // = 42
int ivalue = coder.decodeInt(coder.encodeInt(13));            // = 13
double dvalue = coder.decodeDouble(coder.encodeDouble(0.5d)); // = 0.5
float fvalue = coder.decodeFloat(coder.encodeFloat(33.3f));   // = 33.3

// multiple numbers
long[] lvalues = coder.decodeLongs(coder.encodeLongs(1L,2L));              // = [1,2]
int[] ivalues = coder.decodeInts(coder.encodeInts(3,6,9));                 // = [3,6,9]
double[] dvalues = coder.decodeDoubles(coder.encodeDoubles(0.5d,55.789d)); // = [0.5,55.789]

// (enum) names (upper letters and _ only)
String name = coder.decodeName(coder.encodeName("RUNTIME")); // = "RUNTIME" 

// any text
String text = coder.decodeText(coder.encodeText("🥳"));      // = "🥳"
```

When constructing a `Coder` instance the secret can be passed explicitly or when omitted the
`lusid.secret` system property or environment variable is used. Default mode is `Mode.MIXED`.

```java
// explicit custom secret property
System.setProperty("my.secret.property", "42");
Coder c1 = Coder.of("my.secret.property", 1);   // minimum length 1, mode MIXED

// explicit secret value, explicit mode
Coder c2 = Coder.of(42L, 2, Mode.UPPER);        // minimum length 2, mode UPPER

// implicit secret from lusid.secret
Coder c3 = Coder.of(3);                         // minimum length 3, mode MIXED
```


## 🔠 Modes
Five standard modes are included:

* `MIXED`: uses upper and lower case letters and digits
* `UPPER`: uses upper case letters and digits
* `LOWER`: uses lower case letters and digits
* `XSAFE`: uses upper and lower consonants and digits (safest when trying to avoid 🤬 words)
* `SHAPE`: uses upper and lower letters and digits that cannot be easily confused visually

It is also easy to create further user defined modes.

The below table demonstrates IDs using the different `Mode`s with minimum length 6:
```
 Value   MIXED   UPPER   LOWER   XSAFE   SHAPE
 
      1  VJH5h8  HH5VJ8  55hjv8  VVjvJr  VvJ6Er
     12  XH6r8k  R5XK86  6hkx8r  XjxKrk  XJ7Wrk
    123  k4eS8s  6TCO8S  rgn38f  ktCSrs  k5eSrs
   1234  w8NcsC  L8E1SC  z81efn  LrNcsC  UrNcsC
  12345  8KW5uS  8R7VGO  86wjt3  rXlvGS  rxL6AS
 123456  5R9wZj  V69LW5  jr9z7h  vkhLZj  6KhUZj
1234567 GR2oSct U6PFO1T 4rds3eg TkpFSct tK3uSct
```

## 🛡️ Security
> [!Important]
> **TLDR;** Do not expose an API to retrieve an encoded ID for a known input (original) value.
> Both the secret and original values must stay "unknown" to maintain information hiding.

This is not a typical "encryption" library. 
Meaning the algorithm is easily reversible.
The information is hidden and disclosed using a simple XOR with a 64 bit secret.
This might appear awfully simplistic, however, as long as an attacker 
does not know the secret or any original value together with its corresponding 
encoded value there is no way of telling if an assumed secret is correct as any
secret will result in a number, just not the original one. 

The security comes from removing the possibility to recognise if the
original value is found when reversing the algorithm. 
This means a brute force attack is fairly meaningless. 
It maybe can be used to find some bits of the secret 
based assumptions like - "most original numbers are small positive numbers" -
but the lower bits of the secret (which are used most) are also 
most impossible to extract this way as any combination results in a
set of small numbers if the original set was indeed a set of small numbers.

## ⏱️ Performance

> [!Tip]
> **TLDR;** The takeaway here is encoding and decoding is very cheap.  
> It literally can be done millions of times per second on any HW around.

Some rough numbers for encoding and decoding all values between 
-1 million and +1 million with a minimal length of 8 in `MIXED` mode.
This means padding was used all the time (worst case scenario; 
encoding/decoding without padding would be noticeably faster).
```
Benchmark                       Mode  Cnt    Score    Error  Units
CoderAvgBenchmark.decodeDouble  avgt    3  183.560 ± 35.838  ns/op
CoderAvgBenchmark.decodeFloat   avgt    3  150.219 ± 30.255  ns/op
CoderAvgBenchmark.decodeInt     avgt    3  110.799 ± 19.256  ns/op
CoderAvgBenchmark.decodeLong    avgt    3  110.073 ± 32.172  ns/op
CoderAvgBenchmark.encodeDouble  avgt    3  149.111 ± 26.844  ns/op
CoderAvgBenchmark.encodeFloat   avgt    3   75.705 ±  5.836  ns/op
CoderAvgBenchmark.encodeInt     avgt    3   61.169 ±  5.636  ns/op
CoderAvgBenchmark.encodeLong    avgt    3   58.463 ±  7.764  ns/op
CoderAvgBenchmark.recodeDouble  avgt    3  351.010 ± 38.346  ns/op
CoderAvgBenchmark.recodeFloat   avgt    3  234.458 ± 62.205  ns/op
CoderAvgBenchmark.recodeInt     avgt    3  184.654 ± 26.260  ns/op
CoderAvgBenchmark.recodeLong    avgt    3  182.452 ± 21.883  ns/op
```
(decode = op is just decoding, encode = op is just encoding, recode = op is encoding and decoding) 

This wasn't a very accurate run as it lacks in iterations and forks (I just don't have the patience 😂)
but rerunning the benchmark a bunch has shown that if anything a more accurate Score is smaller (faster).

Also, any attempt to do better isn't worth much as this was running on 2018 laptop HW while having 
development tools running as well.
The intent here is to have a ballpark number; encoding is around 100ns, decoding around 150ns,
double is worst, long is best, closely followed by int and float.
These numbers all make sense considering the work done in the different scenarios.
The algorithm is build for `long` so it is expected to do best (for same length).
`double` also does worse since it does need 20 characters most of the time.
Very large long values will move towards the double score but never quit get as high (slow).

## 🧮 Algorithm
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

#### Padding

If the resulting character sequence is shorter than the target minimum length
padding is added on the left. For a single missing character the `Mode.pad1` is added.
For 2 or more missing characters `Mode.padN` is added left most followed by
the number of additional padding bytes left to the 2nd length encoding character.

Assuming the example from above should be padded to different minimum length, again using `Mode.UPPER`.

```
character        9   8   7   6   5   4   3   2   1   0  off
UPPER character              C   H   3   V   G   E   3             
secret          100 101 001 100 110 010 001 110 000 101 00
character index              =1  =5  =3  =5  =4  =1  =3 --
table index          2   1   0   3   2   1   0   3   2  (2)
padding 1                9   C   H   3   V   G   E   3
padding 2            8   T   C   H   3   V   G   E   3
padding 3        8   1   S   C   H   3   V   G   E   3
```
The `T` encodes zero additional padding bytes (that follow the `T`).
It is encoded as `T` because the count (0) is XORed with the lowest 3 bits of the 32 bit (high/low int) secret (4).
The table used continuous the cycle so T encodes the character at index 4 in the table at index 1.

If there are padding characters right to the padding count this cycles through the existing
results for value XOR secret right to left but encodes the value with the table belonging to
the padding character's position. In the example we get the `S` because the right most XORed
value was 3, the table for the padding character is 1, so the character at index 3 in the table at index 1.

To not always lead padded IDs with the padding indicator (`Mode.pad1` or `Mode.padN`) the character
finally switches place with the character at the index resulting from 
bit-count of value XOR secret (full 32bit) modulo the ID target length.

#### Flipping
As negative numbers usually have leading 1s (not leading zeros) it is often preferable to do a bitwise flip and
encode the flipped number instead. This always takes place before any of the above. If the number is flipped
the `Mode.flip` marker character is prepended. After the encoding is done the marker is the also swapped to another
position based on the bit-count of the original value (64 bit) module the ID length.
If padding is available the flip bit takes one of the padding places. Otherwise, it is "extra".

#### Joining
When multiple numbers are encoded each number is encoded as described above.
The parts are then joined (or seperated) by the `Mode.join` character.
The padding to reach the target length is equally distributed on the individual numbers.
