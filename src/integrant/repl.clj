(ns integrant.repl
  (:require [integrant.core :as ig]
            [clojure.tools.namespace.repl :as repl]))

(repl/disable-reload!)

(def config nil)

(def system nil)

(def preparer nil)

(defn set-prep! [prep]
  (alter-var-root #'preparer (constantly prep)))

(defn- prep-error []
  (Error. "No system preparer function found."))

(defn prep []
  (if-let [prep preparer]
    (do (alter-var-root #'config (fn [_] (prep))) :prepped)
    (throw (prep-error))))

(defn- halt-system [system]
  (when system (ig/halt! system)))

(defn init []
  (alter-var-root #'system (fn [sys] (halt-system sys) (ig/init config)))
  :initiated)

(defn go []
  (prep)
  (init))

(defn clear []
  (alter-var-root #'system (fn [sys] (halt-system sys) nil))
  (alter-var-root #'config (constantly nil))
  :cleared)

(defn halt []
  (halt-system system)
  :halted)

(defn suspend []
  (when system (ig/suspend! system))
  :suspended)

(defn resume []
  (if-let [prep preparer]
    (let [cfg (prep)]
      (alter-var-root #'config (constantly cfg))
      (alter-var-root #'system (fn [sys] (if sys (ig/resume cfg sys) (ig/init cfg))))
      :resumed)
    (throw (prep-error))))

(defn reset []
  (suspend)
  (repl/refresh :after 'integrant.repl/resume))

(defn reset-all []
  (suspend)
  (repl/refresh-all :after 'integrant.repl/resume))
