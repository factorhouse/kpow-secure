(ns kpow.secure.key
  (:require [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log])
  (:import (javax.crypto SecretKey SecretKeyFactory)
           (javax.crypto.spec SecretKeySpec PBEKeySpec)
           (java.security SecureRandom)
           (java.util Base64)
           (java.nio.charset StandardCharsets))
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
  (SecretKeySpec. (.decode (Base64/getDecoder) key-text) key-enc-algorithm))

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

(def cli-options
  ;; An option with a required argument
  [["-g" "--generate" "Generate a new secure key"]
   ["-p" "--passfile PASSPHRASE-FILE" "(required) File containing key passphrase"]
   ["-s" "--salt SALT" "(optional) Salt to use with key generation, random if none provided"]
   ["-h" "--help"]])

(defn -main [& args]
  (let [{:keys [options summary errors]} (cli/parse-opts args cli-options)
        {:keys [generate passfile salt help]} options]
    (cond
      errors (log/info (str "\n\n" errors))
      (or help (not generate)) (log/info (str "\n\n" summary))
      (and generate (not passfile)) (log/info "\n\n  required: --passfile PASSPHRASE-FILE  File containing key passphrase")
      :else (log/info (str "\n\n"
                           "Kpow Secure Key:\n"
                           "----------------\n\n"
                           (export-key (secret-key (slurp passfile) salt))
                           "\n\n"
                           (if salt
                             "This key can be regenerated with the same passphrase and salt."
                             "Random salt used, this key cannot be regenerated."))))))