(ns io.factorhouse.shroud.secure-test
  (:require [clojure.test :refer [deftest is]]
            [io.factorhouse.shroud.secure :as secure]
            [io.factorhouse.shroud.secure.key :as key]))

(def sample-input (str "SSL_KEYSTORE_PASSWORD=keypass1234\n"
                       "SSL_TRUSTSTORE_PASSWORD=trustpass1234"))

(deftest prefixed?

  (is (secure/prefixed? "AES:abc"))
  (is (not (secure/prefixed? "aes:abc")))
  (is (not (secure/prefixed? "aesabc")))
  (is (not (secure/prefixed? "abc")))
  (is (not (secure/prefixed? "")))
  (is (not (secure/prefixed? nil))))

(deftest error-cases

  (is (thrown? IllegalArgumentException
               (secure/encrypted nil "some-text")))

  (is (thrown? IllegalArgumentException
               (secure/encrypted "" "some-text")))

  (is (thrown? IllegalArgumentException
               (secure/decrypted nil "ARDuFSOqVc5l8dPe2l8jLnRvf2Y2/ZnhWNtkuZuoP1Updxo4cFAsFr+eM4WVcH/yIogK3ypO4sLp7sSXjkXv3L5Ci/5poJG2U/+No5ySBR1BhDjcV3mkO3TBYp4nQu65mpA=")))

  (is (thrown? IllegalArgumentException
               (secure/decrypted "" "ARDuFSOqVc5l8dPe2l8jLnRvf2Y2/ZnhWNtkuZuoP1Updxo4cFAsFr+eM4WVcH/yIogK3ypO4sLp7sSXjkXv3L5Ci/5poJG2U/+No5ySBR1BhDjcV3mkO3TBYp4nQu65mpA=")))

  (is (thrown? IllegalArgumentException
               (secure/decrypted "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88=" nil)))

  (is (thrown? IllegalArgumentException
               (secure/decrypted "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88=" "")))

  (is (thrown? IllegalArgumentException
               (secure/encrypted "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88=" nil)))

  ;; you can encrypt a blank string (just not nil, above)
  (is (= ""
         (->> (secure/encrypted "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88=" "")
              (secure/decrypted "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88=")))))

(deftest full-trip-and-variants

  (let [secret-key (key/secret-key "aquickredfox" "some-salt")]

    (is (= sample-input
           (->> (secure/encrypted "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88=" sample-input)
                (secure/decrypted "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88="))))

    (is (= sample-input
           (secure/decrypted
            "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88="
            "ARDuFSOqVc5l8dPe2l8jLnRvf2Y2/ZnhWNtkuZuoP1Updxo4cFAsFr+eM4WVcH/yIogK3ypO4sLp7sSXjkXv3L5Ci/5poJG2U/+No5ySBR1BhDjcV3mkO3TBYp4nQu65mpA=")))

    (is (= sample-input
           (->> (secure/encoded-payload secret-key sample-input)
                (secure/decoded-text secret-key))))

    (is (= sample-input
           (->> (secure/encoded-payload secret-key sample-input)
                (secure/decoded-text (key/import-key "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88=")))))

    (is (= sample-input
           (->> (secure/encoded-payload (key/import-key "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88=") sample-input)
                (secure/decoded-text secret-key))))

    (is (= sample-input
           (->> (secure/encoded-payload (key/import-key "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88=") sample-input)
                (secure/decoded-text (key/import-key "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88=")))))

    ;; the random IV guarantees (well almost) different payload even with the same key when encoded for a second time
    (is (not= (secure/encoded-payload secret-key sample-input)
              (secure/encoded-payload secret-key sample-input)))))

(deftest interpretation

  (let [secret-key (key/secret-key "aquickredfox" "some-salt")]

    (is (= {"SSL_KEYSTORE_PASSWORD"   "keypass1234"
            "SSL_TRUSTSTORE_PASSWORD" "trustpass1234"}
           (->> (secure/encoded-payload secret-key sample-input)
                (secure/decoded-text secret-key)
                (secure/->map))))

    (is (= {"SASL_JAAS_CONFIG"        "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"kpow\" password=\"kpow-secret\";"
            "SASL_MECHANISM"          "PLAIN"
            "SECURITY_PROTOCOL"       "SASL_PLAINTEXT"
            "SSL_TRUSTSTORE_LOCATION" "/ssl/truststore.jks"
            "SSL_TRUSTSTORE_PASSWORD" "password1234"}
           (-> (secure/decrypted (slurp "dev-resources/secure/passphrase.key") (slurp "dev-resources/secure/config.env.aes"))
               (secure/->map))))

    (is (= {"sasl.jaas.config"        "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"kpow\" password=\"kpow-secret\";"
            "sasl.mechanism"          "PLAIN"
            "security.protocol"       "SASL_PLAINTEXT"
            "ssl.truststore.location" "/ssl/truststore.jks"
            "ssl.truststore.password" "1234"}
           (-> (secure/decrypted (slurp "dev-resources/secure/passphrase.key") (slurp "dev-resources/secure/props.env.aes"))
               (secure/->map))))))

(deftest special-characters

  (let [secret-key (key/secret-key "aquickredfox" "some-salt")]

    ;; OWASP special characters list: https://owasp.org/www-community/password-special-characters
    (is (= " !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"
           (->> (secure/encrypted "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88=" " !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~")
                (secure/decrypted "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88="))))

    (is (= " !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"
           (secure/decoded-text secret-key "ARBU3lQltrp0+i4IHmIsYhA1X/4kbXxlHllPBlmzdYcNeJtEzv9SseIMbwJ+wuxldZWrjo1qNFRg71ysTX7YZBvU")))

    ;; Mixed characters pass generated by KeepassXC
    (is (= "w;7MQ{&{o[F(H&^Fv[aN^y{7J@z+K7#"
           (->> (secure/encrypted "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88=" "w;7MQ{&{o[F(H&^Fv[aN^y{7J@z+K7#")
                (secure/decrypted "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88="))))))
