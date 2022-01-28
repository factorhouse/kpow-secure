(defproject io.operatr/kpow-secure "1.0.0"

  :description "Secure Key Generation and Payload Encryption"

  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.clojure/tools.cli "1.0.206"]
                 [ch.qos.logback/logback-classic "1.2.10"]]

  :aliases {"kaocha"       ["with-profile" "+kaocha,+smoke" "run" "-m" "kaocha.runner"]
            "check"        ["with-profile" "+smoke" "check"]
            "fmt"          ["with-profile" "+smoke" "cljfmt" "check"]
            "fmtfix"       ["with-profile" "+smoke" "cljfmt" "fix"]
            "kondo"        ["with-profile" "+smoke" "run" "-m" "clj-kondo.main" "--lint" "src:test" "--parallel"]}

  :source-paths ["src"]
  :test-paths ["test"]

  :javac-options ["-target" "8" "-source" "8" "-Xlint:-options"]

  :pedantic? :abort)
