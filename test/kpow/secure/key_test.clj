(ns kpow.secure.key-test
  (:require [clojure.test :refer [deftest is]]
            [kpow.secure.key :as key]))

(deftest key-gen-and-export-import

  ;; the same inputs generate the same key
  (is (= (key/secret-key "aquickredfox" "some-salt")
         (key/secret-key "aquickredfox" "some-salt")))

  ;; key exported for visibility
  (is (= "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88="
         (key/export-key
          (key/secret-key "aquickredfox" "some-salt"))))

  ;; the default random salt determines that a different  key is generated each time for simple passphrase input
  (is (not= (key/secret-key "aquickredfox")
            (key/secret-key "aquickredfox")))

  ;; an imported key is equivalent to one generated from inputs
  (is (= (key/import-key "//iQh9KYe7pM+mevjifZPrm7YE2+rRloG1E15zzjR88=")
         (key/secret-key "aquickredfox" "some-salt"))))