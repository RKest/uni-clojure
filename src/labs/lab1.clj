(ns labs.lab1
  (:require [clj-http.client :as client])
  (:require [hickory.core :as h])
  (:require [hickory.select :as s])
  (:require [clojure.string :as str]))

(defn internal_href? [url] (str/starts-with? url "/wiki"))
(defn section_href? [url] (str/includes? url ":"))
(defn main_page_href? [url] (str/includes? url "Main_Page"))
(defn make_full_href [url] (str "https://en.wikipedia.org" url))
(defn href [tag] (get-in tag [:attrs :href]))

(defn links_for_url [url]
  (reduce conj [] (->> url
    client/get :body h/parse h/as-hickory
    (s/select (s/tag :a))
    (map href)
    (filter (comp not nil?))
    (filter internal_href?)
    (filter (comp not main_page_href?))
    (filter (comp not section_href?))
    (map make_full_href)
    (distinct))))

(defn get_links [url depth max_depth]
  (if (= depth max_depth) '()
  (let [links (links_for_url url) next_depth_links #(get_links % (+ 1 depth) max_depth)] 
    (concat links (reduce concat (map next_depth_links links))))))

(defn -main
  [& args]
  (if (not= 2 (count args))
    (println "lein run -m labs.lab1 -- URL MAX_DEPTH")
    (let [url (nth args 0) max_depth (Integer/parseInt (nth args 1))]
    (time (->>
      (get_links url 0 max_depth)
      (distinct)
      (run! println))))))
