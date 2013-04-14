(ns longpolling.job
  (:require [slingshot.slingshot :refer [throw+]]))

(defonce jobs (ref {}))
(defonce job-id (atom 0))
(defonce deliverer (agent nil))

(defn- next-job-id []
  (str (swap! job-id inc)))

(defn- create-job [id initial-value]
  {:id id
   :version 0
   :status :init
   :value initial-value
   :next (promise)})

(defn- trim [job]
  (when job
    (select-keys job [:id :version :status :value])))

(defn update [id f & args]
  (dosync
    (if-let [job (get @jobs id)]
      (let [p (:next job)
            new-value (apply f (cons (:value job) args))
            new-status (if (every? (partial = 99) (vals new-value)) :done :run)
            new-job (assoc job :version (inc (:version job))
                               :status new-status
                               :next (promise)
                               :value new-value)]
        (alter jobs assoc id new-job)
        (send deliverer (fn [_] (deliver p (trim new-job)) nil))
        (:version new-job))
      (throw+ {:error :not-found :message (str "unknown job: id=" id)}))))

(defn start []
  (let [id (next-job-id)
        job (create-job id (zipmap (range 10) (repeat 0)))]
    (dosync
      (alter jobs assoc id job))
    (doseq [n (range 10)]
      (future
        (let [speed (+ 10 (rand-int 90))]
          (doseq [v (range 100)]
            (Thread/sleep speed)
            (update id assoc n v)))))
    (trim job)))

(defn- job-ref [id version]
  (dosync
    (if-let [job (get @jobs id)]
      (if (= version (:version job))
        (:next job)
        (doto (promise) (deliver job)))
      (throw+ {:error :not-found :message (str "unknown job: id=" id)}))))

(defn status [id version timeout]
  (let [v (deref (job-ref id version) timeout :timeout)]
    (if (= v :timeout)
      {:result :timeout}
      {:result :update :data (trim v)})))
