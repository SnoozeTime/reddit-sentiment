(ns reddit-link-fetcher.core
  (:gen-class)
  (:require [reddit-api.core :as reddit]))

(defn in? 
  "true if coll contains elm"
  [coll elm]  
  (some #(= elm %) coll))

(defn poll-new-listings
  [subreddit]
  (let [latest-listing-url (atom "")]
    (while true
      (let [listings (reddit/get-last-subreddit-threads subreddit 1)
            urls (vec (map #(reddit/extract-url-from-thread %) listings))]
        (if (in? urls @latest-listing-url)
          (do
            ;; Here, we need to get the urls from new to latest.
            (println "found URL")
            )
          (do
            ;; Here, we give everything
            (println "Cannot find url")
            )
          )
        ;; New value for the latest
        (when (not-empty urls)
          (reset! latest-listing-url (get urls 0))
          (println "NEW LATEST URL " (get urls 0))))

      ;; Sleep after working to avoid querying too much
      (Thread/sleep 5000))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
