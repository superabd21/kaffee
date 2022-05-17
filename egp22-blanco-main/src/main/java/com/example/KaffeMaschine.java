package com.example;


import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class KaffeMaschine extends AbstractBehavior<KaffeMaschine.Request>
{
    private Integer vorrat;

    public interface Request {}
    public static final class Checkvorrat implements Request
    {
        public ActorRef<Kaffeetrinkende.Response> trinkend;
        public ActorRef<Loadbalancer.Request> sender;
        public Checkvorrat ( ActorRef<Loadbalancer.Request> sender , ActorRef<Kaffeetrinkende.Response> trinkend){
            this.sender = sender;
            this.trinkend=trinkend;
        }
    }
    public static final class KaffeeStellen implements Request
    {
        public  ActorRef <Kaffeetrinkende.Response> sender;
        public KaffeeStellen(ActorRef <Kaffeetrinkende.Response> sender)
        {
            this.sender = sender;
        }
    }


    public static Behavior<Request> create(int vorrat)
    {
        return Behaviors.setup(context -> new KaffeMaschine(context, vorrat));
    }

    private KaffeMaschine(ActorContext<Request> context, int vorrat)
    {
        super(context);
        this.vorrat = vorrat;
    }
    @Override
    public Receive<Request> createReceive()
    {
        return newReceiveBuilder()
                .onMessage(Checkvorrat.class, this::onCheckvorrat)
                .onMessage(KaffeeStellen.class, this::onKaffeeStellen)
                .build();
    }
    // check for 3 mashine !!
    private Behavior<Request> onCheckvorrat(Checkvorrat request)
    {
        getContext().getLog().info("Got a check vorrat Anfrage von{}  {})!", request.sender.path(),vorrat);
        request.sender.tell(new Loadbalancer.VorratStand(this.getContext().getSelf(), vorrat , request.trinkend));
        return this;
    }


    private Behavior<Request> onKaffeeStellen(KaffeeStellen kaffeeStellen)
    {
        getContext().getLog().info("kaffee wurde bestelt from {} mit Vorrat {}!", kaffeeStellen.sender, vorrat);
        if(vorrat > 0)
        {
        vorrat--;
        kaffeeStellen.sender.tell(new Kaffeetrinkende.KaffeeNehmen(kaffeeStellen.sender));
        }

        return this;
    }




}

