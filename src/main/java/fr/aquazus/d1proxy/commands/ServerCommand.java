package fr.aquazus.d1proxy.commands;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import fr.aquazus.d1proxy.Proxy;
import fr.aquazus.d1proxy.network.ProxyClient;
import lombok.Getter;
import simplenet.packet.Packet;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class ServerCommand implements Command
{

  @Getter
  private final String description = "[DEBUG] Crée un serveur HTTP pour envoyer/recevoir des paquets. A ne lancer qu'une fois.";
  private final Proxy proxy;

  private final int serverPort = 8769;

  public ServerCommand(Proxy proxy)
  {
    this.proxy = proxy;
  }

  @Override
  public void execute(ProxyClient proxyClient, String args)
  {
    try
    {
      HttpServer server = HttpServer.create(new InetSocketAddress(serverPort), 0);
      server.createContext("/send", new ServerCommand.ServerSendHandler(proxyClient));
      server.createContext("/receive", new ServerCommand.ServerReceiveHandler(proxyClient));
      server.setExecutor(null); // creates a default executor
      server.start();

      proxyClient.sendMessage("<b>Serveur HTTP démarré sur le port " + serverPort + "</b>");
      proxyClient.sendMessage("Endpoints `/send` et `/receive` écoutent le body reçu via les méthodes HTTP POST.");
    }
    catch (Exception error)
    {
      System.out.println("ERROR HTTP SERVER");
      System.out.println(error.toString());
      System.out.println("ERROR HTTP SERVER");
    }
  }

  public static String httpRequestBodyToString(InputStream reqBody)
  {
    try
    {
      InputStreamReader isr = new InputStreamReader(reqBody, "utf-8");
      BufferedReader br = new BufferedReader(isr);

      // From now on, the right way of moving from bytes to utf-8 characters:
      int b;
      StringBuilder buf = new StringBuilder(512);
      while ((b = br.read()) != -1)
      {
        buf.append((char) b);
      }

      br.close();
      isr.close();
      return buf.toString();
    }
    catch (Exception err)
    {
      System.out.println("ERROR IN httpRequestBodyToString");
      System.out.println(err.toString());
    }
    return "failedToReadRequestBody";
  }

  static class ServerSendHandler implements HttpHandler
  {
    private ProxyClient proxyClient;

    public ServerSendHandler(ProxyClient proxyClient)
    {
      this.proxyClient = proxyClient;
    }

    public void handle(HttpExchange t) throws IOException
    {
      if (t.getRequestMethod().equalsIgnoreCase("POST"))
      {
        String body = ServerCommand.httpRequestBodyToString(t.getRequestBody());
        System.out.println("HTTP SERVER COMMAND - SEND " + body);
        Packet.builder().putBytes(body.getBytes(StandardCharsets.UTF_8)).putByte(10).putByte(0).writeAndFlush(proxyClient.getServer());
        String response = "Sent " + body;
        t.sendResponseHeaders(200, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
      }
    }
  }

  static class ServerReceiveHandler implements HttpHandler
  {
    private ProxyClient proxyClient;

    public ServerReceiveHandler(ProxyClient proxyClient)
    {
      this.proxyClient = proxyClient;
    }

    public void handle(HttpExchange t) throws IOException
    {
      if (t.getRequestMethod().equalsIgnoreCase("POST"))
      {
        String body = ServerCommand.httpRequestBodyToString(t.getRequestBody());
        System.out.println("HTTP SERVER COMMAND - RECEIVE " + body);
        Packet.builder().putBytes(body.getBytes(StandardCharsets.UTF_8)).putByte(0).writeAndFlush(proxyClient.getClient());
        String response = "Received " + body;
        t.sendResponseHeaders(200, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
      }
    }
  }
}
