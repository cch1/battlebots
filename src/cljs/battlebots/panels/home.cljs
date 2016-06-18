(ns battlebots.panels.home
  (:require [re-frame.core :as re-frame]
            [battlebots.components.arena :refer [render-arena]]))

(defn battlebot-game
  "renders available games"
  [game user]
  (let [game-id (:_id game)
        user-id (:_id user)
        is-registered? (boolean (first (filter #(= user-id (:_id %)) (:players game))))]
    (fn []
      [:li
       [:button {:on-click #(re-frame/dispatch [:set-active-game game])} game-id]
       (cond
        (and (not is-registered?) (= (:state game) "pending"))
        [:button {:on-click #(re-frame/dispatch [:register-user-in-game game-id user-id])} "Join Game"]
        (= (:state game) "finalized")
        [:button {:on-click #(re-frame/dispatch [:play-game game-id])} "Play Game"])])))

(defn authed-homepage
  [user games]
  [:div
   [:h3 (str "Welcome back " (:login user) "!")]
   [:p "Game ids"]
   [:ul.game-list
    (doall (for [game games]
             ^{:key (str (:_id game) "-" (count (:players game)))} [battlebot-game game user]))]
   (render-arena)])

(def unauthed-homepage
  [:div
   [:p
    [:a {:href "/signin/github"} "Signup"]
    " and get started!"]])

(defn home-panel []
  (re-frame/dispatch [:fetch-games])
  (let [games (re-frame/subscribe [:games])
        user (re-frame/subscribe [:user])]
    (fn []
      [:div.panel-home
       (if @user
         (authed-homepage @user @games)
         unauthed-homepage)])))
