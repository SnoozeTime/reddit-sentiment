(ns elasticsearch-api.core-test
  (:require [clojure.test :refer :all]
            [elasticsearch-api.core :refer :all])
  (:use [slingshot.slingshot :only [try+]]))

;; This function will throw an exception if the connection is not valid
(deftest validate-connection-test
  
  ;; OK case
  (testing "connection is valid"
    (let [conn {:url "http://localhost"}]
      (validate-connection conn)
      (is true)))

  ;; Connection is nil
  (testing "Connection is nil, throw an exception"
    (let [expected-error "connection is nil"]
      (try+
       (validate-connection nil)
       (is false)
       
       (catch Object {:keys [error-type]}
         (is (= expected-error error-type))))))

  ;; URL is nil
  (testing "Connection's URL is nil, throw an exception"
    (let [expected-error "Connection's URL is nil"
          conn {:other "Hello."}]
      (try+
       (validate-connection conn)
       (is false)

       (catch Object {:keys [error-type]}
         (is (= expected-error error-type)))))))


(deftest create-index-url-test
  
  ;; Normal case
  (testing "Create index URL - normal case"
    (let [expected-url "http://localhost:9200/comments"
          conn {:url "http://localhost:9200"}]
      (is (= expected-url (create-index-url conn "comments")))))
  
  ;; What happened if we add a slash to the url ? Shouldn't fail for something so trivial
  (testing "Create index URL when slash at the end of URL -> should succeed"
    (let [expected-url "http://localhost:9200/comments"
          conn {:url "http://localhost:9200/"}]
      (is (= expected-url (create-index-url conn "comments")))))

  ;; API of the library should throw if we input wrong data
  (testing "When url is nil, throw exception"
    (let [conn {:something-else ""}
          expected-error "Connection's URL is nil"]
      (try+
       (create-index-url conn "comments")
       ;; should not go there. if it goes there, we fail the test
       (is (= 0 1))

       (catch Object {:keys [error-type]}
         (is (= expected-error error-type)))))))


;; Test the helper to create the URLs
(deftest join-url-parts-test

  (testing "Create document URL with slash at the end of URL"
    (let [expected-url "http://localhost:9200/comments/doc/12345"
          id "12345"
          index "comments"
          es-url "http://localhost:9200/"]
      (is (= expected-url (join-url-parts es-url index "doc" id)))))

  (testing "Create document URL without slash."
    (let [expected-url "http://localhost:9200/comments/doc/12345"
          id "12345"
          index "comments"
          es-url "http://localhost:9200"]
      (is (= expected-url (join-url-parts es-url index "doc" id)))))

  (testing "Search URL for indices"
    (let [expected-url "http://localhost/comments/_search"
          index "comments"
          es-url "http://localhost"]
      (is (= expected-url (join-url-parts es-url index "_search"))))))
