(ns integrant.repl.state
  #?(:clj (:require [clojure.tools.namespace.repl :as repl])))

#?(:clj (repl/disable-reload!))

(def config   nil)
(def system   nil)
(def preparer nil)
