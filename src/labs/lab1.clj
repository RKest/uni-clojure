(ns labs.lab1
  (:require [clj-http.client :as client])
  (:require [hickory.core :as h])
  (:require [hickory.select :as s])
  (:require [clojure.string :as str]))

(def result (atom #{}))

(defn hrefs-on-page [url] (->>
  (client/get url) :body h/parse h/as-hickory (s/select (s/tag :a))
  (map #(get-in % [:attrs :href]))
  (filter (every-pred some? #(str/starts-with? % "/wiki")))
  (remove #(str/includes? % ":"))
  (remove #(str/includes? % "Main_Page"))
  (map #(str "https://en.wikipedia.org" %))
  (remove #(@result %))))

(defn run [depth url]
  (swap! result conj url)
  (when-not (zero? depth) (doseq [u (hrefs-on-page url)] (run (dec depth) u))))

(defn -main [& args]
  (if (not= 2 (count args))
    (println "lein run -m labs.lab1 -- URL MAX_DEPTH")
    (let [url (nth args 0) max_depth (Integer/parseInt (nth args 1))]
      (time (run max_depth url))
      (doseq [v @result] (println v))
    )))
