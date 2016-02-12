(ns untangled-todomvc.mutations
  (:require [untangled.client.mutations :as m]
            [untangled-todomvc.utils :as util]
            [om.next :as om]
            [untangled.client.logging :as log]))

(defmethod m/mutate 'todo/new-item [{:keys [state]} _ {:keys [text]}]
  {:action (fn []
             (let [id (om/tempid)]
               (swap! state #(-> %
                              (update :todos (fn [todos] ((fnil conj []) todos [:todo/by-id id])))
                              (assoc-in [:todo/by-id id] {:id id :text text})))
               (util/update-storage! #(conj % {:id id :text text :completed false}))))})

(defmethod m/mutate 'todo/toggle-complete [{:keys [state]} _ {:keys [id]}]
  {:action (fn []
             (swap! state (fn [st] (update-in st [:todo/by-id id :completed] #(not %))))
             (swap! state (fn [st] (update st :todos/num-completed
                                     (if (get-in st [:todo/by-id id :completed]) inc dec)))))})

(defmethod m/mutate 'todo/edit [{:keys [state]} _ {:keys [id text]}]
  {:action (fn []
             (swap! state assoc-in [:todo/by-id id :text] text))})

(defmethod m/mutate 'todo/delete-item [{:keys [state]} _ {:keys [id]}]
  {:action (fn []
             (letfn [(remove-item [todos] (vec (remove #(= id (second %)) todos)))]

               (when (get-in @state [:todo/by-id id :completed])
                 (swap! state update :todos/num-completed dec))

               (swap! state #(-> %
                              (update :todos remove-item)
                              (update :todo/by-id dissoc id)))))})

(defmethod m/mutate 'todo/toggle-all [{:keys [state]} _ {:keys [all-completed?]}]
  {:action (fn []
             (letfn [(set-completed [val todos]
                       (into {} (map (fn [[k v]] [k (assoc v :completed val)]) todos)))]

               (if all-completed?
                 (swap! state #(-> %
                                (assoc :todos/num-completed 0)
                                (update :todo/by-id (partial set-completed false))))
                 (swap! state #(-> %
                                (assoc :todos/num-completed (count (:todos @state)))
                                (update :todo/by-id (partial set-completed true)))))))})

(defmethod m/mutate 'todo/clear-complete [{:keys [state]} _ _]
  {:action (fn []
             (let [todos (vals (get @state :todo/by-id))
                   completed-todo-ids (set (keep #(when (:completed %) (:id %)) todos))]

               (swap! state assoc :todos/num-completed 0)
               (swap! state (fn [st]
                              (-> st
                                (update :todos
                                  (fn [todos] (vec (remove #(contains? completed-todo-ids (second %)) todos))))
                                (update :todo/by-id
                                  (fn [todos] (into {}
                                                (remove (fn [[k _]] (contains? completed-todo-ids k)) todos)))))))))})
