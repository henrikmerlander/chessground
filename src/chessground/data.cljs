(ns chessground.data
  "Representation and manipulation of the application data"
  (:require [chessground.common :as common :refer [pp]]
            [chessground.chess :as chess]))

(def defaults
  "Default state, overridable by user configuration"
  {:fen nil ; replaced by :chess by data/make
   :chess {} ; representation of a chess game
   :orientation "white"
   :dragging false ; currently dragging?
   :movable {:free true ; all moves are valid - board editor
             :color "both" ; color that can move. white or black or both
             :dests nil ; valid moves. {"a2" ["a3" "a4"] "b1" ["a3" "c3"]} | nil
             :drop-off "revert" ; when a piece is dropped outside the board. "revert" | "trash"
             :drag-center true ; whether to center the piece under the cursor on drag start
             :events {:after (fn [orig dest chess] nil) ; called after the moves has been played
                      }}})

(defn with-fen [state fen]
  (assoc state :chess (chess/make (or fen "start"))))

(defn make [js-config]
  (let [config (-> js-config
                   common/keywordize-keys
                   (common/keywordize-keys-in [:movable])
                   (common/keywordize-keys-in [:movable :events]))]
    (-> (common/deep-merge defaults config)
        (with-fen (:fen config))
        (dissoc :fen))))

(defn clear [state]
  (-> state
      (assoc :chess chess/clear)
      (assoc-in [:movable :dests] nil)))

(defn is-movable? [state key]
  "Piece on this square may be moved somewhere, if the validation allows it"
  (let [owner (chess/owner-color (:chess state) key)
        movable (:movable state)
        color (:color movable)]
    (and owner (or (= color "both") (= color owner)))))

(defn can-move? [state orig dest]
  "The piece on orig can definitely be moved to dest"
  (and (is-movable? state orig)
       (or (-> state :movable :free)
           (common/seq-contains? (get-in state [:movable :dests orig]) dest))))

(defn- update-chess-dests [state]
  (update-in state [:chess] chess/update-dests (-> state :movable :dests)))

(defn set-dests [state dests]
  (-> state
      (assoc-in [:movable :dests] dests)
      (assoc-in [:movable :free] false)
      update-chess-dests))

(defn set-color [state color]
  (if (common/seq-contains? (conj chess/colors "both" "none") color)
    (assoc-in state [:movable :color] color)
    state))

(defn set-orientation [prev next]
  (if (common/seq-contains? chess/colors next) next prev))

(defn toggle-orientation [prev]
  (set-orientation prev (if (= prev "white") "black" "white")))
