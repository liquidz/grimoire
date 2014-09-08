(ns grimoire.web.routes
  (:require [compojure.core :refer [defroutes context GET]]
            [compojure.route :as route]
            [grimoire.web.util :as util]
            [grimoire.web.views :as views]
            [ring.util.response :as response]
            [taoensso.timbre :as timbre :refer [info warn]]))

(defroutes app
  (GET "/" {uri :uri}
       (info (pr-str {:uri uri :type :html}))
       (views/home-page))

  (GET "/about" {uri :uri}
       (info (pr-str {:uri uri :type :html}))
       (views/markdown-page "about"))

  (GET "/contributing" {uri :uri}
       (info (pr-str {:uri uri :type :html}))
       (views/markdown-page "contributing"))

  (GET "/api" {uri :uri}
       (info (pr-str {:uri uri :type :html}))
       (views/markdown-page "API"))

  (GET "/favicon.ico" []
       (response/redirect "/public/favicon.ico"))

  (route/resources "/public")

  (context "/:version" [version]
           (GET "/" {uri :uri}
                (when (#{"1.4.0" "1.5.0" "1.6.0"} version)
                  (views/version-page version)))

           (context "/:namespace" [namespace]
                    (GET "/" {uri :uri}
                         (views/namespace-page-memo version namespace))

                    (context "/:symbol" [symbol]
                             (GET "/" {header-type :content-type
                                       {param-type :type} :params
                                       :as req
                                       uri :uri}
                                  (let [type (or header-type param-type :html)]
                                    (if (#{"catch" "finally"} symbol)
                                      (response/redirect (str "/" version "/clojure.core/try/"))
                                      (when-let [res (views/symbol-page version namespace symbol type)]
                                        (info (pr-str {:uri uri :type :html}))
                                        res))))

                             (GET "/docstring" {uri :uri}
                                  (let [f  (util/resource-file version namespace symbol "docstring.md")]
                                    (when (and f (.isFile f))
                                      (info (pr-str {:uri uri :type :text}))
                                      (slurp f))))

                             (GET "/extended-docstring" {uri :uri}
                                  (let [f (util/resource-file version namespace symbol "extended-docstring.md")]
                                    (when (and f (.isFile f))
                                      (info (pr-str {:uri uri :type :text}))
                                      (slurp f))))

                             (GET "/related" {uri :uri}
                                  (let [f (util/resource-file version namespace symbol "related.txt")]
                                    (when (and f (.isFile f))
                                      (info (pr-str {:uri uri :type :text}))
                                      (slurp f))))

                             (GET "/examples" {uri :uri}
                                  (when-let [examples (views/all-examples version namespace symbol :text)]
                                    (info (pr-str {:uri uri :type :text}))
                                    examples))

                             (route/not-found
                              (fn [{uri :uri}]
                                (warn (pr-str {:uri uri}))
                                (views/error-unknown-symbol version namespace symbol))))

                    (route/not-found
                     (fn [{uri :uri}]
                       (warn (pr-str {:uri uri}))
                       (views/error-unknown-namespace version namespace))))

           (route/not-found
            (fn [{uri :uri}]
              (warn (pr-str {:uri uri}))
              (views/error-unknown-version version))))

  (route/not-found
   (fn [{uri :uri}]
     (warn (pr-str {:uri uri}))
     (views/error-404))))
