(ns untangled-todomvc.ui
  (:require [om.next :as om :refer-macros [defui]]
            [untangled.client.mutations :as mut]
            [om.dom :as dom]
            [untangled.client.logging :as log]))

(defn is-enter? [evt] (= 13 (.-keyCode evt)))

(defn validate-text-input [evt callback]
  (let [trimmted-text (clojure.string/trim (.. evt -target -value))]
    (when (and (is-enter? evt) (not (empty? trimmted-text)))
      (callback evt trimmted-text))))

(defui TodoItem
  static om/IQuery
  (query [_] [:id :text :completed :editing])
  static om/Ident
  (ident [_ props] [:todo/by-id (:id props)])
  Object
  (initLocalState [this]
    {:edit-text (:text (om/props this))})

  (componentDidUpdate [this prev-props _]
    (when (and (not (:editing prev-props)) (:editing (om/props this)))
      (let [input-field (js/ReactDOM.findDOMNode (.. this -refs -edit_field))
            input-field-length (.. input-field -value -length)]
        (.focus input-field)
        (.setSelectionRange input-field input-field-length input-field-length))))

  (render [this]
    (let [{:keys [id text completed editing]} (om/props this)
          edit-text (om/get-state this :edit-text)
          onDelete (om/get-computed this :onDelete)
          edit-transaction (fn [_ text]
                             (om/transact! this `[(todo/edit ~{:id id :text text})])
                             (mut/toggle! this :editing))
          validate-edit (fn [evt] (validate-text-input evt edit-transaction))]

      (dom/li #js {:className (cond-> ""
                                completed (str "completed")
                                editing (str " editing"))}
        (dom/div #js {:className "view"}
          (dom/input #js {:className "toggle"
                          :type      "checkbox"
                          :checked   completed
                          :onChange  #(om/transact! this `[(todo/toggle-complete ~{:id id}) :todos/num-completed])})
          (dom/label #js {:onDoubleClick #(mut/toggle! this :editing)} text)
          (dom/button #js {:className "destroy"
                           :onClick   #(onDelete id)}))
        (dom/input #js {:className "edit"
                        :value     edit-text
                        :onChange  #(om/update-state! this assoc :edit-text (.. % -target -value))
                        :ref       "edit_field"
                        :onKeyDown validate-edit})))))

(def ui-todo-item (om/factory TodoItem {:keyfn :id}))

(defui TodoList
  static om/IQuery
  (query [this] [{:todos (om/get-query TodoItem)}
                 :todos/num-completed])
  Object
  (render [this]
    (let [{:keys [todos todos/num-completed]} (om/props this)
          num-todos (count todos)
          delete-item (fn [item-id] (om/transact! this `[(todo/delete-item ~{:id item-id})]))
          all-completed? (= num-completed num-todos)]
      (dom/div nil
        (dom/section #js {:className "todoapp"}

          (.header this)

          (when (pos? num-todos)
            (dom/div nil
              (dom/section #js {:className "main"}
                (dom/input #js {:className "toggle-all"
                                :type      "checkbox"
                                :checked   all-completed?
                                :onClick   #(om/transact! this `[(todo/toggle-all ~{:all-completed? all-completed?})])})
                (dom/label #js {:htmlFor "toggle-all"} "Mark all as complete")
                (dom/ul #js {:className "todo-list"}
                  (map #(ui-todo-item (om/computed % {:onDelete delete-item})) todos)))

              (.filter-footer this))))

        (.footer-info this))))

  (header [this]
    (letfn [(add-item [evt text]
              (om/transact! this `[(todo/new-item ~{:text text})])
              (set! (.. evt -target -value) ""))
            (validate-new-item [evt]
              (validate-text-input evt add-item))]

      (dom/header #js {:className "header"}
        (dom/h1 nil "todos")
        (dom/input #js {:className   "new-todo"
                        :placeholder "What needs to be done?"
                        :autoFocus   true
                        :onKeyDown   validate-new-item}))))

  (filter-footer [this]
    (let [{:keys [todos todos/num-completed]} (om/props this)
          num-todos (count todos)]
      (dom/footer #js {:className "footer"}
        (dom/span #js {:className "todo-count"}
          (dom/strong nil (- num-todos num-completed)) " items left")
        (dom/ul #js {:className "filters"}
          (dom/li nil
            (dom/a #js {:className "selected" :href "#"} "All")
            (dom/a #js {:href "#"} "Active")
            (dom/a #js {:href "#"} "Completed")))
        (when (pos? num-completed)
          (dom/button #js {:className "clear-completed"
                           :onClick   #(om/transact! this `[(todo/clear-complete)])} "Clear Completed")))))

  (footer-info [this]
    (dom/footer #js {:className "info"}
      (dom/p nil "Double-click to edit a todo")
      (dom/p nil "Created by "
        (dom/a #js {:href   "http://www.thenavisway.com"
                    :target "_blank"} "NAVIS"))
      (dom/p nil "Part of "
        (dom/a #js {:href   "http://todomvc.com"
                    :target "_blank"} "TodoMVC")))))
