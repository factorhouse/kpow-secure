# Kpow Secure: Key Generation and Payload Encryption

[![CircleCI](https://circleci.com/gh/operatr-io/kpow-secure.svg?style=svg&circle-token=6e95b380dbe34c368a074c2c061053cebaa1a29d)](https://circleci.com/gh/operatr-io/kpow-secure)

This library is used to secure configuration for [kPow for Apache Kafka](https://kpow.io).

See the [kPow Secure Configuration Guide](https://kpow.io) for specifics on secure configuration for kPow.

## Capabilities

 * 256-bit AES encryption key generation from a passphrase and salt using PBKDF2
 * Plain-text payload encryption / decryption
 * CLI interface for key generation and encryption