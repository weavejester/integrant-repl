(ns integrant.repl
  (:require [clj-reload.core :as reload]
            [integrant.core :as ig]
            [integrant.repl.state :as state]))

(defn set-prep! [prep]
  (alter-var-root #'state/preparer (constantly prep)))

(defn- prep-error []
  (Error. "No system preparer function found."))

(defn prep []
  (if-let [prep state/preparer]
    (do (alter-var-root #'state/config (fn [_] (prep))) :prepped)
    (throw (prep-error))))

(defn- halt-system [system]
  (when system (ig/halt! system)))

(defn- build-system [build wrap-ex]
  (try
    (build)
    (catch clojure.lang.ExceptionInfo ex
      (when-let [system (:system (ex-data ex))]
        (try
          (ig/halt! system)
          (catch clojure.lang.ExceptionInfo halt-ex
            (throw (wrap-ex ex halt-ex)))))
      (throw ex))))

(defn- init-system [config keys]
  (build-system
   (if keys
     #(ig/init config keys)
     #(ig/init config))
   #(ex-info "Config failed to init; also failed to halt failed system"
             {:init-exception %1}
             %2)))

(defn- resume-system [config system]
  (build-system
   #(ig/resume config system)
   #(ex-info "Config failed to resume; also failed to halt failed system"
             {:resume-exception %1}
             %2)))

(defn init
  ([] (init nil))
  ([keys]
   (alter-var-root #'state/system (fn [sys]
                                    (halt-system sys)
                                    (init-system state/config keys)))
   :initiated))

(defn go
  ([] (go nil))
  ([keys]
   (prep)
   (init keys)))

(defn clear []
  (alter-var-root #'state/system (fn [sys] (halt-system sys) nil))
  (alter-var-root #'state/config (constantly nil))
  :cleared)

(defn halt []
  (halt-system state/system)
  (alter-var-root #'state/system (constantly nil))
  :halted)

(defn suspend []
  (when state/system (ig/suspend! state/system))
  :suspended)

(defn resume []
  (if-let [prep state/preparer]
    (let [cfg (prep)]
      (alter-var-root #'state/config (constantly cfg))
      (alter-var-root #'state/system (fn [sys]
                                       (if sys
                                         (resume-system cfg sys)
                                         (init-system cfg nil))))
      :resumed)
    (throw (prep-error))))

(defn- reload [opts]
  (let [last-log (atom nil)]
    (reload/reload (assoc opts :log-fn #(reset! last-log %)))
    (println @last-log)))

(defn reset []
  (suspend)
  (reload {})
  ((requiring-resolve `resume))
  :reset)

(defn reset-all []
  (suspend)
  (reload {:only :loaded})
  ((requiring-resolve `resume))
  :reset)
