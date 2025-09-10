(ns integrant.repl-test
  (:require [clojure.test :refer [deftest is]]
            [integrant.core :as ig]
            [integrant.repl :as r]
            [integrant.repl.state :as rs]))

(defmethod ig/init-key ::a [_ {:keys [x]}]
  {:x (inc x)})

(deftest system-test
  (r/set-prep! (constantly {::a {:x 1}}))
  (r/go)
  (is (= {::a {:x 1}} rs/config))
  (is (= {::a {:x 2}} rs/system)))
