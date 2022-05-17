package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class Kaffeetrinkende extends AbstractBehavior<Kaffeetrinkende.Response>
{

    private final ActorRef<Loadbalancer.Request> loadbalacncer;
    private final ActorRef <Kaffeekasse.Request> kaffeekasse;
    private static ActorRef<KaffeMaschine.Request> kaffeMaschine;

    public interface Response {}
    public static final class Abholbar implements Response
    {
        public Abholbar(ActorRef<KaffeMaschine.Request> kaffeeMaschineRef)
        {
            kaffeMaschine = kaffeeMaschineRef;
        }
    }
    public static final class NichtAbholbar implements Response
    {
        ActorRef<Kaffeetrinkende.Response> sender;
        public NichtAbholbar(ActorRef<Kaffeetrinkende.Response> sender) {
            this.sender=sender;
        }
    }
    public static final class Fail implements Response {}
    public static final class KaffeeNehmen implements Response
    {
        public ActorRef<Kaffeetrinkende.Response> sender;
        public KaffeeNehmen(ActorRef<Kaffeetrinkende.Response> sender)
        {
            this.sender=sender;
        }
    }
    public static final class Aufgeladen implements Response
    {
       public ActorRef<Kaffeetrinkende.Response> sender;
        public Aufgeladen(ActorRef<Kaffeetrinkende.Response> sender) {this.sender=sender;}
    }

    public static Behavior<Response> create(ActorRef<Loadbalancer.Request> loadbalancer, ActorRef<Kaffeekasse.Request> kaffeekasse)
    {return Behaviors.setup(context -> new Kaffeetrinkende(context, loadbalancer, kaffeekasse));}


    private Kaffeetrinkende(ActorContext<Response> context , ActorRef<Loadbalancer.Request> loadbalancer, ActorRef<Kaffeekasse.Request> kaffeekasse)
    {
        super(context);
        this.loadbalacncer =  loadbalancer;
        this.kaffeekasse = kaffeekasse;
        if (Math.random()< 0.5)
        {kaffeekasse.tell(new Kaffeekasse.Aufladen(this.getContext().getSelf()));}
        else
        {loadbalacncer.tell(new Loadbalancer.KaffeeAbholen(this.getContext().getSelf()));}
    }

    @Override
    public Receive<Response> createReceive()
    {
        return newReceiveBuilder()
                .onMessage(Abholbar.class, this::onAbholbar)
                .onMessage(NichtAbholbar.class, this::onNichtAbholbar)
                .onMessage(KaffeeNehmen.class, this::onKaffeeNehmen)
                .onMessage(Aufgeladen.class, this::onAufgeladen)
                .onMessage(Fail.class, this::onFail)
                .build();
    }
    private Behavior<Response> onAufgeladen(Aufgeladen command)
    {
        getContext().getLog().info("Konto wurde aufgeladen {}.",command.sender);
        if (Math.random()< 0.5)
        {kaffeekasse.tell(new Kaffeekasse.Aufladen(this.getContext().getSelf()));}
        else
        {loadbalacncer.tell(new Loadbalancer.KaffeeAbholen(this.getContext().getSelf()));}
        return this;
    }

    private Behavior<Response> onAbholbar(Abholbar command)
    {
        getContext().getLog().info("Guthaben reicht {}",kaffeMaschine);
        kaffeMaschine.tell(new KaffeMaschine.KaffeeStellen(this.getContext().getSelf()));
       return this;
    }
    private Behavior<Response> onNichtAbholbar(NichtAbholbar command)
    {
        getContext().getLog().info("Guthaben reicht nicht{}",command.sender);
        if (Math.random()< 0.5)
        {kaffeekasse.tell(new Kaffeekasse.Aufladen(this.getContext().getSelf()));}
        else
        {loadbalacncer.tell(new Loadbalancer.KaffeeAbholen(this.getContext().getSelf()));}
        return this;
    }

    private Behavior<Response> onKaffeeNehmen(KaffeeNehmen command)
    {
        getContext().getLog().info("Cafe wurde abgeholt {}",command.sender);
        if (Math.random()< 0.5)
        {kaffeekasse.tell(new Kaffeekasse.Aufladen(this.getContext().getSelf()));}
        else
        {loadbalacncer.tell(new Loadbalancer.KaffeeAbholen(this.getContext().getSelf()));}
        return this;
    }

    private Behavior<Response> onFail(Fail command)
    {
        getContext().getLog().info("Es gibt Kein kaffee mehr.");
        return Behaviors.stopped();
    }

}
