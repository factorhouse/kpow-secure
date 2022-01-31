(ns kpow.secure-test
  (:require [clojure.test :refer [deftest is]]
            [kpow.secure :as secure]
            [kpow.secure.key :as key]))

(def sample-input (str "SSL_KEYSTORE_PASSWORD=keypass1234\n"
                       "SSL_TRUSTSTORE_PASSWORD=trustpass1234"))

(deftest full-trip-and-variants

  (let [secret-key (key/secret-key "aquickredfox" "some-salt")]

    (is (= sample-input
           (->> (secure/encoded-payload secret-key sample-input)
                (secure/decoded-payload secret-key))))

    (is (= sample-input
           (->> (secure/encoded-payload secret-key sample-input)
                (secure/decoded-payload (key/import-key "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88=")))))

    (is (= sample-input
           (->> (secure/encoded-payload (key/import-key "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88=") sample-input)
                (secure/decoded-payload secret-key))))

    (is (= sample-input
           (->> (secure/encoded-payload (key/import-key "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88=") sample-input)
                (secure/decoded-payload (key/import-key "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88=")))))

    ;; the random IV guarantees (well almost) different payload even with the same key when encoded for a second time
    (is (not= (secure/encoded-payload secret-key sample-input)
              (secure/encoded-payload secret-key sample-input)))))

(deftest interpretation

  (let [secret-key (key/secret-key "aquickredfox" "some-salt")]

    (is (= {"SSL_KEYSTORE_PASSWORD"   "keypass1234"
            "SSL_TRUSTSTORE_PASSWORD" "trustpass1234"}
           (->> (secure/encoded-payload secret-key sample-input)
                (secure/decoded-payload secret-key)
                (secure/->map))))

    (is (= {"SASL_JAAS_CONFIG"        "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"kpow\" password=\"kpow-secret\";"
            "SASL_MECHANISM"          "PLAIN"
            "SECURITY_PROTOCOL"       "SASL_PLAINTEXT"
            "SSL_TRUSTSTORE_LOCATION" "/ssl/truststore.jks"
            "SSL_TRUSTSTORE_PASSWORD" "password1234"}
           (-> (secure/decrypt-file "dev-resources/secure/passphrase.key" "dev-resources/secure/config.env.aes")
               (secure/->map))))

    (is (= {"sasl.jaas.config"        "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"kpow\" password=\"kpow-secret\";"
            "sasl.mechanism"          "PLAIN"
            "security.protocol"       "SASL_PLAINTEXT"
            "ssl.truststore.location" "/ssl/truststore.jks"
            "ssl.truststore.password" "1234"}
           (-> (secure/decrypt-file "dev-resources/secure/passphrase.key" "dev-resources/secure/props.env.aes")
               (secure/->map))))))