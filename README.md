# Kpow Secure: Key Generation and Payload Encryption

[![CircleCI](https://circleci.com/gh/operatr-io/kpow-secure.svg?style=svg&circle-token=6e95b380dbe34c368a074c2c061053cebaa1a29d)](https://circleci.com/gh/operatr-io/kpow-secure)

This library is used to secure configuration for [kPow for Apache Kafka](https://kpow.io).

See the [kPow Secure Configuration Guide](https://kpow.io) for specifics on secure configuration for kPow.

The chosen algorithms are suited to low-volume encryption of local files.

## Capabilities

 * 256-bit AES encryption key generation from a passphrase and salt using PBKDF2
 * Plain-text payload encryption / decryption
 * Base64 key serialization / deserialization
 * CLI interface for key generation and encryption

## Key Generation

Generate a key with a random salt (not reproducible from inputs)

```clojure
(key/secret-key "aquickredfox")
=> #object[javax.crypto.spec.SecretKeySpec 0x9a9f63e "javax.crypto.spec.SecretKeySpec@15b1a"]
```

Generate a key with a chosen salt (reproducible from inputs)

```clojure
(key/secret-key "aquickredfox" "asalt")
=> #object[javax.crypto.spec.SecretKeySpec 0x5c2ac756 "javax.crypto.spec.SecretKeySpec@fffe96a4"]
```

## Key Serialization

Serialize a key to Base64 text

```clojure
(key/export-key (key/secret-key "aquickredfox" "asalt"))
=> "Ic9cChI5tatKL1pzbQqVzJ0Tv0DsiEa7ES/CW1IVgok="
```

Produce a key from Base64 text

```clojure
(key/import-key "Ic9cChI5tatKL1pzbQqVzJ0Tv0DsiEa7ES/CW1IVgok=")
=> #object[javax.crypto.spec.SecretKeySpec 0x3d2b5928 "javax.crypto.spec.SecretKeySpec@fffe96a4"]
```

## Payload Encryption

Produce an encrypted payload with random initialization vector from key and plaintext

```clojure
(secure/encoded-payload
 (key/secret-key "aquickredfox" "some-salt")
 (str "SSL_KEYSTORE_PASSWORD=keypass1234\n"
      "SSL_TRUSTSTORE_PASSWORD=trustpass1234"))
=> "ARAOGa3BAZ2TMxbU1aj+tFYfNHNwnRh3r/w2sG7FA4L7fVRzArpzrxAd2dUovyDfel++FHgW1IFrinZddTo+KiYFYm2rsn+ul65eQ1L5t9MsBq3LpuGjoFDSxkYFZweo/w0="
```

## Payload Decryption

Produce plain text from key and encrypted payload

```clojure
(secure/decoded-payload
 (key/secret-key "aquickredfox" "some-salt")
 "ARAOGa3BAZ2TMxbU1aj+tFYfNHNwnRh3r/w2sG7FA4L7fVRzArpzrxAd2dUovyDfel++FHgW1IFrinZddTo+KiYFYm2rsn+ul65eQ1L5t9MsBq3LpuGjoFDSxkYFZweo/w0=")
=> "SSL_KEYSTORE_PASSWORD=keypass1234\nSSL_TRUSTSTORE_PASSWORD=trustpass1234"
```

## Command Line Interface

This library exposes both key generation and payload encryption functions via a CLI.

You can uberjar this project, or include the library within your own project and uberjar it.

### Key Generation

Show the help menu

```bash
java -cp target/kpow-secure-1.0.0-standalone.jar kpow.secure.key --help
16:29:49.803 [main] INFO kpow.secure.key -

  -g, --generate                  Generate a new secure key
  -p, --passfile PASSPHRASE-FILE  (required) File containing key passphrase
  -s, --salt SALT                 (optional) Salt to use with key generation, random if none provided
  -h, --help
```

Generate a key with random salt

```bash
java -cp target/kpow-secure-1.0.0-standalone.jar kpow.secure.key --generate --passfile pass.txt                                                             ✔  10271  16:29:49
16:30:53.994 [main] INFO kpow.secure.key -

Kpow Secure Key:
----------------

+mdHJeHGw7+SeF1yrClRdp2H672xn8UefnmwDmjBU84=

Random salt used, this key cannot be regenerated.
```

Generate a key with chosen salt

```bash
java -cp target/kpow-secure-1.0.0-standalone.jar kpow.secure.key --generate --passfile pass.txt --salt abcdef
16:31:32.027 [main] INFO kpow.secure.key -

Kpow Secure Key:
----------------

M3dREc8AHDLPxv8DoAMaK51EO+yZizkcvTlzRjv2kx4=

This key can be regenerated with the same passphrase and salt.
```
