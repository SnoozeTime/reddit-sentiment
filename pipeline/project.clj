(defproject pipeline "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-http "3.7.0"]
                 [org.clojure/data.json "0.2.6"]
                 [slingshot "0.12.2"]
                 [org.clojure/core.async "0.4.474"]
                 [org.clojure/tools.logging "0.3.1"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                 javax.jms/jms
                                                 com.sun.jmdk/jmxtools
                                                 com.sun.jmx/jmxri]]] 
  :main ^:skip-aot pipeline.core
  :target-path "target/%s"
  
  :profiles {:uberjar {:aot :all}
             ;; run with lein test-refresh
             :dev {:plugins [[com.jakemccrary/lein-test-refresh "0.22.0"]]}
             :test {:plugins [[venantius/ultra "0.5.2"]] ;; Just for some colors :)
                    }})
