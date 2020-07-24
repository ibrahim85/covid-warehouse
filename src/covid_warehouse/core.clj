(ns covid-warehouse.core
  (:gen-class)
  (:require [java-time :as t]
            [clojure.string :as str]
            [next.jdbc :as jdbc]
            [covid-warehouse.db :refer :all]))

(defn dw-series [ds country state county]
  (cond
    (nil? county)
    (->>
     (dw-series-by-state ds country state)
     (map
      (comp
       println
       (partial str/join " ")
       (juxt
        :DIM_DATE/YEAR
        :DIM_DATE/MONTH
        :DIM_DATE/DAY_OF_MONTH
        :DIM_LOCATION/COUNTRY
        :DIM_LOCATION/STATE
        :CASE_CHANGE
        :DEATH_CHANGE
        :RECOVERY_CHANGE)))
     doall)
    :else
    (->>
     (dw-series-by-county ds country state county)
     (map
      (comp
       println
       (partial str/join " ")
       (juxt
        :DIM_DATE/YEAR
        :DIM_DATE/MONTH
        :DIM_DATE/DAY_OF_MONTH
        :DIM_LOCATION/COUNTRY
        :DIM_LOCATION/STATE
        :DIM_LOCATION/COUNTY
        :FACT_DAY/CASE_CHANGE
        :FACT_DAY/DEATH_CHANGE
        :FACT_DAY/RECOVERY_CHANGE)))
     doall)))

(defn -main
  [action & args]

  (jdbc/with-transaction [con ds]
    (cond
      (= "load" action)
      (do
        (println "staging data")
        (create-stage! con)
        (time
          (stage-data!
            con 
            (first args)))

        (println "loading dimensions")
        (create-dims! con)
        (time (load-dim-location! con))
        (time (load-dim-date! con))

        (println "loading facts")
        (drop-fact-day! con)
        (create-fact-day! con)
        (time (load-fact-day! con)))
      (= "query" action)
      (let [[country state county] args]
        (println "querying for" country state county)
        (dw-series con country state county)
        (println "totals")
        (cond
          (nil? county)
          (->>
            (dw-sums-by-state con country state)
            (map (comp println (partial str/join " ") vals))
            doall)
          :else
          (->>
            (dw-sums-by-county con country state county)
            (map (comp println (partial str/join " ") vals))
            doall))))))

(comment
  (-main)

  (create-stage! ds)

  (time (stage-data! ds "/home/john/workspace/COVID-19/csse_covid_19_data/csse_covid_19_daily_reports"))

  (create-dims! ds)

  (time (load-dim-location! ds))

  (time (load-dim-date! ds))

  (dim-locations ds)

  (dim-dates ds)

  (drop-fact-day! ds)

  (create-fact-day! ds)

  (time (load-fact-day! ds))

  (take 20 (fact-days ds))

  (count (fact-days ds))

  (take 20 (map :date (staged-data ds)))

  (dim->lookup (map (partial take 2) (dim-dates ds)))

  (t/local-date (t/java-date) "UTC")

  (map
   (comp prn vals)
   (cases-by-window ds "US" "Pennsylvania" (t/local-date) 14))

  (map
   (comp
    prn
    (juxt
     :DIM_DATE/YEAR
     :DIM_DATE/MONTH
     :DIM_DATE/DAY_OF_MONTH
     :DIM_LOCATION/COUNTRY
     :DIM_LOCATION/STATE
     :DIM_LOCATION/COUNTY
     :FACT_DAY/CASE_CHANGE
     :FACT_DAY/DEATH_CHANGE))
   (dw-series-by-county ds "US" "Pennsylvania" "Lancaster"))

  (->>
   (deaths-by-country ds)
   (map vals)
   (map prn))

  nil)
