package com.example;


import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.HashMap;
import java.util.Map;

public class Kaffeekasse extends AbstractBehavior<Kaffeekasse.Request> {


    public static Map <ActorRef<Kaffeetrinkende.Response>, Integer> guthaben = new HashMap<>();


    public Map<ActorRef<Kaffeetrinkende.Response>, Integer> getGuthaben() {
        return guthaben;
    }



    public interface Request {}
    public static final class Aufladen implements Request {
        public ActorRef<Kaffeetrinkende.Response> sender;
        public Aufladen(ActorRef<Kaffeetrinkende.Response> sender) {this.sender = sender;}
    }

    public static final class CheckGuthaben implements Request{
        public ActorRef<Kaffeetrinkende.Response> kaffeetrinkende;
        public ActorRef <Loadbalancer.Request> loadbalancer;
        public CheckGuthaben(ActorRef <Loadbalancer.Request> loadbalancer, ActorRef<Kaffeetrinkende.Response> kaffeetrinkende ){
            this.kaffeetrinkende = kaffeetrinkende;
            this.loadbalancer = loadbalancer;
        }
    }




    public static Behavior<Request> create () {
        return Behaviors.setup(context -> new Kaffeekasse(context));

    }

    private Kaffeekasse(ActorContext<Request> context){
        super(context);
    }

    @Override
    public Receive<Request> createReceive(){
      return newReceiveBuilder()
              .onMessage(Aufladen.class, this::onAufladen)
              .onMessage(CheckGuthaben.class, this::onCheckGuthaben)
              .build();

    }
    private Behavior<Request>onAufladen(Aufladen request){
        getContext().getLog().info("Aufladen Anfrage wurde erhalten {} {}", request.sender.path(), guthaben.get(request.sender));
        Integer newGuthaben = guthaben.get(request.sender)+1;
        guthaben.put(request.sender, newGuthaben);
        request.sender.tell(new Kaffeetrinkende.Aufgeladen(request.sender));
        return this;
    }

    private Behavior<Request> onCheckGuthaben(CheckGuthaben request){
        getContext().getLog().info("Guthaben wurde jetzt überpruft {}.", request.kaffeetrinkende);
        Integer aktuellGuthaben = guthaben.get(request.kaffeetrinkende);
        if(aktuellGuthaben > 0) {
            aktuellGuthaben--;
            getContext().getLog().info("Guthaben wurde reduiziert {} der akteuelguthaben ist:{}", request.kaffeetrinkende,aktuellGuthaben);

            guthaben.put(request.kaffeetrinkende, aktuellGuthaben);
            request.loadbalancer.tell(new Loadbalancer.GenugGuthaben(request.kaffeetrinkende));
        }
        else{
            request.loadbalancer.tell(new Loadbalancer.NichtGenugGuthaben(request.kaffeetrinkende));
        }
        return this;

    }


}
