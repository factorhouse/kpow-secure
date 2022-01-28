(ns kpow.secure.key-test
  (:require [clojure.test :refer :all]
            [kpow.secure.key :as key])
  (:import (java.nio.charset StandardCharsets)))

(deftest key-gen-and-export-import

  ;; the same inputs generate the same key
  (is (= (key/secret-key "aquickredfox" (.getBytes "some-salt" (StandardCharsets/UTF_8)))
         (key/secret-key "aquickredfox" (.getBytes "some-salt" (StandardCharsets/UTF_8)))))

  ;; key exported for visibility
  (is (= "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88="
         (key/export-key
          (key/secret-key "aquickredfox" (.getBytes "some-salt" (StandardCharsets/UTF_8))))))

  ;; the default random salt determines that a different master key is generated each time for simple passphrase input
  (is (not= (key/secret-key "aquickredfox")
            (key/secret-key "aquickredfox")))

  ;; an imported key is equivalent to one generated from inputs
  (is (= (key/import-key "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88=")
         (key/secret-key "aquickredfox" (.getBytes "some-salt" (StandardCharsets/UTF_8))))))