(defproject io.factorhouse/shroud "1.0.4"

  :description "Secure Key Generation and Payload Encryption"

  :url "https://github.com/factorhouse/shroud"

  :license {:name "MIT License"
            :url  "https://github.com/factorhouse/shroud/blob/main/LICENSE"}


  :dependencies [[org.clojure/clojure "1.12.0"]
                 [org.clojure/tools.cli "1.1.230"]
                 [org.clojure/tools.logging "1.3.0"]
                 [ch.qos.logback/logback-classic "1.5.16"]]

  :profiles {:dev     {:plugins        [[dev.weavejester/lein-cljfmt "0.13.0"]]
                       :dependencies   [[clj-kondo "2025.01.16"]]
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
