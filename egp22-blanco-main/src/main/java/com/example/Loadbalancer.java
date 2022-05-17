package com.example;


import akka.actor.Actor;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import scala.Int;

import java.util.*;

public class Loadbalancer extends AbstractBehavior<Loadbalancer.Request>
{

    private final ActorRef<Kaffeekasse.Request> kaffeekasse;
    public static Map<ActorRef<KaffeMaschine.Request>, Integer> kaffeMaschinen = new HashMap<>();
    Map.Entry<ActorRef<KaffeMaschine.Request>, Integer> maxEntry;
    private static ActorRef<Kaffeetrinkende.Response> trinkende;
    private int callTellAbholbar = 0;

    public Map<ActorRef<KaffeMaschine.Request>, Integer> getKaffeMaschinen() {
        return kaffeMaschinen;
    }

    public interface Request {}

    public static final class GenugGuthaben implements Request
    {
        public ActorRef<Kaffeetrinkende.Response> kaffeetrinkende;

        public GenugGuthaben(ActorRef<Kaffeetrinkende.Response> kaffeetrinkende)
        {
            this.kaffeetrinkende = kaffeetrinkende;
        }
    }

    public static final class NichtGenugGuthaben implements Request
    {
        public ActorRef<Kaffeetrinkende.Response> sender;
        public NichtGenugGuthaben(ActorRef<Kaffeetrinkende.Response> sender) {
            this.sender = sender;
        }
    }

    public static final class KaffeeAbholen implements Request
    {
        public ActorRef<Kaffeetrinkende.Response> sender;
        public KaffeeAbholen(ActorRef<Kaffeetrinkende.Response> sender)
        {
//            trinkende = sender;
            this.sender = sender;
        }
    }

    public static final class VorratStand implements Request
    {
        public ActorRef<KaffeMaschine.Request> kaffeMaschine;
        public Integer vorrat;
        public ActorRef<Kaffeetrinkende.Response>sender;
        public VorratStand(ActorRef<KaffeMaschine.Request> kaffeMaschine, Integer vorrat , ActorRef<Kaffeetrinkende.Response>sender)
        {
            this.vorrat = vorrat;
            this.kaffeMaschine = kaffeMaschine;
            this.sender=sender;
        }
    }

    public static Behavior<Request> create(ActorRef<Kaffeekasse.Request> kaffeekasse)
    {
        return Behaviors.setup(context -> new Loadbalancer(context, kaffeekasse));
    }

    private Loadbalancer(ActorContext<Request> context, ActorRef<Kaffeekasse.Request> kaffeekasse)
    {
        super(context);
        this.kaffeekasse = kaffeekasse;
    }

    @Override
    public Receive<Request> createReceive()
    {
        return newReceiveBuilder()
                .onMessage(GenugGuthaben.class, this::onGenugGuthaben)
                .onMessage(NichtGenugGuthaben.class, this::onNichtGenug)
                .onMessage(KaffeeAbholen.class, this::onKaffeeAbholen)
                .onMessage(VorratStand.class, this::onVorratStand)
                .build();
    }

    private Behavior<Request> onVorratStand(VorratStand command)
    {
        kaffeMaschinen.put(command.kaffeMaschine, command.vorrat);
        if (maxEntry != null && command.kaffeMaschine.equals(maxEntry.getKey())){
            if (maxEntry.getValue().equals(command.vorrat)){
                kaffeMaschinen.put(command.kaffeMaschine, command.vorrat--);
            }
        }


        if (++callTellAbholbar % kaffeMaschinen.size() == 0)
        {
            for (Map.Entry<ActorRef<KaffeMaschine.Request>, Integer> entry : kaffeMaschinen.entrySet())
            {
                if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
                {
                    maxEntry = entry;
                }
            }
            if (maxEntry.getValue() == 0)
            {
                command.sender.tell(new Kaffeetrinkende.Fail());
            }
            else
            {

                command.sender.tell(new Kaffeetrinkende.Abholbar(maxEntry.getKey()));


            }
        }


//        if (++callTellAbholbar % kaffeMaschinen.size() == 0) {
//            Map.Entry<ActorRef<KaffeMaschine.Request>, Integer> kaffeeMaschineEntry = kaffeMaschinen.entrySet().stream()
//                    .max((t0, t1) -> Math.max(t0.getValue(), t1.getValue())).get();
//            if (kaffeeMaschineEntry.getValue() == 0) {
//                trinkende.tell(new Kaffeetrinkende.Fail());
//            }
//            trinkende.tell(new Kaffeetrinkende.Abholbar(kaffeeMaschineEntry.getKey()));
//            callTellAbholbar = 0;
//        }
        return this;
    }


 // check vorrat for 3 mashine
    private Behavior<Request> onGenugGuthaben(GenugGuthaben genugGuthaben)
    {
        getContext().getLog().info("Genug Guthaben {}.",genugGuthaben.kaffeetrinkende);
        kaffeMaschinen.keySet()
                .forEach(km -> km.tell(new KaffeMaschine.Checkvorrat(this.getContext().getSelf() ,genugGuthaben.kaffeetrinkende)));
        return this;
    }


    private Behavior<Request> onNichtGenug(NichtGenugGuthaben nichtGenugGuthaben)
    {
        getContext().getLog().info("Nicht genug Guthaben. {}", nichtGenugGuthaben.sender);
        nichtGenugGuthaben.sender.tell(new Kaffeetrinkende.NichtAbholbar(nichtGenugGuthaben.sender));
        return this;
    }

    private Behavior<Request> onKaffeeAbholen(KaffeeAbholen kaffeeAbholen)
    {
        getContext().getLog().info("Auftrag wurde entgegengenommen.{}",kaffeeAbholen.sender);

        kaffeekasse.tell(new Kaffeekasse.CheckGuthaben(this.getContext().getSelf(), kaffeeAbholen.sender));
        return this;

    }


}
