# Kpow Secure: Key Generation and Payload Encryption

[![CircleCI](https://circleci.com/gh/operatr-io/kpow-secure.svg?style=svg&circle-token=6e95b380dbe34c368a074c2c061053cebaa1a29d)](https://circleci.com/gh/operatr-io/kpow-secure)

This library is used to secure configuration for [kPow for Apache Kafka](https://kpow.io).

See the [kPow Secure Configuration Guide](https://kpow.io) for specifics on secure configuration for kPow.

The chosen algorithms are suited to low-volume encryption of local files.

## Capabilities

 * 256-bit AES encryption key generation from a passphrase and salt using PBKDF2WithHmacSHA256
 * AES/CBC/PKCS5Padding cipher-text with random IV encryption / decryption
 * Base64 key serialization / deserialization of keys for import / export
 * CLI interface for key generation and encryption

## Key Generation

* Generate a key with a random salt (not reproducible from inputs)

```clojure
(key/secret-key "aquickredfox")
=> #object[javax.crypto.spec.SecretKeySpec 0x9a9f63e "javax.crypto.spec.SecretKeySpec@15b1a"]
```

* Generate a key with a chosen salt (reproducible from inputs)

```clojure
(key/secret-key "aquickredfox" "asalt")
=> #object[javax.crypto.spec.SecretKeySpec 0x5c2ac756 "javax.crypto.spec.SecretKeySpec@fffe96a4"]
```

## Key Serialization

* Serialize a key to Base64 text

```clojure
(key/export-key (key/secret-key "aquickredfox" "asalt"))
=> "Ic9cChI5tatKL1pzbQqVzJ0Tv0DsiEa7ES/CW1IVgok="
```

* Produce a key from Base64 text

```clojure
(key/import-key "Ic9cChI5tatKL1pzbQqVzJ0Tv0DsiEa7ES/CW1IVgok=")
=> #object[javax.crypto.spec.SecretKeySpec 0x3d2b5928 "javax.crypto.spec.SecretKeySpec@fffe96a4"]
```

## Payload Encryption

* Produce an encrypted payload with random initialization vector from key and plaintext

```clojure
(secure/encoded-payload
 (key/secret-key "aquickredfox" "some-salt")
 (str "SSL_KEYSTORE_PASSWORD=keypass1234\n"
      "SSL_TRUSTSTORE_PASSWORD=trustpass1234"))
=> "ARAOGa3BAZ2TMxbU1aj+tFYfNHNwnRh3r/w2sG7FA4L7fVRzArpzrxAd2dUovyDfel++FHgW1IFrinZddTo+KiYFYm2rsn+ul65eQ1L5t9MsBq3LpuGjoFDSxkYFZweo/w0="
```

## Payload Decryption

* Produce plain text from key and encrypted payload

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

The passphrase and key are read from a local files to ensure they are not observable in your shell history.

* Show the help menu

```bash
java -cp target/kpow-secure-1.0.0-standalone.jar kpow.secure.key --help
```

```bash
16:29:49.803 [main] INFO kpow.secure.key -

  -g, --generate                  Generate a new secure key
  -p, --passfile PASSPHRASE-FILE  (required) File containing key passphrase
  -s, --salt SALT                 (optional) Salt to use with key generation, random if none provided
  -h, --help
```

* Generate a key with random salt (output written to stdout and file)

```bash
java -cp target/kpow-secure-1.0.0-standalone.jar kpow.secure.key --generate --passfile secure/passphrase.txt
```

```bash
21:35:08.889 [main] INFO kpow.secure.key -

Kpow Secure Key:
----------------

mO3TWj8Z7ANjC75NK4ufNP6n2I1W9JEtNUeWUGABxN0=

Key file written to: secure/passphrase.txt.key

Random salt used, this key cannot be regenerated.
```

* Generate a key with chosen salt (output written to stdout and file)

```bash
java -cp target/kpow-secure-1.0.0-standalone.jar kpow.secure.key --generate --passfile secure/passphrase.txt --salt abcdef
```

```bash
21:36:45.911 [main] INFO kpow.secure.key -

Kpow Secure Key:
----------------

M3dREc8AHDLPxv8DoAMaK51EO+yZizkcvTlzRjv2kx4=

Key file written to: secure/passphrase.txt.key

This key can be regenerated with the same passphrase and salt.
```

## Payload Encryption

* Show the help menu

```bash
java -cp target/kpow-secure-1.0.0-standalone.jar kpow.secure --help
```

```bash
21:41:52.561 [main] INFO kpow.secure -

  -e, --encrypt TEXT-FILE     Encrypt plain text file
  -d, --decrypt PAYLOAD-FILE  Decrypt payload file
  -p, --keyfile KEY-FILE      (required) File containing base64 encryption key
  -h, --help
```

* Encrypt a plain-text file

```bash
java -cp target/kpow-secure-1.0.0-standalone.jar kpow.secure --encrypt secure/config.env --keyfile secure/passphrase.txt.key
```

```bash
21:44:58.533 [main] INFO kpow.secure -

Plain text encrypted: secure/config.env > secure/config.env.payload
```

* Confirm the encrypted payload

```bash
cat secure/config.env.payload
```

```bash
ARBqlg5MtXNJJbJIElZYiN189bfavvfTlhz2qLYqMDyY0s+aErl3znh/fbcErByfFwukelX1ooHvwWD7MzE6KkIRsfHaOOOL6fozzDJsc3fJlVmnLs25o3LBuu+7OFpxNIcKg3zu6FUvZ992z75Sj8xjtJtNcEAdoJJEBQWQYu0AbX3GoJE7ALrPr45vg8LHA3Iy+pgj5qHAqNCABza0rjrNsUa3l0DgM0SwC83LwLEW7a4ldAtXNxlwk4UYkLIP1e+ipVtVz58dllWZS7WS87oj%
```

* Decrypt the payload

```bash
java -cp target/kpow-secure-1.0.0-standalone.jar kpow.secure --decrypt secure/config.env.payload --keyfile secure/passphrase.txt.key
```

```bash
21:46:57.218 [main] INFO kpow.secure -

Payload decrypted: secure/config.env.payload > secure/config.env.payload.plain
```

* Confirm the decrypted plain-text

```bash
cat secure/config.env.payload.plain
```

```bash
SECURITY_PROTOCOL=SASL_PLAINTEXT
SASL_MECHANISM=PLAIN
SASL_JAAS_CONFIG=org.apache.kafka.common.security.plain.PlainLoginModule required username="kpow" password="kpow-secret";
```
