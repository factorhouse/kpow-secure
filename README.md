# Kpow Secure: Key Generation and Payload Encryption

[![CircleCI](https://circleci.com/gh/operatr-io/kpow-secure.svg?style=svg&circle-token=6e95b380dbe34c368a074c2c061053cebaa1a29d)](https://circleci.com/gh/operatr-io/kpow-secure)

This library is used to secure configuration for [kPow for Apache Kafka](https://kpow.io).

See the [kPow Secure Configuration Guide](https://kpow.io) for specifics on secure configuration for kPow.

The chosen algorithms are suited to low-volume encryption of local files.

## Capabilities

 * 256-bit AES encryption key generation from a passphrase and salt using PBKDF2WithHmacSHA256
 * AES/CBC/PKCS5Padding cipher-text with random IV encryption / decryption
 * Base64 key serialization / deserialization of keys for import / export
 * Payload interpretation (Clojure Map or Java Properties)
 * CLI interface for key generation and encryption
 * Java API for secret decryption / interop

## Key Generation

* Generate a key with a random salt (not reproducible from inputs)

```clojure
(key/secret-key "aquickredfox")
```

```clojure
=> #object[javax.crypto.spec.SecretKeySpec 0x9a9f63e "javax.crypto.spec.SecretKeySpec@15b1a"]
```

* Generate a key with a chosen salt (reproducible from inputs)

```clojure
(key/secret-key "aquickredfox" "asalt")
```

```clojure
=> #object[javax.crypto.spec.SecretKeySpec 0x5c2ac756 "javax.crypto.spec.SecretKeySpec@fffe96a4"]
```

## Key Serialization

* Serialize a key to Base64 text

```clojure
(key/export-key (key/secret-key "aquickredfox" "asalt"))
```

```clojure
=> "Ic9cChI5tatKL1pzbQqVzJ0Tv0DsiEa7ES/CW1IVgok="
```

* Produce a key from Base64 text

```clojure
(key/import-key "Ic9cChI5tatKL1pzbQqVzJ0Tv0DsiEa7ES/CW1IVgok=")
```

```clojure
=> #object[javax.crypto.spec.SecretKeySpec 0x3d2b5928 "javax.crypto.spec.SecretKeySpec@fffe96a4"]
```

## Payload Encryption

* Encrypted payload from serialized key and plain text

```clojure
(secure/encrypt
 "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88="
 (str "SSL_KEYSTORE_PASSWORD=keypass1234\n"
      "SSL_TRUSTSTORE_PASSWORD=trustpass1234"))
```

```clojure
=> "ARAOGa3BAZ2TMxbU1aj+tFYfNHNwnRh3r/w2sG7FA4L7fVRzArpzrxAd2dUovyDfel++FHgW1IFrinZddTo+KiYFYm2rsn+ul65eQ1L5t9MsBq3LpuGjoFDSxkYFZweo/w0="
```

* Encrypted payload from SecretKey and plain text

```clojure
(secure/encoded-payload
 (key/secret-key "aquickredfox" "some-salt")
 (str "SSL_KEYSTORE_PASSWORD=keypass1234\n"
      "SSL_TRUSTSTORE_PASSWORD=trustpass1234"))
```

```clojure
=> "ARAOGa3BAZ2TMxbU1aj+tFYfNHNwnRh3r/w2sG7FA4L7fVRzArpzrxAd2dUovyDfel++FHgW1IFrinZddTo+KiYFYm2rsn+ul65eQ1L5t9MsBq3LpuGjoFDSxkYFZweo/w0="
```

## Payload Decryption

* Plain text from serialized key and encrypted payload

```clojure
(secure/decrypt
 "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88="
 "ARAOGa3BAZ2TMxbU1aj+tFYfNHNwnRh3r/w2sG7FA4L7fVRzArpzrxAd2dUovyDfel++FHgW1IFrinZddTo+KiYFYm2rsn+ul65eQ1L5t9MsBq3LpuGjoFDSxkYFZweo/w0=")
```

```clojure
=> "SSL_KEYSTORE_PASSWORD=keypass1234\nSSL_TRUSTSTORE_PASSWORD=trustpass1234"
```

* Plain text from SecretKey and encrypted payload

```clojure
(secure/decoded-payload
 (key/secret-key "aquickredfox" "some-salt")
 "ARAOGa3BAZ2TMxbU1aj+tFYfNHNwnRh3r/w2sG7FA4L7fVRzArpzrxAd2dUovyDfel++FHgW1IFrinZddTo+KiYFYm2rsn+ul65eQ1L5t9MsBq3LpuGjoFDSxkYFZweo/w0=")
```

```clojure
=> "SSL_KEYSTORE_PASSWORD=keypass1234\nSSL_TRUSTSTORE_PASSWORD=trustpass1234"
```

## Payload Interpretation

* Transform Java Properties encoded payload text into a Clojure Map or Java Properties

```clojure
(-> (secure/decrypt-file "dev-resources/secure/passphrase.key" "dev-resources/secure/config.env.aes")
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

* See [dev-resources/secure/props.env](dev-resources/secure/props.env) for an example of the flexibility of Java Properties encoding

```clojure
(-> (secure/decrypt-file "dev-resources/secure/passphrase.key" "dev-resources/secure/props.env.aes")
    (secure/->map))
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

This library exposes both key generation and payload encryption functions via a CLI.

You can uberjar this project, or include the library within your own project and uberjar it.

### Key Generation

The passphrase and key are read from a local files to ensure they are not observable in your shell history.

* Show the help menu

```bash
java -cp target/kpow-secure-1.0.0-standalone.jar kpow.secure.key --help
```

```bash
19:45:46.477 [main] INFO kpow.secure.key -

  -p, --pass-file PASSPHRASE-FILE  (required) File containing key passphrase
  -s, --salt SALT                  (optional) Salt to use with key generation, random if none provided
  -o, --out-file OUT-FILE          (optional) File for key output, default: [PASSPHRASE-FILE].key
  -h, --help
```

* Generate a key with random salt (output written to stdout and default key file)

```bash
java -cp target/kpow-secure-1.0.0-standalone.jar kpow.secure.key --pass-file dev-resources/secure/passphrase.txt
```

```bash
19:46:50.912 [main] INFO kpow.secure.key -

Kpow Secure Key:
----------------

nP+O/6xOu9+9+JZFYgfhS+R6x4OjVgToP9DlM1bx35g=

Key file written to: dev-resources/secure/passphrase.txt.key

Random salt used, this key cannot be regenerated.
```

* Generate a key with chosen salt (output written to stdout and specific key file)

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

## Payload Encryption

* Show the help menu

```bash
java -cp target/kpow-secure-1.0.0-standalone.jar kpow.secure --help
```

```bash
19:49:12.446 [main] INFO kpow.secure -

  -e, --encrypt FILE       File to encrypt
  -d, --decrypt FILE       File to decrypt
  -p, --key-file KEY-FILE  (required) File containing base64 encryption key
  -o, --out-file OUT-FILE  (optional) File for encrypted/decrypted output, default: [FILE].(aes|plain)
  -h, --help
```

* Encrypt a plain-text file

```bash
java -cp target/kpow-secure-1.0.0-standalone.jar kpow.secure --encrypt dev-resources/secure/config.env --key-file dev-resources/secure/passphrase.txt.key
```

```bash
19:56:34.117 [main] INFO kpow.secure -

Encrypted: dev-resources/secure/config.env > dev-resources/secure/config.env.aes
```

* Confirm the encrypted payload

```bash
cat dev-resources/secure/config.env.aes
```

```bash
ARD9I/BlocgOwYfsW/oXrJtY/u2AnMWm/ewWIm7iDJrSkkGnQbM38ZbCM1hWfYZLHpIo99LATlgtnR4rcSjDIEY01wZTsZUyxLXKMoH1sX31FwoywxjmGPooMQg2d6VIHpLGeTsrmD1HQ2U9miIr01w5moMy4U6/UTAm1o+f8xGmR5l2sMj59tddK5VTC9BRs0L4ptxj+bR/QhItwL2qnqExnsEBTUOwrrTiHZySXhr8iJWvD1WIFL374KmneLxFhqMuIiY1D3v9/ChlyCojvh5JR6pJ3ZuIK3HP2YbjZSTSliz7mV5hMI021E4MN8hWE4L3poLhHY5KWVVb6Ma5kQAt2M5t9Ij8HkdtjMgxrva+kCtXUg81F9WoWmsc3xQcY5o=
```

* Decrypt the payload

```bash
java -cp target/kpow-secure-1.0.0-standalone.jar kpow.secure --decrypt dev-resources/secure/config.env.aes --key-file dev-resources/secure/passphrase.txt.key
```

```bash
19:58:27.901 [main] INFO kpow.secure -

Decrypted: dev-resources/secure/config.env.aes > dev-resources/secure/config.env.aes.plain
```

* Confirm the decrypted plain-text

```bash
cat dev-resources/secure/config.env.aes.plain
```

```bash
SECURITY_PROTOCOL=SASL_PLAINTEXT
SASL_MECHANISM=PLAIN
SASL_JAAS_CONFIG=org.apache.kafka.common.security.plain.PlainLoginModule required username="kpow" password="kpow-secret";
SSL_TRUSTSTORE_LOCATION=/ssl/truststore.jks
SSL_TRUSTSTORE_PASSWORD=password1234
```

## Java API

Kpow Secure is implemented in our langauge of choice, Clojure.

We provide a basic Decoder API in Java to allow encrypted payloads to be decoded to `java.util.Properties`

* Decode payload file with a serialized key file

```Java
Properties myProps = Decoder.loadProperties("/path/to/your.key", "/path/to/config.env.aes");
```

```clojure
;; Java API returns this as a java.util.Properties object
=> 
{"sasl.jaas.config"        "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"kpow\" password=\"kpow-secret\";"
 "sasl.mechanism"          "PLAIN"
 "security.protocol"       "SASL_PLAINTEXT"
 "ssl.truststore.location" "/ssl/truststore.jks"
 "ssl.truststore.password" "1234"}
``` 

* Decode payload text with a serialized key

```Java
Properties myProps = Decoder.properties("//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88=", "ARAOGa3BAZ2TMxbU1aj+tFYfNHNwnRh3r/w2sG7FA4L7fVRzArpzrxAd2dUovyDfel++FHgW1IFrinZddTo+KiYFYm2rsn+ul65eQ1L5t9MsBq3LpuGjoFDSxkYFZweo/w0=");
```

```clojure
;; Java API returns this as a java.util.Properties object
=> 
{"SSL_TRUSTSTORE_PASSWORD" "trustpass1234"
 "SSL_KEYSTORE_PASSWORD"   "keypass1234"}
``` 
