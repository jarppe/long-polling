(ns longpolling.app
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [noir.response :as resp]
            [cheshire.core :as json]
            [slingshot.slingshot :refer [try+ throw+]]))

(require 'clojure.pprint)

(defonce jobs (ref {}))
(defonce job-id (atom 0))
(defonce deliverer (agent nil))

(defn next-job-id []
  (str (swap! job-id inc)))

(defn create-job [id]
  {:id id
   :version 0
   :status :init
   :next (promise)})

(defn trim [job]
  (when job
    (select-keys job [:id :version :status :value])))

(defn start []
  (let [id (next-job-id)
        job (create-job id)]
    (dosync
      (alter jobs assoc id job))
    (resp/json (trim job))))

(defn job-ref [id version]
  (dosync
    (if-let [job (get @jobs id)]
      (if (= version (:version job))
        (:next job)
        (doto (promise) (deliver job)))
      (throw+ {:status 404 :message (str "unknown job: id=" id)}))))

(defn status [id version timeout]
  (let [r (job-ref id version)
        v (deref r timeout :timeout)]
    (if (= v :timeout)
      (resp/json {:status :timeout})
      (resp/json {:status :updated :job (trim v)}))))

(defn update [id value]
  (dosync
    (if-let [job (get @jobs id)]
      (let [p (:next job)
            new-job (assoc job
                           :version (inc (:version job))
                           :status :run
                           :next (promise)
                           :value value)]
        (alter jobs assoc id new-job)
        (send deliverer (fn [_] (deliver p (trim new-job)) nil))
        (resp/json {:updated id :version (:version new-job)}))
      (throw+ {:status 404 :message (str "unknown job: id=" id)}))))

(defn ->long [v]
  (if (string? v) (Long/parseLong v) v))

(defroutes app-routes
  (POST "/start" [] (start))
  (GET "/status" {{id :id version :version timeout :timeout :or {version "0" timeout "60000"}} :params} (status id (->long version) (->long timeout)))
  (POST "/update" {{id :id value :value} :params} (update id value))
  (GET "/ping" [] (fn [r] (println "PING:") (clojure.pprint/pprint r) (resp/json (dissoc r :body))))
  (GET "/bang" [] (throw+ {:status 501 :message "oh noes"}))
  (route/not-found "wut?"))
