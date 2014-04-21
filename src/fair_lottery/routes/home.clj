(ns fair-lottery.routes.home
  (:use compojure.core)
  (:require [fair-lottery.views.layout :as layout]
            [fair-lottery.util :as util]
            [fair-lottery.models.db :as db]
            [noir.session :as session]))

(defn home-page []
  (layout/render
    "home.html" {}))

(defn about-page []
  (layout/render "about.html" {:content (util/md->html "/md/about.md")}))

(defn user-draw []
  (if-let [user-id (session/get :user-id)]
    (let [user (db/get-user user-id)
          draw-attend-ids (:draw_attend user)
          draw-hold-ids (:draw_hold user)
          draw-attend (db/get-draw-list-with-ids draw-attend-ids)
          draw-hold (db/get-draw-list-with-ids draw-hold-ids)]
      (layout/render "user-draw.html" {:draw-attended draw-attend
                                       :draw-hold draw-hold}))
    (layout/render "about.html" {:content "<h1>Please login!</h1>"})))

(defn- not-attended [user-id users]
  (if user-id
    (every? #(not= user-id (% :email)) users)
    true))

(defn draw-page [draw-id]
  (let [draw (db/get-draw draw-id)
        user-id (session/get :user-id)]
    (if draw
      (let [users (:user draw)]
        (if (draw :end)
          (layout/render "draw.html" {:users users
                                      :winner-email (:email (:winner (:result draw)))
                                      :draw-id draw-id})
          (layout/render "draw-notend.html" {:users users
                                             :draw-id draw-id
                                             :not-attended (not-attended user-id users)
                                             :endtime (:endtime draw)})))
      (layout/render "home.html" {}))))

(defn draw-data [draw-id]
  (let [draw (db/get-draw draw-id)]
    {:headers {"Content-Disposition" "attachment; filename=\"data.json\""}
     :body (:user draw)}))

(defn draw-list-page []
  (let [draw-list-end (db/get-draw-list-end)
        draw-list-notend (db/get-draw-list-notend)]
    (layout/render "draw-list.html" {:draws draw-list-end
                                     :draws-not-end draw-list-notend})))

(defn draw-attend [draw-id random-str]
  (when-let [user-id (session/get :user-id)]
    (do
      (when (not-attended user-id (:user (db/get-draw draw-id)))
        (db/update-draw-add-gamer draw-id user-id random-str (util/current-timestamp)))
      (draw-page draw-id))))

(defn draw-create [name time-in-hour]
  (when-let [draw-id (db/create-draw name (. Integer parseInt time-in-hour))]
    (db/update-user-draw-hold (session/get :user-id) draw-id)
    (draw-list-page)))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/about" [] (about-page))
  (GET "/draw" [] (draw-list-page))
  (GET "/draw/:id" [id] (draw-page id))
  (POST "/draw-attend" [draw-id random-str] (draw-attend draw-id random-str))
  (POST "/draw-create" [name time-in-hour] (draw-create name time-in-hour))
  (GET "/draw/data/:id" [id] (draw-data id))
  (GET "/user-draw" [] (user-draw)))
