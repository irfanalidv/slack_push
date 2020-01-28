(ns slack-push.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]))

;;define hook_url
(def hook-url "https://hooks.slack.com/services/")

;;check what type of data is obtained from the following URL
#_(client/get "https://samples.openweathermap.org/data/2.5/weather?q=London,uk&appid=b6907d289e10d714a6e88b30761fae22")
;; => {:cached nil, :request-time 903, :repeatable? false, :protocol-version {:name "HTTP", :major 1, :minor 1}, :streaming? true, :http-client #object[org.apache.http.impl.client.InternalHttpClient 0x701017fb "org.apache.http.impl.client.InternalHttpClient@701017fb"], :chunked? true, :reason-phrase "OK", :headers {"Server" "openresty/1.9.7.1", "Content-Type" "application/json; charset=utf-8", "X-Content-Type-Options" "nosniff", "X-Runtime" "0.001380", "X-Frame-Options" "SAMEORIGIN", "Connection" "close", "Transfer-Encoding" "chunked", "ETag" "W/\"e70c27085ed41de5321252b16c9582fe\"", "Date" "Tue, 28 Jan 2020 17:47:17 GMT", "X-Request-Id" "c36f2685-b23a-4e52-9993-300e602940c9", "X-XSS-Protection" "1; mode=block", "Cache-Control" "max-age=0, private, must-revalidate"}, :orig-content-encoding nil, :status 200, :length -1, :body "{\"coord\":{\"lon\":-0.13,\"lat\":51.51},\"weather\":[{\"id\":300,\"main\":\"Drizzle\",\"description\":\"light intensity drizzle\",\"icon\":\"09d\"}],\"base\":\"stations\",\"main\":{\"temp\":280.32,\"pressure\":1012,\"humidity\":81,\"temp_min\":279.15,\"temp_max\":281.15},\"visibility\":10000,\"wind\":{\"speed\":4.1,\"deg\":80},\"clouds\":{\"all\":90},\"dt\":1485789600,\"sys\":{\"type\":1,\"id\":5091,\"message\":0.0103,\"country\":\"GB\",\"sunrise\":1485762037,\"sunset\":1485794875},\"id\":2643743,\"name\":\"London\",\"cod\":200}", :trace-redirects []}

;;This function takes a map m to pull values out and another map val-map 
;;which tells it which values to pull and what to assign them to.
(defn pull-values [m val-map]
  (into {} (for [[k v] val-map]
             [k (get-in m (if (sequential? v) v [v]))])))

;;pull data from the given url
(defn weather_data [url]
  (-> url
      client/get
      :body
      json/read-str
      (pull-values {:name ["name"]
                    :temp ["main" "temp"]
                    :humidity ["main" "humidity"]
                    :temp-min ["main" "temp_min"]
                    :temp-max ["main" "temp_max"]
                    :conditions ["weather" 0 "main"]})))


(def weather_data_ "https://samples.openweathermap.org/data/2.5/weather?q=London,uk&appid=b6907d289e10d714a6e88b30761fae22")
;; => {:name "London", :temp 280.32, :humidity 81, :temp-min 279.15, :temp-max 281.15, :conditions "Drizzle"}


(defn weather-to-str [w]
  (str "City: " (:name w) " \n " 
       "Temperature: " (:temp w) " F " "\n" 
       "Condition: " (:conditions w) "\n"
       "Min: "  (:temp-min w) " F " "\n" 
       "Max: " (:temp-max w) " F" "\n"
       "Humidity: " (:humidity w)))


(defn post-to-slack [url msg]
  (client/post url {:body (json/write-str msg)
                    :content-type :json}))


(defn weather-to-slack [url]
  (let [weather (-> url
                    weather_data
                    weather-to-str)]
    (post-to-slack hook-url {:text weather})))

(weather-to-slack weather_data_)