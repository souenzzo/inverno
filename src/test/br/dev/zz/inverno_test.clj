(ns br.dev.zz.inverno-test
  (:require [clojure.test :refer [deftest is]])
  (:import (br.dev.zz.inverno Inverno)
           (clojure.lang IDeref)
           (java.net.http HttpClient HttpResponse)
           (java.util.concurrent CompletableFuture)
           (org.graalvm.polyglot Context)
           (org.graalvm.polyglot.proxy ProxyExecutable)))

(comment
  (require 'virgil)
  (virgil/compile-java ["src/main"]))

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

(deftest fetch-http-status
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

;; https://common-min-api.proposal.wintercg.org/
(deftest common-index-api-interfaces
  (with-open [ctx (Context/create (into-array String ["js"]))]
    (Inverno/wintercg ctx)
    (is (= "function"
          (str (.eval ctx "js" "typeof TextEncoder"))))))

;; https://common-min-api.proposal.wintercg.org/
(deftest common-index-api-global-methods-properties
  (with-open [ctx (Context/create (into-array String ["js"]))]
    (Inverno/wintercg ctx)
    (is (= "object"
          (str (.eval ctx "js" "typeof globalThis"))))
    (is (= "true"
          (str (.eval ctx "js" "Object.is(globalThis, globalThis.self)"))))
    (is (= "function"
          (str (.eval ctx "js" "typeof globalThis.btoa"))))
    (is (= "function"
          (str (.eval ctx "js" "typeof globalThis.atob"))))
    (is (= "object" (str (.eval ctx "js" "typeof globalThis.console"))))
    #_(is (= "object" (str (.eval ctx "js" "typeof globalThis.crypto"))))
    #_(is (= "object" (str (.eval ctx "js" "typeof globalThis.navigator"))))
    #_(is (= "object" (str (.eval ctx "js" "typeof globalThis.navigator.userAgent"))))
    #_(is (= "object" (str (.eval ctx "js" "typeof globalThis.queueMicrotask"))))
    #_(is (= "object" (str (.eval ctx "js" "typeof globalThis.setTimeout"))))
    #_(is (= "object" (str (.eval ctx "js" "typeof globalThis.clearTimeout"))))
    #_(is (= "object" (str (.eval ctx "js" "typeof globalThis.setInterval"))))
    #_(is (= "object" (str (.eval ctx "js" "typeof globalThis.clearInterval"))))
    #_(is (= "object" (str (.eval ctx "js" "typeof globalThis.structuredClone"))))))

(deftest atob-btoa
  (with-open [ctx (Context/create (into-array String ["js"]))]
    (Inverno/wintercg ctx)
    (is (= "YWJj"
          (str (.eval ctx "js" "globalThis.btoa('abc')"))))
    (is (= "abc"
          (str (.eval ctx "js" "globalThis.atob('YWJj')"))))))

(deftest text-encoder
  (with-open [ctx (Context/create (into-array String ["js"]))]
    (Inverno/wintercg ctx)
    (is (= "Uint8Array(3)[226, 130, 172]"
          (str (.eval ctx "js" "(new TextEncoder()).encode('€')"))))))
