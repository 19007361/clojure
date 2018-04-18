(ns simpleserver.domain
  (:require
    [clojure.data.csv :as csv]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.tools.logging :as log])
  )


; Store all domain objects to this cache once read from csv files.
(def my-domain-atom (atom {}))

(defn get-product-groups
  "Get product groups"
  []
  (log/trace "ENTER get-product-groups")
  (if-let [product-groups (@my-domain-atom :product-groups)]
    product-groups
    (let [raw (with-open [reader (io/reader "resources/product-groups.csv")]
                (doall
                  (csv/read-csv reader)))
          product-groups-from-file (into {} (map
                                              (fn [[item]]
                                                (str/split item #"\t"))
                                              raw))]
      (swap! my-domain-atom assoc :product-groups product-groups-from-file)
      product-groups-from-file)))



(defn -get-raw-products
  "Get raw products for a product group, returns the whole product information for each product"
  [pg-id]
  (log/trace (str "ENTER get-raw-products, pg-id: " pg-id))
  (let [my-key (str "pg-" pg-id "-raw-products")]
    (if-let [raw-products (@my-domain-atom my-key)]
      raw-products
      (let [raw-products-from-file (try
                  (with-open [reader (io/reader (str "resources/pg-" pg-id "-products.csv"))]
                    (doall
                      (csv/read-csv reader :separator \tab)))
                  (catch java.io.FileNotFoundException e nil))]
        (if raw-products-from-file
          (do
            (swap! my-domain-atom assoc my-key raw-products-from-file)
            raw-products-from-file)
          nil)))))


(defn get-products
  "Get products for a product group, returns list of items: [p-id, pg-id, name, price]"
  [pg-id]
  (log/trace (str "ENTER get-products, pg-id: " pg-id))
  (let [my-key (str "pg-" pg-id "-products")]
    (if-let [products (@my-domain-atom my-key)]
      products
      (let [raw (-get-raw-products pg-id)
            products-from-file (and raw
                                    (map
                                      (fn [item]
                                        (take 4 item)) raw))]
        (if products-from-file
          (do
            (swap! my-domain-atom assoc my-key products-from-file)
            products-from-file)
          nil)))))


(defn get-product
  "Gets product info for a product, returned item varies related to product group"
  [pg-id p-id]
  (log/trace (str "ENTER get-product, pg-id: " pg-id ", p-id: " p-id))
  (let [products (-get-raw-products pg-id)]
    (first (filter (fn [item]
                     (let [id (first item)]
                       (= id (str p-id))))
                   products))))