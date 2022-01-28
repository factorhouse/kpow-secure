(ns kpow.secure.key
  (:require [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log])
  (:import (javax.crypto SecretKey SecretKeyFactory)
           (javax.crypto.spec SecretKeySpec PBEKeySpec)
           (java.security SecureRandom)
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
  (SecretKeySpec. (.decode (Base64/getDecoder) key-text) key-enc-algorithm))

(defn secret-key
  "Generate a new AES SecretKeySpec from PBKDF2 passphrase, salt, and rounds"
  ([passphrase]
   (let [salt (byte-array 64)]
     (.nextBytes (SecureRandom.) salt)
     (secret-key passphrase salt default-rounds)))
  ([passphrase salt]
   (secret-key passphrase salt default-rounds))
  ([^String passphrase ^"[B" salt ^Integer rounds]
   (-> (SecretKeyFactory/getInstance key-gen-algorithm)
       (.generateSecret (PBEKeySpec. (char-array passphrase) salt (or rounds default-rounds) (int key-strength)))
       (.getEncoded)
       (SecretKeySpec. key-enc-algorithm))))

(def cli-options
  ;; An option with a required argument
  [["-g" "--generate" "Generate a new secure key"]
   ["-p" "--passfile PASSPHRASE-FILE" "File containing key passphrase"]
   ["-h" "--help"]])

(defn -main [& args]
  (let [{:keys [options summary errors]} (cli/parse-opts args cli-options)
        {:keys [generate passfile help]} options]
    (cond
      errors (log/info (str "\n\n" errors))
      (or help (not generate)) (log/info (str "\n\n" summary))
      (and generate (not passfile)) (log/info "\n\n  required: --passfile PASSPHRASE-FILE  File containing key passphrase")
      :else (log/info (str "\n\n"
                           "Kpow Secure Key:\n"
                           "----------------\n\n"
                           (export-key (secret-key (slurp passfile)))
                           "\n\n"
                           "Save this key, it cannot be retrieved later.")))))