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
* 🆔 Generates unique IDs for any `long`, `int`, `double`, `float` number
* 🗲 Low overhead, algorithm is fast and mostly allocation free
* 🔧 Easy to create custom modes for specific target patterns
* ⏨ integer numbers require less or as many characters as the number written in decimal
* 🔢 Can generate IDs for multiple numbers
* 📢 The character mapping can be public without compromising the number of secret
* 💂 No amount of encoded IDs will disclose the original numbers or the secret used
