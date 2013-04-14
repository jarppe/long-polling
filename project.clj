(defproject longpolling "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [lib-noir "0.5.0"]
                 [compojure "1.1.5" :exclusions [ring/ring-core]]
                 [slingshot "0.10.3"]]
  :profiles {:production {:ring {:handler longpolling.server/prod-app
                                 :open-browser? false
                                 :stacktraces? false
                                 :auto-reload? false}}}
  :plugins [[lein-ring "0.8.3"]]
  :main longpolling.server
  :ring {:handler longpolling.server/dev-app
         :port 8080
         :open-browser? false}
  :repl-options {:init-ns longpolling.server}
  :min-lein-version "2.0.0")
