(ns br.dev.zz.inverno-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.edn :as edn])
  (:import (br.dev.zz.inverno Inverno)
           (java.net.http HttpClient HttpResponse)
           (java.util.concurrent CompletableFuture)
           (org.graalvm.polyglot Context)
           (org.graalvm.polyglot.proxy ProxyExecutable)
           (clojure.lang IDeref)))


(set! *warn-on-reflection* true)

(defn ^ProxyExecutable js-promise
  []
  (let [p (promise)]
    (reify ProxyExecutable
      (execute [this vs]
        (deliver p (first vs))
        nil)
      IDeref
      (deref [this]
        (deref p 100 ::timeout)))))

(deftest hello
  (System/setProperty "polyglot.engine.WarnInterpreterOnly" "false")
  (with-open [ctx (Context/create (into-array String ["js"]))]
    (let [http-client (proxy [HttpClient] []
                        (sendAsync [http-request body-handler]
                          (doto (CompletableFuture.)
                            (.complete (reify HttpResponse
                                         (statusCode [this] 200))))))
          bindings (.getBindings ctx "js")
          p (js-promise)]
      (Inverno/wintercg ctx http-client)
      (.putMember bindings "deliver" p)
      (.eval ctx "js" "fetch('https://fetch.localhost/hello')
      .then(res => deliver(res.status))")
      (let []
        (is (= "200"
              (str @p)))))))
;; https://proposal-common-min-api.deno.dev/
(deftest global-properties
  (System/setProperty "polyglot.engine.WarnInterpreterOnly" "false")
  (with-open [ctx (Context/create (into-array String ["js"]))]
    (Inverno/wintercg ctx)
    (is (= "YWJj"
          (str (.eval ctx "js" "globalThis.btoa('abc')"))))
    (is (= "abc"
          (str (.eval ctx "js" "globalThis.atob('YWJj')"))))
    (is (= "object" (str (.eval ctx "js" "typeof globalThis.console"))))
    #_(is (= "object" (str (.eval ctx "js" "typeof globalThis.crypto"))))
    #_(is (= "object" (str (.eval ctx "js" "typeof globalThis.navigator"))))
    #_(is (= "object" (str (.eval ctx "js" "typeof globalThis.navigator.userAgent"))))
    (is (= "object" (str (.eval ctx "js" "typeof globalThis.navigator"))))))
