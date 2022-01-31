(ns kpow.secure
  (:require [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log]
            [kpow.secure.key :as key])
  (:import (java.io StringReader)
           (java.nio ByteBuffer)
           (java.nio.charset StandardCharsets)
           (java.security SecureRandom)
           (javax.crypto SecretKey Cipher)
           (javax.crypto.spec IvParameterSpec)
           (java.util Base64 Properties))
  (:gen-class))

;; scheme version static as v1 for now and encoded into the message as first byte
(def scheme-v1 (unchecked-byte 1))
(def cipher-algorithm "AES/CBC/PKCS5Padding")

(defn random-iv
  "Generate a 16-byte random IvParameterSpec"
  []
  (let [bytes (byte-array 16)]
    (.nextBytes (SecureRandom.) bytes)
    (IvParameterSpec. bytes)))

(defn cipher-bytes
  "Produce cipher-text from key / iv / plain-text input"
  [^SecretKey secret-key ^IvParameterSpec iv-spec ^String plain-text]
  (let [cipher (Cipher/getInstance cipher-algorithm)]
    (.init cipher Cipher/ENCRYPT_MODE secret-key ^IvParameterSpec iv-spec)
    (.doFinal cipher (.getBytes plain-text (.name StandardCharsets/UTF_8)))))

(defn plain-text
  "Produce plain-text from key / iv / cipher-bytes input"
  [^SecretKey secret-key ^IvParameterSpec iv-spec cipher-bytes]
  (let [cipher (Cipher/getInstance cipher-algorithm)]
    (.init cipher Cipher/DECRYPT_MODE secret-key ^IvParameterSpec iv-spec)
    (String. (.doFinal cipher cipher-bytes) (.name StandardCharsets/UTF_8))))

(defn encoded-payload
  "Produce a payload with format:
    * 1 byte scheme version (currently hard-coded to '1')
    * 1 byte initialization vector length (v1 scheme expects 16 bytes)
    * Initialization vector of size ^
    * Cipher text"
  [^SecretKey secret-key ^String plain-text]
  (let [payload-iv           (random-iv)
        payload-iv-bytes     (.getIV ^IvParameterSpec payload-iv)
        payload-bytes        (cipher-bytes secret-key payload-iv plain-text)
        payload-iv-length    (count payload-iv-bytes)
        payload-bytes-length (count payload-bytes)
        buffer               (ByteBuffer/allocate (+ 1 1 payload-iv-length payload-bytes-length))]
    (.put buffer (byte-array [scheme-v1]))
    (.put buffer (byte-array [(unchecked-byte payload-iv-length)]))
    (.put buffer ^"[B" payload-iv-bytes)
    (.put buffer ^"[B" payload-bytes)
    (String. (.encode (Base64/getEncoder) (.array buffer)) StandardCharsets/UTF_8)))

(defn decoded-payload
  "Validate the payload parts, then produce plain-text original of input cipher-text"
  [^SecretKey secret-key ^String encoded-payload]
  (let [buffer          (->> (.getBytes encoded-payload StandardCharsets/UTF_8)
                             (.decode (Base64/getDecoder))
                             (ByteBuffer/wrap))
        message-version (unchecked-int (.get buffer))
        iv-length       (unchecked-int (.get buffer))]
    (when-not (= 1 message-version)
      (throw (IllegalArgumentException. (format "invalid scheme version: %s" message-version))))
    (when-not (= 16 iv-length)
      (throw (IllegalArgumentException. (format "invalid initialization vector size: %s" iv-length))))
    (let [iv-bytes (byte-array iv-length)]
      (.get buffer iv-bytes)
      (let [cypher-bytes (byte-array (.remaining buffer))]
        (.get buffer cypher-bytes)
        (plain-text secret-key (IvParameterSpec. iv-bytes) cypher-bytes)))))

(defn encrypt
  [key-text plain-text]
  (encoded-payload (key/import-key key-text) plain-text))

(defn encrypt-file
  ([key-file in-file]
   (encrypt (slurp key-file) (slurp in-file)))
  ([key-file in-file out-file]
   (spit out-file (encrypt-file key-file in-file))
   (log/infof "\n\nEncrypted: %s > %s" in-file out-file)))

(defn decrypt
  [key-text encoded-payload]
  (decoded-payload (key/import-key key-text) encoded-payload))

(defn decrypt-file
  ([key-file in-file]
   (decrypt (slurp key-file) (slurp in-file)))
  ([key-file in-file out-file]
   (spit out-file (decrypt-file key-file in-file))
   (log/infof "\n\nDecrypted: %s > %s" in-file out-file)))

(defn ->props
  [text]
  (let [props (Properties.)]
    (.load props (StringReader. text))
    props))

(defn ->map
  [text]
  (into {} (->props text)))

(def cli-options
  [["-e" "--encrypt FILE" "File to encrypt"]
   ["-d" "--decrypt FILE" "File to decrypt"]
   ["-p" "--key-file KEY-FILE" "(required) File containing base64 encryption key"]
   ["-o" "--out-file OUT-FILE" "(optional) File for encrypted/decrypted output, default: [FILE].(enc|dec)"]
   ["-h" "--help"]])

(defn -main [& args]
  (let [{:keys [options summary errors]} (cli/parse-opts args cli-options)
        {:keys [encrypt decrypt key-file out-file help]} options]
    (try
      (cond
        errors (log/error (str "\n\n" errors))
        (or help (not (or encrypt decrypt))) (log/info (str "\n\n" summary))
        (and (or encrypt decrypt) (not key-file)) (log/info "\n\nRequired: --keyfile KEY-FILE  File containing base64 encryption key")
        encrypt (encrypt key-file encrypt (or out-file (str encrypt ".aes")))
        decrypt (decrypt key-file decrypt (or out-file (str decrypt ".plain"))))
      (catch Exception ex
        (log/errorf ex "\nFailed to %s %s" (if encrypt "encrypt" "decrypt") (or encrypt decrypt))))))