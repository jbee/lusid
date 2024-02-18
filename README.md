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
* 🗲 Low overhead, algorithm is fast and mostly allocation free
* 🔧 Easy to create custom modes for specific target patterns
* ⏨ integer numbers require less or as many characters as the number written in decimal
* 🔢 Can generate an ID for multiple numbers
* 📢 The character mapping can be public without compromising the number or secret
* 💂 No amount of encoded IDs will help to disclose the original numbers or secret 

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
