(ns integrant.repl
  (:require [integrant.core :as ig]
            [integrant.repl.state :as state]
            [clojure.tools.namespace.repl :as repl]))

(repl/disable-reload! (find-ns 'integrant.core))

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
      (try
        (ig/halt! (:system (ex-data ex)))
        (catch clojure.lang.ExceptionInfo halt-ex
          (throw (wrap-ex ex halt-ex))))
      (throw ex))))

(defn- init-system [config]
  (build-system
   #(ig/init config)
   #(ex-info "Config failed to init; also failed to halt failed system"
             {:init-exception %1}
             %2)))

(defn- resume-system [config system]
  (build-system
   #(ig/resume config system)
   #(ex-info "Config failed to resume; also failed to halt failed system"
             {:resume-exception %1}
             %2)))

(defn init []
  (alter-var-root #'state/system (fn [sys]
                                   (halt-system sys)
                                   (init-system state/config)))
  :initiated)

(defn go []
  (prep)
  (init))

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
                                         (init-system cfg))))
      :resumed)
    (throw (prep-error))))

(defn reset []
  (suspend)
  (repl/refresh :after 'integrant.repl/resume))

(defn reset-all []
  (suspend)
  (repl/refresh-all :after 'integrant.repl/resume))
