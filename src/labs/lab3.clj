(ns labs.lab3
  (:require [clj-http.client :as client])
  (:require [hickory.core :as h])
  (:require [hickory.select :as s])
  (:require [clojure.string :as str])
  (:require [clojure.core.async :as async :refer [>! <! >!! <!!]]))

(def dup-set (atom #{}))
(defn duplicate? [v] (@dup-set v))
(defn add-to-dup-set [v] (swap! dup-set conj v) v)

(defn internal_href? [url] (str/starts-with? url "/wiki"))
(defn section_href? [url] (str/includes? url ":"))
(defn main_page_href? [url] (str/includes? url "Main_Page"))
(defn make_full_href [url] (str "https://en.wikipedia.org" url))
(defn href [tag] (get-in tag [:attrs :href]))

(defn hrefs-on-page [url]
  (->> url client/get :body h/parse h/as-hickory (s/select (s/tag :a)) (map href)))
(def valid-href
  (every-pred some? internal_href? (comp not main_page_href?) (comp not section_href?) (comp not duplicate?)))

(def processed-count (atom 1))
(defn push-process! [] (swap! processed-count inc))
(defn pop-process! [] (swap! processed-count dec))
(defn has-process? [] (not (compare-and-set! processed-count 0 0)))

(defn links_for_url [url]
  (->>
    (hrefs-on-page url)
    (filter valid-href)
    (mapv make_full_href)
    (mapv add-to-dup-set)))

(defn recurse-links [chan]
  (while (has-process?)
    (when-let [[depth url] (<!! chan)]
      (async/go
        (pop-process!)
        (when (and (zero? depth) (not (has-process?))) (async/close! chan))
        (let [next-urls (links_for_url url)]
          (when-not (zero? depth)
            (doseq [u next-urls]
              (push-process!)
              (>! chan [(dec depth) u]))))))))

(defn -run [url depth]
  (let [chan (async/chan) f (future (recurse-links chan))]
    (>!! chan [(dec depth) url])
    @f))

(defn -main [& args]
  (if
    (not= 2 (count args))
    (println "lein run -m labs.lab1 -- URL MAX_DEPTH")
    (let [url (nth args 0) max_depth (Integer/parseInt (nth args 1))]
      (-run url max_depth)
      (doseq [v @dup-set] (println v))
      )))


Zakłócenia są sygnałami które użytkownik uznał za nieptrzydatne
