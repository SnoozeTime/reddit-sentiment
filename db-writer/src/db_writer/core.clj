(ns db-writer.core
  (:gen-class)
  (:import [org.zeromq ZMQ ZMQ$Socket]
           [org.zeromq ZContext]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [context (ZContext.)
        socket (.createSocket context ZMQ/REP)]
    ;; Bind to a port
    (.bind socket "tcp://*:5556")

    (while true
      ;; Wait for the next request from the client
      (let [request (.recv socket 0)
            decoded-request (String. request)]
        (println "Received " decoded-request " from client")

        ;; Do some work;
        (Thread/sleep 1000)
        
        ;; Send back the request
        ;; need to encode as bytes
        (.send socket (.getBytes decoded-request) 0)))))
