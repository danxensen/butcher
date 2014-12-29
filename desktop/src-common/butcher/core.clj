(ns butcher.core
  (:require [play-clj.core :refer :all]
            [play-clj.g3d :refer :all]
            [play-clj.math :refer :all]
            [play-clj.ui :refer :all]
            [butcher.entity.core :as e])
  (:import [com.badlogic.gdx.graphics Texture$TextureFilter]
           [com.badlogic.gdx.graphics.g2d SpriteBatch]
           [com.badlogic.gdx.graphics.glutils FrameBuffer]))

(def manager (asset-manager))
(set-asset-manager! manager)

(def camera-y 5)
(def camera-z 5)

(defn update-screen!
  [screen entities]
  ;; move camera with to center on the box
  (doseq [{:keys [x z id]} entities]
    (when (= id :player)
      (position! screen x camera-y (+ z camera-z))))
  entities)

(def pixelate-factor 4)

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen
             :renderer (model-batch)
             :camera (doto (perspective 75 (game :width) (game :height))
                       (position! 0 camera-y camera-z)
                       (direction! 0 2 0)
                       (near! 0.1)
                       (far! 300))
             :fbo (doto (FrameBuffer. (pixmap-format :r-g-b-888)
                                      (/ (game :width) pixelate-factor)
                                      (/ (game :height) pixelate-factor)
                                      true)
                    (-> .getColorBufferTexture
                        (.setFilter
                         Texture$TextureFilter/Nearest
                         Texture$TextureFilter/Nearest)))
             :fbo-batch (SpriteBatch.))
    (conj (e/make-boxes 20) (e/player)))

  :on-render
  (fn [{:keys [fbo fbo-batch] :as screen} entities]
    (.begin fbo)
    (clear! 0 0 0 1)
    (let [entities
          (->> (for [entity entities]
                 ;; call entity render fns
                 (if (:on-render entity)
                   ((:on-render entity) entity entities)
                   entity))
               (render! screen)
               (update-screen! screen))]
      (.end fbo)
      (.begin fbo-batch)
      (.draw fbo-batch
             (.getColorBufferTexture fbo)
             0 0 (game :width) (game :height) 0 0 1 1)
      (.end fbo-batch)
      entities))
  
  :on-key-down
  (fn [screen entities]
    nil)

  :on-key-up
  (fn [screen entities]
    nil)

  :on-touch-down
  (fn [screen entities]
    nil)

  :on-touch-up
  (fn [screen entities]
    nil)
  
  :on-resize
  (fn [screen entities]
    (height! screen 600)))

(defscreen text-screen
  :on-show
  (fn [screen entities]
    (update! screen :camera (orthographic) :renderer (stage))
    (assoc (label "-" (color :white))
      :id :fps))

  :on-render
  (fn [screen entities]
    (->> (for [entity entities]
           (case (:id entity)
             :fps (doto entity
                    (label! :set-text
                            (str (game :fps))))
             entity))
         (render! screen)))

  :on-touch-down
  (fn [screen entities]
    nil)

  :on-touch-up
  (fn [screen entities]
    nil)
  
  :on-resize
  (fn [screen entities]
    (height! screen 300)))

(defgame butcher
  :on-create
  (fn [this]
    (set-screen! this main-screen text-screen)))
