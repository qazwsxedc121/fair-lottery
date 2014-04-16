(ns fair-lottery.util
  (:require [noir.io :as io]
            [markdown.core :as md])
  (:use pandect.core))

(defn md->html
  "reads a markdown file from public/md and returns an HTML string"
  [filename]
  (->>
    (io/slurp-resource filename)
    (md/md-to-html-string)))

(defn hash-choice [data amount]
  (let [x-str (sha256 data)]
    (mod (BigInteger. x-str 16) amount)))





