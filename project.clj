(defproject io.factorhouse/kpow-secure "1.0.3"

  :description "Secure Key Generation and Payload Encryption"

  :url "https://github.com/factorhouse/kpow-secure"

  :license {:name "MIT License"
            :url  "https://github.com/factorhouse/kpow-secure/blob/main/LICENSE"}


  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.cli "1.0.214"]
                 [org.clojure/tools.logging "1.2.4"]
                 [ch.qos.logback/logback-classic "1.2.11"]]

  :profiles {:dev     {:plugins        [[lein-cljfmt "0.8.0"]]
                       :dependencies   [[clj-kondo "2022.11.02"]]
                       :resource-paths ["dev-resources"]}
             :uberjar {:aot :all}}

  :aliases {"smoke" ["do"
                     ["clean"]
                     ["check"]
                     ["cljfmt" "check"]
                     ["run" "-m" "clj-kondo.main" "--lint" "src:test" "--parallel"]
                     ["test"]]}

  :source-paths ["src"]

  :test-paths ["test"]

  :pedantic? :abort)
