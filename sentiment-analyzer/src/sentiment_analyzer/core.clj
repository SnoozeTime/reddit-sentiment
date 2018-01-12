(ns sentiment-analyzer.core
  (:gen-class)
  (:import [edu.stanford.nlp.pipeline Annotation StanfordCoreNLP]
           [edu.stanford.nlp.sentiment SentimentCoreAnnotations$SentimentClass]
           [edu.stanford.nlp.ling CoreAnnotations$SentencesAnnotation]
           java.util.Properties))

(def pipeline "tokenize, ssplit, pos, lemma, parse, sentiment")

(defn build-nlp
  "Builds a Stanford NLP object"
  []
  (let [p (Properties.)]
    (.put p "annotators" pipeline)
    (StanfordCoreNLP. p true)))

(defn- annotate-text
  "Runs the given text through the NLP pipeline"
  [text]
    (.process (build-nlp) text))

(defn- get-sentiment2
  [anno-sentence]
  (.get anno-sentence SentimentCoreAnnotations$SentimentClass))

(defn get-sentiment
  [text]
  (-> text
      annotate-text
      (#(.get % CoreAnnotations$SentencesAnnotation))
      (#(map get-sentiment2 %))))
q(def sam "yeah
I see. I should really make something with network calls (and not just steal it off of github) some day to grasp the basics better XD
but first I need to get my bot online...
Want to get it in simulation mode by the weekend to see feasibility
but so not comfortable with how python represents information yet
used to work with it in univ, but completely slipped from me
object types not being explicit is really throwing me off too. Have to print things or open the implementation to know what I'm looking at, and even then I'm not always sure if I'm looking at a list of arrays or a list of maps")



(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
