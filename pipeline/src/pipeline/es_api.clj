(ns pipeline.es-api
     (:require [clj-http.client :as client]
               [clojure.data.json :as json]
               [clojure.tools.logging :as log])
     (:use [slingshot.slingshot :only [try+]]))

(def default-conn {:url "http://127.0.0.1:9200"})

(def headers {:Content-Type "application/json; charset=UTF-8"})

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
    (try+
     (client/put create-index-url)
     (catch [:status 400] _
       (println "Index already exists")))))

(defn create-document
  "Create document (clojure map) explicitely specifying an ID key in the map"
  [conn index-name document id-key]
  (let [document-id (get document id-key)
        json-file (json/write-str document)
        create-document-url (str (:url conn) "/" index-name "/doc/" document-id)]
    (log/info json-file)
    (client/put create-document-url {:body json-file :headers headers})))


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



(defn initialize-conn 
  "Create the index if it does not exist and will return the connection"
  [properties]
  (let [conn (:es properties)]
    (create-index conn "comments")
    conn))


;; ====================================================================================
; Search API - IN V2

(defn create-search-url
  [conn-url index-name]
  (str conn-url "/" index-name "/_search"))

(defn search
  "Do an elasticsearch search query. Search query is a clojure map that will be the body of the search HTTP query (see https://www.elastic.co/guide/en/elasticsearch/reference/current/_the_search_api.html)"
  [conn index-name search-query]
  (let [query-str (json/write-str search-query)
        http-response (client/get (create-search-url (:url conn) index-name)
                                  {:body query-str :headers headers})]
    (-> http-response
        :body
        (json/read-str :key-fn keyword))))

(defn get-keyword-mention
  "Get the number of hits in the database for the given keyword"
  [conn index-name kw]
  (log/info "Will search mentions for: " kw)
  (let [search-query {:query {:match {:body kw}} :size 1}
        body-json (search conn index-name search-query)]
    {:keyword kw
     :hits (get-in body-json [:hits :total])}))


;; Get all the comments that match a keyword
