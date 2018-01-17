(ns pipeline.core
  (:gen-class)
  (:require [clojure.core.async
                  :as a
                  :refer [>! <! >!! <!! go chan buffer close! thread
                          alts! alts!! timeout]]
            [pipeline.reddit :as reddit]
            [pipeline.es-api :as es-api]))

;; Parameters for the application in seconds 
(def fetch-sleeping-time 1800) ;; 30 minutes

;; ===================================================================================
;; This block will fetch the data from new listings in a subreddit.
;; It will get the listings and their permalink and send the links to the next block

;; We get all the latest threads all the time so that if there are new comments, we will
;; them. When threads are past the latest 25 threads, we don't fetch data from them anymore
;; 
;; Maybe in v2 we could follow all threads from a period of time (last week -> now)
;; ===================================================================================

(defn latest-listing-fetcher
  "What subreddit to follow. How many seconds between each fetch"
  [subreddit sleeping-time]
  (let [out (chan)]
    (go
      (while true
        (let [;; By default, will get the last 25 threads
              listings (reddit/get-last-subreddit-threads subreddit {})]
          
          ;; If we have something, we need to send them to out and then we can update fullname
          (doseq [listing listings]
            (>! out (reddit/extract-url-from-listing listing)))

          ;; Then wait a bit and do it again
          (println "Will sleep for " sleeping-time " seconds.")
          (Thread/sleep (* sleeping-time 1000)))))
    out))

;; ======================================================================================
;; This block will extract all the comments from a thread.
;; Because reddit comment structure is hierarchical, it will flatten this hierarchy and
;; remove useless metadata so that we only keep the relevant info
;; ======================================================================================

(defn listing-comment-fetcher
  "input is a channel which sends URL to a reddit threads.
  E.g. https://reddit.com/r/CryptoCurrency/comments/7qu8t9/18002738255_us_national_suicide_hotline/"
  [in]
  (let [out (chan)]
    ;; Start a new go block in which we are going to listen to incoming links
    (go
      (while true
        (let [url (<! in)
              thread (reddit/get-thread-by-url url)
              comments (reddit/extract-comments-from-thread thread)]
          ;; Send the comments to the next pipeline block
          (doseq [comment comments]
            (>! out comment)))))
    out))

;; ==========================================================================================
;; This last block will input the comments in elasticsearch
;; ==========================================================================================

(defn es-db-writer
  "Will write comment in the database"
  [in conn]
  (go
    (while true
      (let [comment (<! in)
            index-name (str "comments")]
        (es-api/create-document conn index-name comment :id)))))




(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [conn (es-api/initialize-conn)
        ;; Build the pipeline 
        listing-fetcher-out (latest-listing-fetcher "CryptoCurrency" fetch-sleeping-time)
        comment-fetcher-out (listing-comment-fetcher listing-fetcher-out)]
    (es-db-writer comment-fetcher-out conn)

    (while true
      (Thread/sleep 10000))))
