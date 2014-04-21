(ns fair-lottery.models.db
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.operators :refer :all]
            [fair-lottery.util :as util]
            [overtone.at-at :as at-at])
  (:import [org.bson.types ObjectId]))

;; Tries to get the Mongo URI from the environment variable
;; MONGOHQ_URL, otherwise default it to localhost
(let [uri (get (System/getenv) "MONGOHQ_URL" "mongodb://127.0.0.1/fair-lottery")]
  (mg/connect-via-uri! uri))

(defn create-user [user]
  (mc/insert "users" user))

(defn update-user [id first-name last-name email]
  (mc/update "users" {:id id}
             {$set {:first_name first-name
                    :last_name last-name
                    :email email}}))

(defn get-user [id]
  (mc/find-one-as-map "users" {:id id}))

(declare update-draw-end get-draw)

(def my-pool (at-at/mk-pool))

(defn create-draw [name time-in-hour]
  (let [timestamp (+ (util/current-timestamp) (* time-in-hour 3600))
        document (mc/insert-and-return "draw" {:name name
                                               :user []
                                               :end false
                                               :endtime timestamp})
        draw-id (document :_id)]
    (at-at/after
     (* time-in-hour 3600000)
     (fn [] (update-draw-end (str draw-id)))
     my-pool)
    (str draw-id)))

(defn update-user-draw-hold [user-id draw-id]
  (mc/update "users" {:id user-id}
                   {$push {:draw_hold draw-id}}))

(defn update-draw-add-gamer [draw-id email random-str time]
  (mc/update "users" {:id email}
             {$push {:draw_attend draw-id}})
  (mc/update-by-id "draw" (ObjectId. draw-id)
                   {$push {:user {:email email
                                  :str random-str
                                  :time time}}}))

(defn update-draw-update-result [draw-id result]
  (mc/update-by-id "draw" (ObjectId. draw-id)
                   {$set {:result result
                          :end true}}))

(defn update-draw-end [draw-id]
  (let [users (:user (get-draw draw-id))]
    (if (not-empty users)
      (let [coll-str (map #(str (:email %) (:str %) (:time %)) users)
            concat-str (apply str coll-str)
            hashed (util/hash-choice concat-str (count users))
            winner (nth users hashed)]
        (update-draw-update-result draw-id {:data concat-str :winner winner :winner-index (str hashed)}))
      (update-draw-update-result draw-id {:data "" :winner "" :winner-index 0}))))

(defn get-draw [draw-id]
  (mc/find-one-as-map "draw" {:_id (ObjectId. draw-id)}))

(defn get-draw-list-with-ids [draw-ids]
  (map #(assoc {}
          :id (% :_id)
          :name (% :name))
       (map get-draw draw-ids)))

(defn get-draw-list-end []
  (map #(assoc %
          :id (% :_id))
       (mc/find-maps "draw" {:end true})))

(defn get-draw-list-notend []
  (map #(assoc %
          :id (% :_id))
       (mc/find-maps "draw" {:end false})))

