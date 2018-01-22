(ns elasticsearch-api.core
  (:gen-class)
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log])
  (:use [slingshot.slingshot :only [try+ throw+]]
        [clojure.string :only [ends-with? join]]))

(def headers {:Content-Type "application/json; charset=UTF-8"})

;; =======================================================
;; Some validation
;; =======================================================

(defn validate-connection
  "Validate the connection. Throws an exception is there is an issue"
  [conn]
  (when (nil? conn)
    (throw+ {:error-type "connection is nil"}))

  (when (nil? (:url conn))
    (throw+ {:error-type "Connection's URL is nil" :data conn})))

(defn join-url-parts
  "url might or might not end with a slash."
  [url & parts]

  (if (ends-with? url "/")
    (str url (join "/" parts))
    (join "/" (concat [url] parts))))


;; ========================================================
;; CREATE STUFF
;; ========================================================

(defn create-index-url
  "Create the url to create an index"
  [conn index-name]
  
  ;; We get the connection from the exterior so it might be nil
  (validate-connection conn)

  (join-url-parts (:url conn) index-name))


(defn create-index
  "Create index from a connection (map with :url) and the index name. Will throw an exception if something goes banana"
  [conn index-name]
  (let [url (create-index-url conn index-name)]
    ;; This will throw an error 400 if the index exists. This is a library so we will forward this error
    (client/put url)))

(defn create-document
  "Will create a document from a clojure map. There should be an ID in the map."
  [conn index-name document id-key]

  (validate-connection conn)
  (let [document-id (get document id-key)
        json-file (json/write-str document)
        create-document-url (join-url-parts (:url conn) index-name "doc" document-id)]
    (client/put create-document-url {:body json-file :headers headers})))

(defn create-bulk-document
  "Create documents in bulk. Documents is an array of clojure map. Id key of the object
  should be given"
  [conn index-name documents id-key]
  (validate-connection conn)
  (let [body-map (reduce (fn [acc document]
                           (conj acc {:index {:_id (get document id-key)}} document))
                         []
                         documents)
        body (reduce (fn [body body-map-element] (str body (json/write-str body-map-element) "\n"))
                     ""
                     body-map)
        bulk-create-url (join-url-parts (:url conn) index-name "doc" "_bulk")]
    (client/post bulk-create-url {:body body :headers headers})))


;; ==========================================================
;; SEARCH STUFF
;; ==========================================================

(defn search
  "Do an elasticsearch search query. Search query is a clojure map that will be the body of the search HTTP query (see https://www.elastic.co/guide/en/elasticsearch/reference/current/_the_search_api.html)"
  [conn index-name search-query]
  (validate-connection conn)
  (let [query-str (json/write-str search-query)
        http-response (client/get (join-url-parts (:url conn) index-name "_search")
                                  {:body query-str :headers headers})]
    (-> http-response
        :body
        (json/read-str :key-fn keyword))))

