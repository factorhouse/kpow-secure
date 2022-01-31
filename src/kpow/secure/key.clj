(ns kpow.secure.key
  (:require [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log])
  (:import (java.security SecureRandom)
           (javax.crypto SecretKey SecretKeyFactory)
           (javax.crypto.spec SecretKeySpec PBEKeySpec)
           (java.nio.charset StandardCharsets)
           (java.util Base64))
  (:gen-class))

(def key-enc-algorithm "AES")
(def key-gen-algorithm "PBKDF2WithHmacSHA256")
(def key-strength (int 256))
(def default-rounds (int 65536))

(defn export-key
  "Base64 encode the secret keys bytes"
  [^SecretKey secret-key]
  (.encodeToString (Base64/getEncoder) (.getEncoded secret-key)))

(defn import-key
  "Interpret Base64 encoded text as a SecretKeySpec"
  [^String key-text]
  (SecretKeySpec. (.decode (Base64/getDecoder) (str/trim key-text)) key-enc-algorithm))

(defn random-salt
  []
  (let [salt (byte-array 64)]
    (.nextBytes (SecureRandom.) salt)
    salt))

(defn secret-key
  "Generate a new AES SecretKeySpec from PBKDF2 passphrase, salt, and rounds"
  ([passphrase]
   (secret-key passphrase nil nil))
  ([passphrase salt]
   (secret-key passphrase salt nil))
  ([^String passphrase ^String salt ^Integer rounds]
   (-> (SecretKeyFactory/getInstance key-gen-algorithm)
       (.generateSecret (PBEKeySpec. (char-array passphrase)
                                     (or (some-> salt (.getBytes StandardCharsets/UTF_8)) (random-salt))
                                     (or rounds default-rounds)
                                     (int key-strength)))
       (.getEncoded)
       (SecretKeySpec. key-enc-algorithm))))

(defn generate-key
  [pass-file salt out-file]
  (let [secure-key (export-key (secret-key (slurp pass-file) salt))]
    (spit out-file secure-key)
    (log/info (format "\n\nKpow Secure Key:\n----------------\n\n%s\n\nKey file written to: %s\n\n%s"
                      secure-key
                      out-file
                      (if salt
                        "This key can be regenerated with the same passphrase and salt."
                        "Random salt used, this key cannot be regenerated.")))))

(def cli-options
  [["-p" "--pass-file PASSPHRASE-FILE" "(required) File containing key passphrase"]
   ["-s" "--salt SALT" "(optional) Salt to use with key generation, random if none provided"]
   ["-o" "--out-file OUT-FILE" "(optional) File for key output, default: [PASSPHRASE-FILE].key"]
   ["-h" "--help"]])

(defn -main [& args]
  (let [{:keys [options summary errors]} (cli/parse-opts args cli-options)
        {:keys [pass-file out-file salt help]} options]
    (try
      (cond
        errors (log/error (str "\n\n" errors))
        (or help (not pass-file)) (log/info (str "\n\n" summary))
        (not pass-file) (log/info "\n\nRequired: --passfile PASSPHRASE-FILE  File containing key passphrase")
        :else (generate-key pass-file salt (or out-file (str pass-file ".key"))))
      (catch Exception ex
        (log/errorf ex "\nFailed to generate key")))))