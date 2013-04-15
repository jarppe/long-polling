(ns longpolling.app
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [noir.response :as resp]
            [cheshire.core :as json]
            [longpolling.job :as job]))

(require 'clojure.pprint)

(defn ->long [v]
  (if (string? v) (Long/parseLong v) v))

(defn- initial-value []
  (zipmap (range 10) (repeat :working)))

(defn- status-fn [new-value]
  (if (every? (partial = :done) (vals new-value))
    :done
    :run))

(defn- start-new-job []
  (let [job (job/start (initial-value) status-fn)]
    (doseq [n (range 10)]
      (future
        (Thread/sleep (+ 100 (rand-int 2900)))
        (job/update (:id job) assoc n :done)))
    job))

(defroutes app-routes
  (POST "/start" []
    (resp/json (start-new-job)))
  (GET "/status" {{id :id version :version timeout :timeout :or {version "0" timeout "60000"}} :params}
    (resp/json (job/status id (->long version) (->long timeout))))
  (route/not-found "wut?"))
