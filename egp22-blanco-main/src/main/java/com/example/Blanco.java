package com.example;

import akka.Main;
import akka.actor.typed.ActorSystem;

import java.io.IOException;
public class Blanco
{

  public static void main(String[] args)
  {
    final ActorSystem<KaffeeMain.StartMessage> kaffeeMain = ActorSystem.create(KaffeeMain.create(), "main");

    kaffeeMain.tell(new KaffeeMain.StartMessage());

    try
    {
      System.out.println(">>> Press ENTER to exit <<<");
      System.in.read();
    }
    catch (IOException ignored) {}
    finally
    {
      kaffeeMain.terminate();
    }
  }
}
