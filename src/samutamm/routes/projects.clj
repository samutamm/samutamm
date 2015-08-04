(ns samutamm.routes.projects
  (:require [compojure.core :refer :all]
      [liberator.core :refer [defresource resource request-method-in]]
      [noir.io :as io]
      [clojure.java.io :refer [file]]
      [cheshire.core :refer [generate-string]]
      [samutamm.models.db :as database]
      [clojure.data.json :as json]
      [liberator.representation :refer [ring-response]]))


(defn parse-project [context] (json/read-str
                               (slurp (get-in context [:request :body]))
                               :key-fn keyword))
(defn add-project-to-database
  [project] (let [id (+ 1 (count (database/get-all-projects)))]
                                  (try
                                    (database/update-or-create-project (or (:id project) id)
                                                                   (:projectname project)
                                                                   (:description project)
                                                                   (:tags project)
                                                                   (:projectstart project)
                                                                   (:projectend project))
                                    (catch Exception e (println (.getNextException e))))))

(defn project-is-valid [project]
  (let [fields [:projectname :description :tags :projectstart :projectend]]
    (every? true? (map (fn [field] (not (nil? (field project)))) fields))))


(defresource get-all-projects
        :allowed-methods [:get]
        :handle-ok (fn [_] (generate-string (database/get-all-projects)))
        :available-media-types ["application/json"])

(defresource add-new-project
         :allowed-methods [:post]
         :available-media-types ["application/json"]
         :malformed? (fn[ctx] (let [project (parse-project ctx)
                                    updated-ctx (assoc ctx ::project project)
                                    result (conj [] (not (project-is-valid project)) updated-ctx)]
                                  result))
         :handle-malformed (fn [_] (generate-string (str "Malformed json!")))
         :post! (fn [ctx]  { ::data (generate-string (add-project-to-database  (::project ctx))) })
         :handle-created ::data)

(defresource delete-project [id]
         :allowed-methods [:delete]
         :available-media-types ["application/json"]
         :delete! (database/delete-project (Integer/parseInt id))
         :handle-no-content  (fn [_] (generate-string (str "deleted project"))))

(defresource get-project [id]
         :allowed-methods [:get]
         :available-media-types ["application/json"]
         :handle-ok  (fn [_] (generate-string (database/get-project (Integer/parseInt id)))))

(defroutes project-routes
  (GET "/projects" request get-all-projects)
  (POST "/projects" request add-new-project)
  (DELETE "/projects/:id" [id] (delete-project id))
  (GET "/projects/:id" [id] (get-project id)))
