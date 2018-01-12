(defproject sentiment-analyzer "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [edu.stanford.nlp/stanford-corenlp "3.8.0"]
                 [edu.stanford.nlp/stanford-corenlp "3.8.0" 
                  :classifier "models"]]
  :main ^:skip-aot sentiment-analyzer.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
