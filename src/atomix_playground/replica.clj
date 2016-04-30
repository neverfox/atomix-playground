(ns atomix-playground.replica
  (:require
   [hara.component :refer [IComponent]]
   [taoensso.timbre :refer [refer-timbre]])
  (:import
   [io.atomix AtomixReplica]
   [io.atomix.catalyst.transport Address NettyTransport]
   [io.atomix.copycat.server.storage Storage]
   [java.net InetAddress]))

(refer-timbre)

(defrecord ReplicaComponent []
  IComponent
  (-start [{:keys [port mode nodes] :as self}]
    (info "-> Starting replica")
    (let [localhost     (-> (InetAddress/getLocalHost)
                            (.getHostName))
          local-address (Address. localhost port)
          storage       (get-in self [:storage :storage] (Storage.))
          transport     (get-in self [:transport :transport] (NettyTransport.))
          replica       (.. (AtomixReplica/builder local-address)
                            (withTransport transport)
                            (withStorage storage)
                            build)]
      (if nodes
        (let [cluster (map #(Address. %) nodes)]
          (condp = mode
            :bootstrap @(.bootstrap replica cluster)
            :join      @(.join replica cluster)))
        @(.bootstrap replica))
      (assoc self :replica replica)))
  (-stop [{:keys [replica] :as self}]
    (info "<- Stopping replica")
    @(.shutdown replica)
    @(.leave replica)
    (dissoc self :replica)))
