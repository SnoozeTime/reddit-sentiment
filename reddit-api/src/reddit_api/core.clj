(ns reddit-api.core
  (:gen-class)
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.walk])
  (:use [slingshot.slingshot :only [try+ throw+]]))

;; ==============================================================================
;; Some utilities to avoid repeting meself.
;; ==============================================================================

;; TODO
(defn handle-http-error
  "Handle the HTTP error returned by clj-http"
  [error]
  (println error)
  ;; Return empty list  so that we do continue the process
  [])

(defn do-get
  "Will get the given url and manage potential HTTP errors.
  Callbacks should get either the http response or the error. "
  [url url-params success-cb error-cb]
  (try+
    (let [resp (client/get url url-params)]
      (success-cb resp))
    (catch [:status 403] e
      (error-cb e))
    (catch [:status 404] e
      (error-cb e))
    (catch [:status 429] e
      (error-cb e))
    (catch Object _
      ;; Unexpected error, do not catch
      (throw+))))

(defn do-get-retry
  "Try to get a page and will retry a number of time before giving up."
  [url url-params nb-retry success-cb error-cb]
  (println "Remaining tentatives " nb-retry)
  (do-get url
          url-params
          success-cb
          (fn [e]
            ;; Retry if retry-time is not 0
            (if (> nb-retry 0)
              (do
                (println "got exception, will retry")
                (recur (do-get-retry url url-params (dec nb-retry) success-cb error-cb)))
              (error-cb e)))))

(defn get-body-from-http-resp
  "Get the body from response and convert it from JSON string to map"
  [resp]
  (-> resp
      :body
      (json/read-str :key-fn keyword)))

;; ===============================================================================
;; Reddit API wrappers 
;; ===============================================================================

(def reddit-url "https://reddit.com")
(def http-retry-nb 5)

(def test-thread-url "https://www.reddit.com/r/FunfairTech/comments/7pwons/buying_funfair")

(defn build-subreddit-api-call
  "api-call is for example /show.json or /top.json"
  [subreddit api-call]
  (str reddit-url "/r/" subreddit api-call))

(defn extract-reddit-children
  [parent]
  (get-in parent [:data :children]))

(defn extract-reddit-data-element
  [reddit-obj element]
  (get-in reddit-obj [:data element]))

;; --------------------------------------------------------
;; manipulate json from r/<subreddit>/
;; --------------------------------------------------------

(defn extract-url-from-thread
  [thread]
  (get-in thread [:data :url]))

;; --------------------------------------------------------
;; Manipulate JSON from /r/subreddit/url/id/show.json
;; a thread comments
;; --------------------------------------------------------

;; show.json will send an array of two elements
;; First element represent the listing
;; Second element represent the comments
;; The comment structure is one top element with children
;; children are the top-level comments
;; then, top-level comments following the same structure 
;; with potentially more children

;; We want to flatten these maps to just a vector of comments
;; keys to keep would be body, parent-id, subreddit?, thread-id


(defn flatten-comments
  [comments-json]
  (let [flattened (atom []) ;; unfortunatly I need atoms here :) is there a solution without this ?
        to-keep? (fn [node]
                   (and (map? node)
                        (every? identity (map #(contains? node %) [:body]))))]
    ;;post walk will walk over ever node of a collection and apply a function
    ;; Here, I will just add something to flattened
    (clojure.walk/postwalk (fn [node]
                             (when (to-keep? node)
                               (swap! flattened conj node))
                             node)
                           comments-json)
    @flattened))

(def comment-keys [:body :parent_id :id])

(defn select-comment-keys [flattened-comments]
  (map #(select-keys % comment-keys) flattened-comments))

(defn tag-top-level-comments
  [flattened-comments thread-id]
  (vec (map (fn [{:keys [parent_id] :as  comment}]
              (if (= thread-id parent_id)
                (assoc comment :top-level true)
                comment))
            flattened-comments)))


;; -------------------
(defn get-main-comment-from-thread
  [thread]
  (let [main-comment-array (extract-reddit-children (get thread 0))]
    (get main-comment-array 0)))

;; --------------------------------------------------------
;; API CALLS
;; ------------------------------------------------------
(defn get-last-subreddit-threads
  "Number is limited to 100 by reddit API"
  [subreddit number]
  (do-get-retry (build-subreddit-api-call subreddit "/top.json")
                {:query-params {:limit number}}
                http-retry-nb
                (fn [resp]
                  (-> resp
                      get-body-from-http-resp
                      ;; Threads are stored as listing children
                      extract-reddit-children))
                handle-http-error))

(defn get-thread-by-url
  "Get a complete JSON document of the thread (all comments included) from url"
  [url]
  (do-get-retry (str test-thread-url "show.json")
                {}
                http-retry-nb
                get-body-from-http-resp
                handle-http-error))

(defn extract-comments-from-thread
  [thread]
  ;; Comments are second child of the thread
  (let [main-comment (get-main-comment-from-thread thread)
        main-comment-name (extract-reddit-data-element main-comment :name)
        comments (get thread 1)]
    (-> comments
        flatten-comments
        select-comment-keys
        (tag-top-level-comments main-comment-name))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
