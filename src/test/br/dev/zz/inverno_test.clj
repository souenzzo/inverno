(ns br.dev.zz.inverno-test
  (:require [clojure.test :refer [deftest is]])
  (:import (br.dev.zz.inverno Inverno)
           (java.net.http HttpClient HttpResponse)
           (java.util.concurrent CompletableFuture)
           (org.graalvm.polyglot Context)
           (org.graalvm.polyglot.proxy ProxyExecutable)))

(set! *warn-on-reflection* true)

(deftest hello
  (System/setProperty "polyglot.engine.WarnInterpreterOnly" "false")
  (with-open [ctx (Context/create (into-array String ["js"]))]
    (let [http-client (proxy [HttpClient] []
                        (sendAsync [http-request body-handler]
                          (doto (CompletableFuture.)
                            (.complete (reify HttpResponse
                                         (statusCode [this] 200))))))
          bindings (.getBindings ctx "js")
          p (promise)]
      (Inverno/wintercg ctx http-client)
      (.putMember bindings "deliver"
        (reify ProxyExecutable
          (execute [this vs]
            (deliver p (first vs))
            nil)))
      (.eval ctx "js" "fetch('https://fetch.localhost/hello')
      .then(res => deliver(res.status))")
      (let []
        (is (= "200"
              (str (deref p 100 ::timeout))))))))
