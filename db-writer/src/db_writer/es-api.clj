(ns db-writer.es-api
     (:require [clj-http.client :as client]
               [clojure.data.json :as json]))

(def default-conn {:url "http://127.0.0.1:9200"})


(defn cluster-health [conn]
  (let [health-url (str (:url conn) "/_cat/health?v")]
    (client/get health-url )))

(defn cluster-nodes [conn]
  (let [cluster-nodes-url (str (:url conn) "/_cat/nodes?v")]
    (client/get cluster-nodes-url)))

(defn get-indices [conn]
  (let [indices-url (str (:url conn) "/_cat/indices?v")]
    (client/get indices-url)))

(defn create-index [conn index-name]
  (let [create-index-url (str (:url conn) "/" index-name "?pretty")]
    (println create-index-url)
    (client/put create-index-url)))

(defn create-document
  "Create document (clojure map) explicitely specifying an ID key in the map"
  [conn index-name document id-key]
  (let [document-id (get document id-key)
        json-file (json/write-str document)
        create-document-url (str (:url conn) "/" index-name "/doc/" document-id)]
    (client/put create-document-url {:body json-file})))


;; DO NOT WORK YET
(defn create-bulk-document
  "Create documents in bulk. Documents is an array of clojure map. Id key of the object
  should be given"
  [conn index-name documents id-key]
  (let [body-map (reduce (fn [acc document]
                           (conj acc {:index {:_id (get document id-key)}} document))
                         []
                         documents)
        body (reduce (fn [body body-map-element] (str body (json/write-str body-map-element) "\n"))
                     ""
                     body-map)
        bulk-create-url (str (:url conn) "/" index-name "doc/_bulk?pretty")]
    (println body)
    (client/post bulk-create-url {:content-type :json :body body})))

;; ====================================================================================
; Search API - IN V2
