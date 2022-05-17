package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

public class KaffeeMain extends AbstractBehavior<KaffeeMain.StartMessage>
{

    public static class StartMessage {}


    public static Behavior<StartMessage> create(){
        return Behaviors.setup(KaffeeMain::new);
    }

    private KaffeeMain(ActorContext<StartMessage> context){
        super(context);
    }

    @Override
    public Receive<StartMessage> createReceive()
    {return newReceiveBuilder().onMessage(StartMessage.class, this::onStartMessage).build();}

    public Behavior<StartMessage> onStartMessage (StartMessage command)
    {
        ActorRef<KaffeMaschine.Request> kaffeemaschine1 = getContext().spawn(KaffeMaschine.create(1), "kaffeemaschine1");
        ActorRef<KaffeMaschine.Request> kaffeemaschine2 = getContext().spawn(KaffeMaschine.create(1), "kaffeemaschine2");
        ActorRef<KaffeMaschine.Request> kaffeemaschine3 = getContext().spawn(KaffeMaschine.create(1), "kaffeemaschine3");
        ActorRef<Kaffeekasse.Request> kaffeekasse = getContext().spawn(Kaffeekasse.create(), "kaffeekasse");
        ActorRef<Loadbalancer.Request> loadbalancer = getContext().spawn(Loadbalancer.create(kaffeekasse),"loadbalancer");
        ActorRef<Kaffeetrinkende.Response> kaffeetrinkende1 = getContext().spawn(Kaffeetrinkende.create(loadbalancer,kaffeekasse),"kaffeetrinkende1");
        ActorRef<Kaffeetrinkende.Response> kaffeetrinkende2 = getContext().spawn(Kaffeetrinkende.create(loadbalancer, kaffeekasse),"kaffeetrinkende2");
        ActorRef<Kaffeetrinkende.Response> kaffeetrinkende3 = getContext().spawn(Kaffeetrinkende.create(loadbalancer, kaffeekasse),"kaffeetrinkende3");
       // ActorRef<Kaffeetrinkende.Response> kaffeetrinkende4 = getContext().spawn(Kaffeetrinkende.create(loadbalancer, kaffeekasse), "kaffeetrinkende4");
        Kaffeekasse.guthaben.put(kaffeetrinkende1, 0);
        Kaffeekasse.guthaben.put(kaffeetrinkende2, 0);
        Kaffeekasse.guthaben.put(kaffeetrinkende3, 0);
        //Kaffeekasse.guthaben.put(kaffeetrinkende4, 0);
        Loadbalancer.kaffeMaschinen.put(kaffeemaschine1, 1);
        Loadbalancer.kaffeMaschinen.put(kaffeemaschine2, 1);
        Loadbalancer.kaffeMaschinen.put(kaffeemaschine3, 1);
        return this;
    }
}
