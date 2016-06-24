(ns battlebots.utils.arena
  (:require [battlebots.constants.arena :refer [arena-key]]))

(defn empty-arena
  "returns an empty arena"
  [dimx dimy]
  (vec (repeat dimx (vec (repeat dimy (:open arena-key))))))

(defn get-item
  "gets an item based off of given coords"
  [coords arena]
  (get-in arena coords))

(defn get-arena-dimensions
  "returns the dimensions of a given arena (NOTE: Not 0 based)"
  [arena]
  (let [x (count arena)
        y ((comp count first) arena)]
    [x y]))

(defn get-arena-row-cell-length
  "Similar to arena/get-arena-dimensions except that it is zero based"
  [arena]
  (map dec (get-arena-dimensions arena)))

(defn wrap-coords
  "wraps the coords around to the other side of the arena"
  [[c-x c-y] [d-x d-y]]
  (let [x (cond
            (< c-x 0) (if (> (Math/abs c-x) d-x)
                        (- d-x (mod (Math/abs c-x) d-x))
                        (+ d-x c-x))
            (> c-x d-x) (mod c-x d-x)
           :else c-x)
        y (cond
            (< c-y 0) (if (> (Math/abs c-y) d-y)
                        (- d-y (mod (Math/abs c-y) d-y))
                        (+ d-y c-y))
            (> c-y d-y) (mod c-y d-y)
            :else c-y)]
    [x y]))

(defn incx
  [x]
  (fn [v] (+ x v)))

(defn adjust-coords
  "Returns a new set of coords based off of an applied direction.

  0 1 2
  7   3
  6 5 4

  "
  ([coords direction dimensions] (adjust-coords coords direction dimensions 1))
  ([coords direction dimensions steps]
   (let [
         updater (cond
                   (= direction 0) [(incx (- steps)) (incx (- steps))]
                   (= direction 1) [(incx (- steps)) identity]
                   (= direction 2) [(incx (- steps)) (incx steps)]
                   (= direction 3) [identity (incx steps)]
                   (= direction 4) [(incx steps) (incx steps)]
                   (= direction 5) [(incx steps) identity]
                   (= direction 6) [(incx steps) (incx (- steps))]
                   (= direction 7) [identity (incx (- steps))]
                   :else [identity identity])
         coords (map #(%1 %2) updater coords)]
     (wrap-coords coords dimensions))))

(defn draw-line
  "Draw a line from x1,y1 to x2,y2 using Bresenham's Algorithm"
  [x1 y1 x2 y2]
  (let [dist-x (Math/abs (- x1 x2))
        dist-y (Math/abs (- y1 y2))
        steep (> dist-y dist-x)]
    (let [[x1 y1 x2 y2] (if steep [y1 x1 y2 x2] [x1 y1 x2 y2])]
      (let [[x1 y1 x2 y2] (if (> x1 x2) [x2 y2 x1 y1] [x1 y1 x2 y2])]
        (let  [delta-x (- x2 x1)
               delta-y (Math/abs (- y1 y2))
               y-step (if (< y1 y2) 1 -1)]
          (loop [x x1
                 y y1
                 error (Math/floor (/ delta-x 2))
                 res []]
            (let [pt (if steep
                      [(int x) (int y)]
                      [(int y) (int x)])]
              (if (>= x x2)
                (conj res [x2 y2])
                ; Rather then rebind error, test that it is less than delta-y not 0
                (if (< error delta-y)
                  (recur (inc x) (+ y y-step) (+ error (- delta-x delta-y)) (conj res pt))
                  (recur (inc x) y            (- error delta-y) (conj res pt)))))))))))


(defn pretty-print-arena
  [arena]
  (doseq [a (apply map vector arena)] (println (map :display a))))

(defn get-arena-area
  [arena [posx posy] radius]
  (let [[xdim ydim] (get-arena-dimensions arena)
        [xmax ymax] (map dec [xdim ydim])
        maxradius (int (/ (dec xdim) 2))
        nradius (if (> (inc (* 2 radius)) xdim)
                  maxradius
                  radius)
        [x1 y1] (adjust-coords [posx posy] 0 [xmax ymax] nradius)
        [x2 y2] (adjust-coords [posx posy] 4 [xmax ymax] nradius)
        ;; now take these coords and copy the ranges of the arena vectors
        ;; swapping as needed
        columns (if (> x2 x1)
                  (subvec arena x1 (inc x2))
                  (vec (concat (subvec arena (inc x1) xdim) (subvec arena 0 (inc x2)))))
        area (map (fn [col]
                    (if (> y2 y1)
                      (subvec col y1 (inc y2))
                      (vec (concat (subvec col (inc y1) ydim) (subvec col 0 (inc y2))))
                      )) columns)]
    (comment (println "xdim " xdim)
             (println "ydim " ydim)
             (println "xmax " xmax)
             (println "ymax " ymax)
             (println "maxradius " maxradius)
             (println "nradius " nradius)
             (println "x1 " x1)
             (println "y1 " y1)
             (println "x2 " x2)
             (println "y2 " y2)
             (println "columns ")) (pretty-print-arena columns)
    area))

(defn get-wrapped-area-in-range
  "given an arena, a position, and fov radius return an arena subset within range"
  [arena [posx posy] radius]
  ;; find corners
  (let [[xdim ydim] (get-arena-dimensions arena)
        [x1 y1] (adjust-coords [posx posy] 0 [xdim ydim] radius)
        [x2 y2] (adjust-coords [posx posy] 4 [xdim ydim] radius)
        tpose-arena (apply map vector arena)
        diameter (* 2 radius)
        rangex (if (>= (inc diameter) xdim)
                 xdim
                 (inc diameter))
        rangey (if (>= (inc diameter) ydim)
                 ydim
                 (inc diameter))
        ;; if y1 > y2 prepend tail vectors to head
        arenay (if (> y1 y2) (conj (subvec tpose-arena (inc y1))
                                   (take y2 tpose-arena)))

        ]
    (let [dims (get-arena-dimensions arena)
          x-bound (- (get dims 0) 1)
          y-bound (- (get dims 1) 1)
          x1 (if (> radius posx)
               (- posx (mod radius posx))
               (- posx radius))
          x2 (if (> (+ posx radius) x-bound)
               (+ posx (mod (+ posx radius) x-bound))
               (+ posx radius))
          y1 (if (> radius posy)
               (- posy (mod radius posy))
               (- posy radius))
          y2 (if (> (+ posy radius) y-bound)
               (+ posy (mod (+ posy radius)) y-bound)
               (+ posy radius))]
      (map #(subvec % y1 y2) (subvec arena x1 x2)))))

(defn test-arena
  [dimx dimy]
  (vec (map-indexed
        (fn [colidx col]
          (vec (map-indexed
                (fn [cellidx cell] {:display (str colidx cellidx)}) col)))
        (empty-arena dimx dimy))))
