(ns integrant.repl
  "Convenience unctions for running Integrant at the REPL. Follows the
  guidelines set out in Stuart Sierra's reloaded workflow.
  https://www.cognitect.com/blog/2013/06/04/clojure-workflow-reloaded"
  (:require [clj-reload.core :as reload]
            [integrant.core :as ig]
            [integrant.repl.state :as state]))

(defn set-prep!
  "Set the function that's called by [[prep]]. This function should take
  zero arguments and return an Integrant configuration."
  [prep]
  (alter-var-root #'state/preparer (constantly prep)))

(defn- prep-error []
  (Error. "No system preparer function found."))

(defn prep
  "Uses the function passed to [[set-prep!]] to load in an Integrant
  configuration."
  []
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
  "Initiate the current configuration into a running system. Requires [[prep]]
  to be called first to create the configuration. An optional collection of
  keys may be supplied to initiate a subset of the configuration."
  ([] (init nil))
  ([keys]
   (alter-var-root #'state/system (fn [sys]
                                    (halt-system sys)
                                    (init-system state/config keys)))
   :initiated))

(defn go
  "Runs [[prep]] then [[init]]. Accepts an optional collection of keys to
  pass to init."
  ([] (go nil))
  ([keys]
   (prep)
   (init keys)))

(defn clear
  "Halt the running system and set the current configuration to nil."
  []
  (alter-var-root #'state/system (fn [sys] (halt-system sys) nil))
  (alter-var-root #'state/config (constantly nil))
  :cleared)

(defn halt
  "Halt the running system."
  []
  (halt-system state/system)
  (alter-var-root #'state/system (constantly nil))
  :halted)

(defn suspend
  "Suspend the running system so that it can be resumed later via [[resume]]."
  []
  (when state/system (ig/suspend! state/system))
  :suspended)

(defn resume
  "Resume the system that has been suspended via [[suspend]]."
  []
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
  (let [result (reload/reload (assoc opts :log-fn (fn [_])))]
    (prn :reloaded (apply list (:loaded result)))))

(defn reset
  "Suspend the current running system via [[suspend]], reload any changed
  namespaces, and then finally resume the system with [[resume]]."
  []
  (suspend)
  (reload {})
  ((requiring-resolve `resume)))

(defn reset-all
  "As [[reset]], except that *all* namespaces are reloaded."
  []
  (suspend)
  (reload {:only :loaded})
  ((requiring-resolve `resume)))

(defn set-reload-options!
  "Set options for which files to reload when calling [[reset]] or
  [[reset-all]]. Takes the following options:

  `:dirs`
  : a collection of directories to search for modified files. If `nil`,
  then all local directories on the classpath are checked. Defaults to
  `nil`.

  `:file-pattern`
  : only files matching this regular expression are checked. Defaults to
  `#\".*\\.cljc?\"`."
  [{:keys [dirs file-pattern]
    :or   {dirs nil, file-pattern #".*\.cljc?"}}]
  (reload/init {:dirs dirs, :files file-pattern}))
