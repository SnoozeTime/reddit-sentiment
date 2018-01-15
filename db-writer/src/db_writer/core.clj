(ns db-writer.core
  (:gen-class)
  (:require [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.index :as idx]))

(def index-name "test")

