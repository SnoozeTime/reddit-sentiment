(defproject db-writer "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.zeromq/jeromq "0.3.5"]]
  :plugins [[lein-exec "0.3.7"]]
  :main ^:skip-aot db-writer.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
