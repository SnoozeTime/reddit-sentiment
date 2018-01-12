(ns worker.core
  (:gen-class)
  (:import [org.zeromq ZMQ ZMQ$Socket]
           [org.zeromq ZContext]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [context (ZContext.)
        socket (.createSocket context ZMQ/REQ)]
    ;; Bind to a port
    (.connect socket "tcp://localhost:5556")

    (doseq [x (range 1 20)]
      (do
        (println "Sending Hello " x)
        (let [request (str "hello" x)]
          (.send socket (.getBytes request) 0)
          (println (String. (.recv socket 0 ))))))
    (.close socket)
    (.term context)))
