(defproject io.operatr/kpow-secure "1.0.0"

  :description "Secure Key Generation and Payload Encryption"

  :source-paths ["src"]
  :test-paths ["test"]

  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/tools.cli "1.0.206"]
                 [org.clojure/tools.logging "1.2.4"]
                 [ch.qos.logback/logback-classic "1.2.10"]]

  :profiles {:dev     {:plugins        [[lein-cljfmt "0.8.0"]]
                       :dependencies   [[clj-kondo "2022.01.15"]]
                       :resource-paths ["dev-resources"]}
             :uberjar {:aot :all}}

  :aliases {"smoke" ["do"
                     ["clean"]
                     ["check"]
                     ["test"]
                     ["cljfmt" "check"]
                     ["run" "-m" "clj-kondo.main" "--lint" "src:test" "--parallel"]]}

  :pedantic? :abort)
