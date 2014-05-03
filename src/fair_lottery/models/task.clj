(ns fair-lottery.models.task
  (:require [fair-lottery.models.db :as db]
            [clj-time.coerce :as coerce])
  (:use [cronj.core]))

(defn end-draw-handler [t opts]
  (let [draws (db/get-draw-list-notend)
        draws-should-end (filter #(> (coerce/to-long t) (* (% :endtime) 1000)) draws)]
    (doseq [draw draws-should-end]
      (db/update-draw-end (draw :id)))))

(def end-draw-task
  {:id "end-draw-task"
   :handler end-draw-handler
   :schedule "0 0 * * * * *"
   :opts {}})

(def cj (cronj :entries [end-draw-task]))

(defn task-info []
  (get-threads cj "end-draw-task"))

(defn start-cj! []
  (start! cj))
