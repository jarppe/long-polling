(ns longpolling.app
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [noir.response :as resp]
            [cheshire.core :as json]
            [longpolling.job :as job]))

(require 'clojure.pprint)

(defn ->long [v]
  (if (string? v) (Long/parseLong v) v))

(defroutes app-routes
  (POST "/start" []
    (resp/json (job/start)))
  (GET "/status" {{id :id version :version timeout :timeout :or {version "0" timeout "60000"}} :params}
    (resp/json (job/status id (->long version) (->long timeout))))
  (route/not-found "wut?"))
