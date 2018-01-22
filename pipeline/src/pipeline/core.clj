(ns pipeline.core
  (:gen-class)
  (:require [clojure.core.async
                  :as a
                  :refer [>! <! >!! <!! go chan buffer close! thread
                          alts! alts!! timeout go-loop]]
            [pipeline.reddit :as reddit]
            [elasticsearch-api.core :as es-api]
            [clojure.tools.logging :as log]
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:use [slingshot.slingshot :only [throw+ try+]]))

(def default-comment-fetcher-nb 1)

;; -> This is a copy paste of clojure code with a small modification
(defmacro time-with-label
  "Evaluates expr and prints the time it took.  Returns the value of
 expr."
  [label expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (prn (str ~label " - Elapsed time: " (/ (double (- (. System (nanoTime)) start#)) 1000000.0) " msecs"))
ret#))

(defn load-properties []
  (let [path (System/getenv "REDDIT_PROJECT_PROPS_PATH")]
    (try+
     (edn/read-string (slurp path))
     (catch IllegalArgumentException _
       (log/info "Cannot find property file at " path ".\n Please set the environment variable REDDIT_PROJECT_PROPS_PATH")
       (throw+)))))

;; ===================================================================================
;; This block will fetch the data from new listings in a subreddit.
;; It will get the listings and their permalink and send the links to the next block

;; We get all the latest threads all the time so that if there are new comments, we will
;; them. When threads are past the latest 25 threads, we don't fetch data from them anymore
;; 
;; Maybe in v2 we could follow all threads from a period of time (last week -> now)
;; ===================================================================================

(defn latest-listing-fetcher
  "What subreddit to follow (from channel). How many seconds between each fetch"
  [subreddit-chan]
  (log/info "Starts latest-listing-fetcher")
  (let [out (chan)]
    (go-loop [subreddit (<! subreddit-chan)]
      (if (= :close subreddit)
        (log/info "Close latest-listing-fetcher")
        (do
          (let [;; By default, will get the last 25 threads
                listings (reddit/get-last-subreddit-threads subreddit {})]
            
            ;; If we have something, we need to send them to out and then we can update fullname
            (doseq [listing listings]
              (let [url (reddit/extract-url-from-listing listing)]
                (log/info "Will download comments from " url)
                (>! out url)))

            ;; Wait for next subreddit request.
            (recur (<! subreddit-chan))))))
    out))

;; ======================================================================================
;; This block will extract all the comments from a thread.
;; Because reddit comment structure is hierarchical, it will flatten this hierarchy and
;; remove useless metadata so that we only keep the relevant info
;; ======================================================================================

(defn listing-comment-fetcher
  "input is a channel which sends URL to a reddit threads.
  E.g. https://reddit.com/r/CryptoCurrency/comments/7qu8t9/18002738255_us_national_suicide_hotline/"
  [in nb-fetcher]
  (let [out (chan)]

    (dotimes [_ nb-fetcher]
      ;; Start a new go block in which we are going to listen to incoming links
      (go-loop [url (<! in)]
        (when (some? url)

          ;; HTTP request can fail, but we shouldn't stop the worker
          (try+
           (let [thread (time-with-label "Get comments from thread"
                                         (reddit/get-thread-by-url url))
                 comments (time-with-label "Extract comments"
                                           (reddit/extract-comments-from-thread thread))]
             ;; Send the comments to the next pipeline block
             (log/info "After HTTP req for " url)
             (>! out comments))
           (catch Object e
             (log/error "Unexpected error in listing-comment-fetcher " e)))
          
          ;; Get next comment to process
          (recur (<! in)))))
    out))

;; ==========================================================================================
;; This last block will input the comments in elasticsearch
;; ==========================================================================================

(defn es-db-writer
  "Will write comment in the database"
  [in conn]
  (go
    (while true
      (let [comments (<! in)
            index-name (str "comments")]
        (try+
         (time-with-label "Insert into elasticsearch"
                          (es-api/create-bulk-document conn index-name comments :id))
         (catch Object e
           (log/error "Unexpected error in es-db-writer " e)))))))



(defn create-index-if-not-exist [conn index-name]
  (try+
   (es-api/create-index conn index-name)
   (catch [:status 400] _
     (log/info "Index " index-name " already exists."))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [properties (load-properties)
        conn (:es properties)]
    (log/info (str "Will start with following properties: " properties))

    (create-index-if-not-exist conn "comments")

    (try+
     (let [subreddit-chan (chan)
           comment-fetcher-nb (get properties :listing-comment-fetcher-number default-comment-fetcher-nb)
           ;; Build the pipeline 
           listing-fetcher-out (latest-listing-fetcher subreddit-chan)
           comment-fetcher-out (listing-comment-fetcher listing-fetcher-out comment-fetcher-nb)]
       (es-db-writer comment-fetcher-out conn)

       (>!! subreddit-chan "CryptoCurrency")
       (Thread/sleep 5000)
       (>!! subreddit-chan :close))
     (catch java.net.ConnectException e
       (log/error "Cannot connect to elasticsearch " e)
       (throw+))
     (catch Object e
       ;; This is unexpected... Log it and throw it to terminate the program. The supervisor will
       ;; handle the rest
       (log/fatal e)
       (throw+)))))
