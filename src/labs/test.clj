(ns labs.test
  (:require [clojure.core.reducers :as r]))

(defn factorial [v] (->> (range 1 (inc v)) (reduce +)))

(time (->> 20000 range (into []) (r/map factorial) (r/fold +)))
