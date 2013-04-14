(ns longpolling.server
  (:require [clojure.java.io :as io]
            [clojure.walk :refer [keywordize-keys]]
            [compojure.core :refer [routes]]
            [noir.util.middleware :as nm]
            [noir.response :as resp]
            [cheshire.core :as json]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.nested-params :refer [wrap-nested-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            [slingshot.slingshot :refer [try+ throw+]]
            [longpolling.app :refer [app-routes]]))

(defn starts-with [^String s prefix]
  (when (and s prefix)
    (.startsWith s prefix)))

(require 'clojure.pprint)

(defn wrap-json [handler]
  (fn [request]
    (let [json-body (if (starts-with (:content-type request) "application/json")
                      (if-let [body (:body request)]
                        (-> body
                          (io/reader :encoding (or (:character-encoding request) "utf-8"))
                          json/parse-stream
                          keywordize-keys)
                        {}))
          request (assoc request :json json-body)
          request (if json-body (assoc request :params json-body) request)]
      (handler request))))

(defn wrap-sling [handler]
  (fn [request]
    (try+
      (handler request)
      (catch [:error :not-found] {message :message :or {message "not found"}}
        (resp/status 404 message))
      (catch map? {:keys [status message] :or {status 500 message "ups"}}
        (resp/status status message)))))

(def prod-app
  (-> app-routes
    (wrap-json)
    (wrap-sling)
    (wrap-keyword-params)
    (wrap-nested-params)
    (wrap-params)
    (wrap-resource "public")
    (wrap-file-info)))

(def dev-app
  (-> prod-app
    (wrap-reload)))

(defn run [& {:keys [mode port] :or {mode :dev port 8080}}]
  (let [app (if (= :prod mode) prod-app (var dev-app))]
    (jetty/run-jetty app {:port port :join? false})))
