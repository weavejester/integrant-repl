(ns integrant.repl
  (:require [integrant.core :as ig]
            [integrant.repl.state :as state]
            #?(:clj [clojure.tools.namespace.repl :as repl])))

#?(:clj (repl/disable-reload! (find-ns 'integrant.core)))

(defn set-prep! [prep]
  #?(:clj (alter-var-root #'state/preparer (constantly prep))
     :cljs (set! state/preparer prep)))

#?(:clj (defn error [s] (Error. s))
   :cljs (defn error [s] (js/Error. s)) )

(defn- prep-error []
  (error "No system preparer function found."))

(defn prep []
  (if-let [prep state/preparer]
    (do #?(:clj (alter-var-root #'state/config (fn [_] (prep)))
           :cljs (set! state/config (prep)))
        :prepped)
    (throw (prep-error))))

(defn- halt-system [system]
  (when system (ig/halt! system)))

(defn- build-system [build wrap-ex]
  (try
    (build)
    (catch #?(:clj clojure.lang.ExceptionInfo
              :cljs ExceptionInfo) ex
      (if-let [system (:system (ex-data ex))]
        (try
          (ig/halt! system)
          (catch #?(:clj clojure.lang.ExceptionInfo
                    :cljs ExceptionInfo)
                 halt-ex
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
   #?(:clj (alter-var-root #'state/system (fn [sys]
                                            (halt-system sys)
                                            (init-system state/config keys)))
      :cljs (set! state/system (do (halt-system state/system)
                                   (init-system state/config keys))))
   :initiated))

(defn go
  ([] (go nil))
  ([keys]
   (prep)
   (init keys)))

(defn clear []
  #?(:clj (do (alter-var-root #'state/system (fn [sys] (halt-system sys) nil))
              (alter-var-root #'state/config (constantly nil)))
     :cljs (do (set! state/system (do (halt-system state/system) nil))
               (set! state/config nil)))
  :cleared)

(defn halt []
  (halt-system state/system)
  #?(:clj (alter-var-root #'state/system (constantly nil))
     :cljs (set! state/system nil))
  :halted)

(defn suspend []
  (when state/system (ig/suspend! state/system))
  :suspended)

(defn resume []
  (if-let [prep state/preparer]
    (let [cfg (prep)]
      #?(:clj (do (alter-var-root #'state/config (constantly cfg))
                  (alter-var-root #'state/system (fn [sys]
                                                   (if sys
                                                     (resume-system cfg sys)
                                                     (init-system cfg nil)))))
         :cljs (do (set! state/config cfg)
                   (set! state/system (if state/system
                                        (resume-system cfg state/system)
                                        (init-system cfg nil)))))
      :resumed)
    (throw (prep-error))))

(defn reset []
  (suspend)
  #?(:clj (repl/refresh :after 'integrant.repl/resume)
     :cljs (integrant.repl/resume)))


(defn reset-all []
  (suspend)
  #?(:clj (repl/refresh-all :after 'integrant.repl/resume)
     :cljs (integrant.repl/resume)))
