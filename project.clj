(defproject io.operatr/kpow-secure "1.0.0"

  :description "Secure Key Generation and Payload Encryption"

  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/tools.cli "1.0.206"]
                 [org.clojure/tools.logging "1.2.4"]
                 [ch.qos.logback/logback-classic "1.2.10"]]

  :aliases {"fmt"   ["cljfmt" "check"]
            "kondo" ["run" "-m" "clj-kondo.main" "--lint" "src:test" "--parallel"]}

  :profiles {:dev {:plugins      [[lein-cljfmt "0.8.0"]]
                   :dependencies [[clj-kondo "2022.01.15"]]}}

  :source-paths ["src"]
  :test-paths ["test"]

  :javac-options ["-target" "8" "-source" "8" "-Xlint:-options"]

  :pedantic? :abort)
