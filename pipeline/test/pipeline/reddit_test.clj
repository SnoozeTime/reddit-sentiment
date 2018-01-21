(ns pipeline.reddit-test
  (:require [clojure.test :refer :all]
            [pipeline.reddit :refer :all]
            [clojure.java.io :as io]
            [clojure.data.json :as json]))

;; ------------------------------------------------------
;; Will test that we can correctly extract comments from
;; a thread comments json file
;; -------------------------------------------------------

(def comments-json (json/read-str (slurp (io/resource "comments.json")) :key-fn keyword))

(deftest extract-comments-test
  (testing "Extract comments from a json files from reddit"
    (let [comments (extract-comments-from-thread comments-json)
          expected-keys [:body :parent_id :id]]
      
      ;; Sample file contains 3 comments
      (is (= 3 (count comments)))

      ;; Top-level comments
      (let [top-level-comments (filter #(contains? % :top-level) comments)]
        ;; 2 top level comment
        (is (= 2 (count top-level-comments))))

      (doseq [comment comments]
        ;; contains necessary keys
        (is (= true (every? identity (map #(contains? comment %) expected-keys))))))))
