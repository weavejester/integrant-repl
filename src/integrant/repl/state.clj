(ns integrant.repl.state
  (:require [clojure.tools.namespace.repl :as repl]))

(repl/disable-reload!)

(def config nil)

(def system nil)

(def preparer nil)
