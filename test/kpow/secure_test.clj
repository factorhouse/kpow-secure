(ns kpow.secure-test
  (:require [clojure.test :refer [deftest is]]
            [kpow.secure :as secure]
            [kpow.secure.key :as key])
  (:import (java.nio.charset StandardCharsets)))

(def sample-input (str "SSL_KEYSTORE_PASSWORD=keypass1234\n"
                       "SSL_TRUSTSTORE_PASSWORD=trustpass1234"))

(deftest full-trip-and-variants

  (let [secret-key (key/secret-key "aquickredfox" (.getBytes "some-salt" (StandardCharsets/UTF_8)))]

    (is (= sample-input
           (->> (secure/encoded-payload secret-key sample-input)
                (secure/decoded-message secret-key))))

    (is (= sample-input
           (->> (secure/encoded-payload secret-key sample-input)
                (secure/decoded-message (key/import-key "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88=")))))

    (is (= sample-input
           (->> (secure/encoded-payload (key/import-key "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88=") sample-input)
                (secure/decoded-message secret-key))))

    (is (= sample-input
           (->> (secure/encoded-payload (key/import-key "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88=") sample-input)
                (secure/decoded-message (key/import-key "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88=")))))

    ;; the random IV guarantees (well almost) different payload even with the same key when encoded for a second time
    (is (not= (secure/encoded-payload secret-key sample-input)
              (secure/encoded-payload secret-key sample-input)))))