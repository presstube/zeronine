(ns zeronine.core
  (:require [reagent.core :as r :refer [atom]]
            [cljs.core.async :refer [<! chan sliding-buffer put! close! timeout]])
  (:require-macros
    [cljs.core.async.macros :refer [go-loop go]]))

(comment

  hellooooo
  I am a comment and I'm soooo lovely

  )

(enable-console-print!)

; should all these defs at the top just be part of the app-state atom?
; feels the same as the 'curse of var' from js:
; unportable config/state just hanging out at the top

(def spacing-x 140)
(def spacing-y 120)
(def dot-size 100)

(defn fps-to-millis [fps]
  (/ 1000 fps))

(def tick-loop-interval (fps-to-millis 100))
(def playhead-loop-interval (fps-to-millis 1.435))

;                    0       1       2        3         4         5         6         7         8         9
;                    nil     solo    duo      triad     quad      pent      hex       sept      oct       non
(def key-positions [[[0 0 0] [0 0 1] [-1 0 1] [-1 -1 1] [-1 -1 1] [-1 -1 1] [-2 -2 1] [-2 -2 1] [-2 -2 1] [-2 -2 1]]
                    [nil     [0 0 0] [1 0 1]  [1 -1 1]  [1 -1 1]  [1 -1 1]  [2 -2 1]  [2 -2 1]  [2 -2 1]  [2 -2 1]]
                    [nil     nil     [1 0 0]  [0 1 1]   [1 1 1]   [1 1 1]   [2 2 1]   [2 2 1]   [2 2 1]   [2 2 1]]
                    [nil     nil     nil      [0 1 0]   [-1 1 1]  [-1 1 1]  [-2 2 1]  [-2 2 1]  [-2 2 1]  [-2 2 1]]
                    [nil     nil     nil      nil       [-1 1 0]   [0 0 1]   [-1 0 1]  [-1 -1 1] [-1 -1 1] [-1 -1 1]]
                    [nil     nil     nil      nil       nil       [0 0 0]   [1 0 1]   [1 -1 1]  [1 -1 1]  [1 -1 1]]
                    [nil     nil     nil      nil       nil       nil       [1 0 0]   [0 1 1]   [1 1 1]   [1 1 1]]
                    [nil     nil     nil      nil       nil       nil       nil       [0 1 0]   [-1 1 1]  [-1 1 1]]
                    [nil     nil     nil      nil       nil       nil       nil       nil       [-1 1 0]   [0 0 1]]])

;(def step-sequence  [0 1 2 3 4 5 6 7 8 9])
;(def step-sequence  [0 1 2 3 4 5 6 7 8 9 8 7 6 5 4 3 2 1])
;(def step-sequence  [0 1 2 3 4 5 6 7 8 9 8 7 6 5 4 3 2 1
;                     0 1 2 3 4 5 6 7 8 7 6 5 4 3 2 1
;                     0 1 2 3 4 5 6 7 6 5 4 3 2 1
;                     0 1 2 3 4 5 6 5 4 3 2 1
;                     0 1 2 3 4 5 4 3 2 1
;                     0 1 2 3 4 3 2 1
;                     0 1 2 3 2 1
;                     0 1 2 1
;                     0 1
;                     0])
(def step-sequence  [
                     0
                     0 1
                     0 1 2 1
                     0 1 2 3 2 1
                     0 1 2 3 4 3 2 1
                     0 1 2 3 4 5 4 3 2 1
                     0 1 2 3 4 5 6 5 4 3 2 1
                     0 1 2 3 4 5 6 7 6 5 4 3 2 1
                     0 1 2 3 4 5 6 7 8 7 6 5 4 3 2 1
                     0 1 2 3 4 5 6 7 8 9 8 7 6 5 4 3 2 1
                     0 1 2 3 4 5 6 7 8 7 6 5 4 3 2 1
                     0 1 2 3 4 5 6 7 6 5 4 3 2 1
                     0 1 2 3 4 5 6 5 4 3 2 1
                     0 1 2 3 4 5 4 3 2 1
                     0 1 2 3 4 3 2 1
                     0 1 2 3 2 1
                     0 1 2 1
                     0 1
                     ])

(defonce app-state (atom {:step-index        0
                          :running           true
                          :wrapping          true
                          :target-positions  [nil nil nil nil nil nil nil nil nil]
                          :current-positions [nil nil nil nil nil nil nil nil nil]
                          :forces            [[0 0 0] [0 0 0] [0 0 0] [0 0 0] [0 0 0] [0 0 0] [0 0 0] [0 0 0] [0 0 0]]
                          }))








; ACTIONS

(defn move-step-index [amount-to-move]
  (swap! app-state
         (fn [{:keys [step-index wrapping] :as state}]
           (let [last-step-index (- (count step-sequence) 1)
                 new-step-index (+ step-index amount-to-move)
                 step-index-for-swap (cond
                                       (< new-step-index 0) (if wrapping last-step-index 0)
                                       (> new-step-index last-step-index) (if wrapping 0 last-step-index)
                                       :else new-step-index)
                 position-index (get step-sequence step-index-for-swap)]
             (-> state
                 (assoc :step-index step-index-for-swap)
                 (assoc :position-index position-index)
                 (assoc :target-positions
                        (into []
                              (map (fn [dot] (get dot position-index)) key-positions))))))))

(defn toggle-running []
  (swap! app-state
         (fn [state]
           (-> state
               (assoc :running (not (:running state)))))))

; CTRL

(defn on-mouse-down [e]
  (let [{:keys [current-positions] :as state} @app-state]
    (println (str "current-positions: " current-positions))
    (println "mouse down")
    ))

(defn on-mouse-up [e]
  (println "mouse up"))

(defn on-key-down [e]
  (let [keyCode (.-keyCode e)]
    (case keyCode
      32 (toggle-running)                                   ; space
      37 (move-step-index -1)                               ; left
      39 (move-step-index 1)                                ;right
      (println (str keyCode " is not recognized key")))))















; VIEW COMPONENTS

(defn dot [index dot-position]
  (let [[x y a] dot-position]
    (if dot-position
      [:div {:key index
             :style {:position "absolute"
                     :width 0
                     :height 0
                     :left (* x spacing-x)
                     :top (* y spacing-y)
                     :opacity a}
             }
       [:div {:style   {:position         "relative"
                        :top              (- (/ spacing-y 2))
                        :left             (- (/ spacing-x 2))
                        :width            dot-size
                        :height           dot-size
                        :background-color "#ccc"
                        ;:background-color (if (= (mod index 2) 0)
                        ;                    "#ccc"
                        ;                    "#111")
                        :display          "flex"
                        :justify-content  "center"
                        :alignItems       "center"
                        :font-size        30
                        ;:color "white"
                        :color            "rgba(0, 0, 0, 0.2)"
                        :font-family      "\"Lucida Console\", Monaco, monospace"
                        :border-style     "solid"
                        :border-width     8
                        :border-color     "rgba(0, 0, 0, 0.0)"
                        :border-radius    dot-size
                        :box-shadow       "0px 20px 20px rgba(0, 0, 0, 0.1)"
                        }
              :onClick (fn [e]
                         (.preventDefault e)
                         (.stopPropagation e)
                         (println (str "dot click " dot-position)))}
        ;(inc index)
        ]])))

(defn app []
  (let [{:keys [current-positions]} @app-state
        dot-divs (map-indexed dot (reverse current-positions))
        ]
    [:div {:style       {:position         "relative"
                         :width            (.-innerWidth js/window)
                         :height           (.-innerHeight js/window)
                         :background-color "#222"
                         }
           :onMouseDown on-mouse-down
           :onMouseUp   on-mouse-up
           }
     [:div {:style {:position "relative"
                    :height 0
                    :width 0
                    :left (/ (.-innerWidth js/window) 2)
                    :top (/ (.-innerHeight js/window) 2)}}
      dot-divs]]))

(r/render-component [app] (. js/document (getElementById "app")))








; TICK LOGIC

(defn apply-force [force cval tval]
  (let [diff (- tval cval)
        spring 0.005
        friction 0.95
        spring 0.1
        friction 0.8
        new-force (+ force (* diff spring))                 ; spring
        new-force (* new-force friction)]                   ;friction
    new-force))

(defn update-forces-before-apply [{:keys [target-positions current-positions forces] :as state}]
  (-> state
      (assoc :forces
             (into []
                   (map-indexed
                     (fn [index force]
                       (let [cp (get current-positions index)
                             tp (get target-positions index)]
                         [(apply-force (get force 0) (get cp 0) (get tp 0))
                          (apply-force (get force 1) (get cp 1) (get tp 1))
                          (apply-force (get force 2) (get cp 2) (get tp 2))]))
                     forces)))))

(defn apply-forces [{:keys [current-positions forces] :as state}]
  (-> state
      (assoc :current-positions
             (into []
                   (map-indexed
                     (fn [index current-pos]
                       (let [force (get forces index)]
                         [(+ (get current-pos 0) (get force 0))
                          (+ (get current-pos 1) (get force 1))
                          (+ (get current-pos 2) (get force 2))]))
                     current-positions)))))

(defn update-forces-after-apply [{:keys [forces] :as state}]
  (-> state
      (assoc :forces
             (into []
                   (map
                     (fn [force]
                       (let [drag 0.98]
                         [(* (get force 0) drag)
                          (* (get force 1) drag)
                          (* (get force 2) drag)]))
                     forces)))))

(defn get-jitter-amount []
  (* (- (rand) (rand)) 0.005))

(defn jitter-position [[x y a :as position]]
  (if (nil? position)
    nil
    [(+ x (get-jitter-amount))
     (+ y (get-jitter-amount))
     a]))

(defn jitter-current-positions [{:keys [current-positions] :as state}]
  (assoc state :current-positions (into [] (map jitter-position current-positions))))

(defn nil-clamp-on-alpha [{:keys [current-positions] :as state}]
  (-> state
      (assoc :current-positions
             (into []
                   (map
                     (fn [[_ _ a :as cp]]
                       ;(println (str "a: " a))
                       (if (<= a 0)
                         nil
                         cp))
                     current-positions)))))

(defn step-world [app-state]
  ;(println "stepping world")
  (swap! app-state
         (fn [state]
           (-> state
               ; this could be rolled into `(force-tween-dots)`
               (update-forces-before-apply)
               (apply-forces)
               (update-forces-after-apply)
               ;

               (jitter-current-positions)
               (nil-clamp-on-alpha)))))








; INITIAL ONE-TIME SETUP

(defn tick-loop []
  (go
    (<! (timeout tick-loop-interval))
    (when (:running @app-state)
      (step-world app-state))
    (tick-loop)))

(defn playhead-loop []
  (go
    (<! (timeout playhead-loop-interval))
    (when (:running @app-state)
      (move-step-index 1))
    (playhead-loop)))

(defn add-global-key-listeners []
  (set! (.-onkeydown js/document) on-key-down))

(defonce init
         (do
           (println "kickoff!")
           (add-global-key-listeners)
           (tick-loop)
           (playhead-loop)))










; ON FIGWHEEL RELOAD

(defn on-js-reload []
  ;;; (println "reload")
  ;;; optionally touch your app-state to force rerendering depending on
  ;;; your application
  ;;; (swap! app-state update-in [:__figwheel_counter] inc)
  (add-global-key-listeners)
)