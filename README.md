# Kpow Secure: Key Generation and Payload Encryption

[![Kpow secure test](https://github.com/factorhouse/kpow-secure/actions/workflows/ci.yml/badge.svg)](https://github.com/factorhouse/kpow-secure/actions/workflows/ci.yml)
[![Clojars Project](https://img.shields.io/clojars/v/io.factorhouse/kpow-secure.svg)](https://clojars.org/io.factorhouse/kpow-secure)


A library for encrypted Kafka configuration with Java AES encryption and PBKDF2 master key generation.

Encrypted configuration is **not a replacement for secret managers**, but may help in environments with limited secret management options.

Can be used standalone or integrated with your application. This library provides:

* [Clojure implementation](https://github.com/factorhouse/kpow-secure/blob/main/README.md#clojure-implementation)
* [Command line interface](https://github.com/factorhouse/kpow-secure/blob/main/README.md#command-line-interface)

This library is used as an option to secure configuration for [Kpow for Apache Kafka](https://kpow.io) since [v88.2](https://kpow.io/releases/88-2/).

See the [Kpow Secure Configuration Guide](https://github.com/factorhouse/kpow/tree/main/secure-config) for specifics on secure configuration for Kpow.

## Capabilities

 * 256-bit AES encryption key generation from a passphrase and salt using PBKDF2WithHmacSHA256
 * AES/CBC/PKCS5Padding cipher-text with random IV encryption / decryption
 * Base64 key serialization / deserialization of keys for import / export
 * Base64 payload encoding of scheme version, IV length, IV, and cipher text
 * Payload interpretation (decrypt config into `java.util.Properties` or `clojure.lang.PersistentArrayMap`)
 * CLI interface for key generation and encryption / decryption
 * Java API for easy decryption of payload into `java.util.String` or `java.util.Properties`
 * Key lookup via KPOW_SECURE_KEY or KPOW_SECURE_KEY_LOCATION environment variables

## License

This project is released under the MIT license.

## Clojure Implementation

### Key Generation

#### Generate a key with a random salt (not reproducible from inputs)

```clojure
(key/secret-key "aquickredfox")
```

```clojure
=> #object[javax.crypto.spec.SecretKeySpec 0x9a9f63e "javax.crypto.spec.SecretKeySpec@15b1a"]
```

#### Generate a key with a chosen salt (reproducible from inputs)

```clojure
(key/secret-key "aquickredfox" "asalt")
```

```clojure
=> #object[javax.crypto.spec.SecretKeySpec 0x5c2ac756 "javax.crypto.spec.SecretKeySpec@fffe96a4"]
```

### Key Serialization

#### Serialize a key to base64 text

```clojure
(key/export-key (key/secret-key "aquickredfox" "asalt"))
```

```clojure
=> "Ic9cChI5tatKL1pzbQqVzJ0Tv0DsiEa7ES/CW1IVgok="
```

#### Produce a key from base64 text

```clojure
(key/import-key "Ic9cChI5tatKL1pzbQqVzJ0Tv0DsiEa7ES/CW1IVgok=")
```

```clojure
=> #object[javax.crypto.spec.SecretKeySpec 0x3d2b5928 "javax.crypto.spec.SecretKeySpec@fffe96a4"]
```

### Encryption

#### Encrypted payload from base64 encoded key and plaintext

```clojure
(secure/encrypted
 "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88="
 (str "SSL_KEYSTORE_PASSWORD=keypass1234\n"
      "SSL_TRUSTSTORE_PASSWORD=trustpass1234"))
```

```clojure
=> "ARAOGa3BAZ2TMxbU1aj+tFYfNHNwnRh3r/w2sG7FA4L7fVRzArpzrxAd2dUovyDfel++FHgW1IFrinZddTo+KiYFYm2rsn+ul65eQ1L5t9MsBq3LpuGjoFDSxkYFZweo/w0="
```

#### Encrypted payload from SecretKey and plaintext

```clojure
(secure/encoded-payload
 (key/secret-key "aquickredfox" "some-salt")
 (str "SSL_KEYSTORE_PASSWORD=keypass1234\n"
      "SSL_TRUSTSTORE_PASSWORD=trustpass1234"))
```

```clojure
=> "ARAOGa3BAZ2TMxbU1aj+tFYfNHNwnRh3r/w2sG7FA4L7fVRzArpzrxAd2dUovyDfel++FHgW1IFrinZddTo+KiYFYm2rsn+ul65eQ1L5t9MsBq3LpuGjoFDSxkYFZweo/w0="
```

### Decryption

#### Plaintext from serialized key and encrypted payload

```clojure
(secure/decrypted
 "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88="
 "ARAOGa3BAZ2TMxbU1aj+tFYfNHNwnRh3r/w2sG7FA4L7fVRzArpzrxAd2dUovyDfel++FHgW1IFrinZddTo+KiYFYm2rsn+ul65eQ1L5t9MsBq3LpuGjoFDSxkYFZweo/w0=")
```

```clojure
=> "SSL_KEYSTORE_PASSWORD=keypass1234\nSSL_TRUSTSTORE_PASSWORD=trustpass1234"
```

#### Plaintext from SecretKey and encrypted payload

```clojure
(secure/decoded-text
 (key/secret-key "aquickredfox" "some-salt")
 "ARAOGa3BAZ2TMxbU1aj+tFYfNHNwnRh3r/w2sG7FA4L7fVRzArpzrxAd2dUovyDfel++FHgW1IFrinZddTo+KiYFYm2rsn+ul65eQ1L5t9MsBq3LpuGjoFDSxkYFZweo/w0=")
```

```clojure
=> "SSL_KEYSTORE_PASSWORD=keypass1234\nSSL_TRUSTSTORE_PASSWORD=trustpass1234"
```

### Interpretation

Kpow-Secure will interpret payloads where the plaintext is in `java.util.Properties` format.

See [dev-resources/secure/props.env](dev-resources/secure/props.env) for an example of the flexibility of Java Properties encoding.

#### Interpret payload as clojure.lang.PersistentArrayMap

```clojure
(-> (secure/decrypted (slurp "dev-resources/secure/passphrase.key") (slurp "dev-resources/secure/config.env.aes"))
    (secure/->map))
```

```
=>
{"SASL_JAAS_CONFIG"        "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"kpow\" password=\"kpow-secret\";"
 "SASL_MECHANISM"          "PLAIN"
 "SECURITY_PROTOCOL"       "SASL_PLAINTEXT"
 "SSL_TRUSTSTORE_LOCATION" "/ssl/truststore.jks"
 "SSL_TRUSTSTORE_PASSWORD" "password1234"}
```

#### Interpret payload as java.util.Properties

```clojure
(-> (secure/decrypted (slurp "dev-resources/secure/passphrase.key") (slurp "dev-resources/secure/props.env.aes"))
    (secure/->props))
```

```clojure
=>
{"sasl.jaas.config"        "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"kpow\" password=\"kpow-secret\";"
 "sasl.mechanism"          "PLAIN"
 "security.protocol"       "SASL_PLAINTEXT"
 "ssl.truststore.location" "/ssl/truststore.jks"
 "ssl.truststore.password" "1234"}
```

## Command Line Interface

This library exposes key generation and payload encryption / decryption functions via a CLI.

You can uberjar this project, or include the library within your own project and uberjar that.

### Key Generation

The passphrase is read from a local file to ensure it is not observable in your shell history.

#### Show the help menu

```bash
java -cp target/kpow-secure-1.0.0-standalone.jar kpow.secure.key --help
```

```bash
21:27:09.090 [main] INFO kpow.secure.key -

  -p, --pass-file PASSPHRASE-FILE  (required) File containing key passphrase
  -s, --salt SALT                  (optional) Salt to use with key generation, random if none provided
  -o, --out-file OUT-FILE          (optional) File for key output, default: [PASSPHRASE-FILE].key
  -h, --help
```

#### Generate a key with random salt

```bash
java -cp target/kpow-secure-1.0.0-standalone.jar kpow.secure.key --pass-file dev-resources/secure/passphrase.txt --out-file dev-resources/secure/passphrase.key
```

```bash
19:46:50.912 [main] INFO kpow.secure.key -

Kpow Secure Key:
----------------

nP+O/6xOu9+9+JZFYgfhS+R6x4OjVgToP9DlM1bx35g=

Key file written to: dev-resources/secure/passphrase.key

Random salt used, this key cannot be regenerated.
```

#### Generate a key with chosen salt

```bash
java -cp target/kpow-secure-1.0.0-standalone.jar kpow.secure.key --pass-file dev-resources/secure/passphrase.txt --salt abcdef --out-file dev-resources/secure/mykey.aes
```

```bash
19:48:01.933 [main] INFO kpow.secure.key -

Kpow Secure Key:
----------------

88wRMz4DuaRWOmyKPb8IgmY4kZAyQvPiRVxUy79OgL8=

Key file written to: dev-resources/secure/mykey.aes

This key can be regenerated with the same passphrase and salt.
```

### Encryption

#### Show the help menu

```bash
java -cp target/kpow-secure-1.0.0-standalone.jar kpow.secure --help
```

```bash
23:01:46.551 [main] INFO kpow.secure -

      --key TEXT           Base64 encoded key
      --key-file FILE      File containing base64 encoded key
      --encrypt TEXT       Text to encrypt
      --decrypt TEXT       Base64 encoded payload text
      --encrypt-file FILE  File containing text to encrypt
      --decrypt-file FILE  File containing base64 encoded payload text
      --out-file FILE      (optional) File for encrypted/decrypted output
  -h, --help
```

#### Encrypt text directly on the command line 

```bash
java -cp target/kpow-secure-1.0.0-standalone.jar kpow.secure --encrypt supersecrettext --key 88wRMz4DuaRWOmyKPb8IgmY4kZAyQvPiRVxUy79OgL8=
```

```bash
22:19:30.511 [main] INFO kpow.secure -

Kpow Encrypted:
---------------

ARB41R5ZsMNkjNnr3YD+RK2wT6ywAgR0KpcCpLYH/g3rxQ==
```

#### Decrypt text directly on the command line 

```bash
java -cp target/kpow-secure-1.0.0-standalone.jar kpow.secure --decrypt ARDO7O0legO6PFvIGq/ILD/gekC43knuHqCxhvJoyOeCBw== --key-file dev-resources/secure/passphrase.key
```

```bash
22:21:16.491 [main] INFO kpow.secure -

Kpow Decrypted:
---------------

supersecrettext
```

#### Encrypt a plaintext file

```bash
java -cp target/kpow-secure-1.0.0-standalone.jar kpow.secure --encrypt-file dev-resources/secure/config.env --key-file dev-resources/secure/passphrase.key --out-file dev-resources/secure/config.aes
```

```bash
22:33:28.807 [main] INFO kpow.secure -

Kpow Encrypted:
---------------

> dev-resources/secure/config.aes
```

#### View the encrypted payload

```bash
cat dev-resources/secure/config.aes
```

```bash
ARD9I/BlocgOwYfsW/oXrJtY/u2AnMWm/ewWIm7iDJrSkkGnQbM38ZbCM1hWfYZLHpIo99LATlgtnR4rcSjDIEY01wZTsZUyxLXKMoH1sX31FwoywxjmGPooMQg2d6VIHpLGeTsrmD1HQ2U9miIr01w5moMy4U6/UTAm1o+f8xGmR5l2sMj59tddK5VTC9BRs0L4ptxj+bR/QhItwL2qnqExnsEBTUOwrrTiHZySXhr8iJWvD1WIFL374KmneLxFhqMuIiY1D3v9/ChlyCojvh5JR6pJ3ZuIK3HP2YbjZSTSliz7mV5hMI021E4MN8hWE4L3poLhHY5KWVVb6Ma5kQAt2M5t9Ij8HkdtjMgxrva+kCtXUg81F9WoWmsc3xQcY5o=
```

### Decryption

#### Decrypt the payload

```bash
java -cp target/kpow-secure-1.0.0-standalone.jar kpow.secure --decrypt-file dev-resources/secure/config.aes --key-file dev-resources/secure/passphrase.key
```

```bash
22:40:11.998 [main] INFO kpow.secure -

Kpow Decrypted:
---------------

SECURITY_PROTOCOL=SASL_PLAINTEXT
SASL_MECHANISM=PLAIN
SASL_JAAS_CONFIG=org.apache.kafka.common.security.plain.PlainLoginModule required username="kpow" password="kpow-secret";
SSL_TRUSTSTORE_LOCATION=/ssl/truststore.jks```
```

## License

Distributed under the Apache 2.0 License.

Copyright (c) [Factor House](https://factorhouse.io)
