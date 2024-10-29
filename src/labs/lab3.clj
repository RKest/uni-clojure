(ns labs.lab3
  (:require [clj-http.client :as client])
  (:require [hickory.core :as h])
  (:require [hickory.select :as s])
  (:require [clojure.string :as str])
  (:require [clojure.core.async :as async :refer [<! <!!]]))

(defn internal_href? [url] (str/starts-with? url "/wiki"))
(defn section_href? [url] (str/includes? url ":"))
(defn main_page_href? [url] (str/includes? url "Main_Page"))
(def valid-href? (every-pred some? internal_href? (comp not main_page_href?) (comp not section_href?)))

(defn make_full_href [url] (str "https://en.wikipedia.org" url))
(defn href [tag] (get-in tag [:attrs :href]))

(defn hrefs-on-page [url] (->> (client/get url) :body h/parse h/as-hickory (s/select (s/tag :a)) (map href)))

(def result (atom #{}))
(defn duplicate? [v] (@result v))
(defn result-add [v] (swap! result conj v))

(defn hrefs-at-page [url]
  (->>
    (hrefs-on-page url)
    (filter valid-href?)
    (map make_full_href)
    (filter (comp not duplicate?))
    ))

(defn run [depth url]
  (result-add url)
  (async/go (when-not (zero? depth)
      (<! (->> (hrefs-at-page url) (mapv (partial run (dec depth))) async/merge))
  )))

(defn -main [& args]
  (if
    (not= 2 (count args))
    (println "lein run -m labs.lab1 -- URL MAX_DEPTH")
    (let [url (nth args 0) max_depth (Integer/parseInt (nth args 1))]
      (time (<!! (run max_depth url)))
      (doseq [v @result] (println v))
      )))
