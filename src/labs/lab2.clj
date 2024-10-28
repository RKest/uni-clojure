(ns labs.lab2
  (:require [seesaw.core :as ssw])
  (:require [seesaw.font :as font])
  (:require [seesaw.mig :as mig]))

(ssw/native!)

(def editor (ssw/editor-pane :text "3" :font (font/font :size 30)))
(def progress (ssw/progress-bar :orientation :horizontal))
(def set-progress! (partial ssw/config! progress :value))

(def progress-max (atom 100))
(defn factorial-progress! [n] (set-progress! (* 100 (float (/ n @progress-max)))))

(defn printlnts [& args] (locking *out* (println args)))

(def runner (atom nil))
(defmacro run-bg [& args]
  `(do
    (when-let [prev# @runner] (printlnts "Did cancel previous:" (future-cancel prev#)))
    (reset! runner (future (~@args)))))

(defn inter-handle [f] #(if-not (Thread/interrupted) (do (factorial-progress! %) (f %)) (throw (InterruptedException. ""))))
(defn factorial [n]
    (reset! progress-max n)
    (let [result (reduce *' (take n (iterate (inter-handle inc) 1N)))]
      (set-progress! 100)
      result))
(defn factorial-edit []
  (try
    (->> editor ssw/text bigint factorial str (ssw/set-text* editor))
    (catch InterruptedException _ ())))

(defn a-factorial  [_] (run-bg (factorial-edit)))

(def main-panel (mig/mig-panel 
                  :constraints ["fill, ins 0"] 
                  :items [[(ssw/scrollable editor) "grow"] [progress "dock south"]]))
(def menus
  (let [a-factorial (ssw/action :handler a-factorial :name "Calculate factorial" :tip "Calculate factorial" :key "menu N")]
    (ssw/menubar :items [(ssw/menu :text "Eval" :items [a-factorial])])))

(defn run-ssw [root] (->> (ssw/invoke-now root) ssw/pack! ssw/show!))
(defn -main [& args]
  (run-ssw 
    (ssw/frame
      :title "Seesaw factorial multithreaded calculator"
      :content main-panel
      :minimum-size [640 :by 480]
      :menubar menus
      :on-close :exit args)))
