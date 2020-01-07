package fr.aquazus.d1proxy.network;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import simplenet.packet.Packet;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class RpcWebSocketServer extends WebSocketServer
{
  private static final int serverPort = 8769;
  private List<ProxyClient> dofusClients;

  public RpcWebSocketServer()
  {
    super(new InetSocketAddress("127.0.0.1", serverPort));
  }

  public void registerDofusClients(List<ProxyClient> dofusClients)
  {
    this.dofusClients = dofusClients;
  }

  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake)
  {
    conn.send("hello"); //This method sends a message to the new client
    System.out.println("[RPC] New connection from " + conn.getRemoteSocketAddress());
  }

  @Override
  public void onClose(WebSocket conn, int code, String reason, boolean remote)
  {
    System.out.println("closed " + conn.getRemoteSocketAddress() + " with exit code " + code + " additional info: " + reason);
  }

  @Override
  public void onMessage(WebSocket conn, String message)
  {
    // Received a SEND command
    if (message.startsWith("send "))
    {
      String packet = message.substring(5);
      broadcastSendToDofusClients(packet);
      conn.send("sent " + packet);
    }
    // Received a RECEIVE command
    else if (message.startsWith("receive "))
    {
      String packet = message.substring(8);
      broadcastReceiveToDofusClients(packet);
      conn.send("received " + packet);
    }
  }

  @Override
  public void onError(WebSocket conn, Exception ex)
  {
    System.err.println("[RPC] An error occurred on connection " + conn.getRemoteSocketAddress() + ":" + ex);
  }

  @Override
  public void onStart()
  {
    System.out.println("[RPC] RPC server started successfully on ws://127.0.0.1:" + serverPort);
  }


  public void broadcastSentPacketToWebSocketClients(String packet)
  {
    this.broadcast("sniffer_sent "+packet);
  }

  public void broadcastReceivedPacketToWebSocketClients(String packet)
  {
    this.broadcast("sniffer_received "+packet);
  }

  public void broadcastSendToDofusClients(String packet)
  {
    System.out.println("[RPC] SEND " + packet);
    for (ProxyClient aClient : dofusClients)
      Packet.builder().putBytes(packet.getBytes(StandardCharsets.UTF_8)).putByte(10).putByte(0).writeAndFlush(aClient.getServer());
  }

  public void broadcastReceiveToDofusClients(String packet)
  {
    System.out.println("[RPC] RECEIVE " + packet);
    for (ProxyClient aClient : dofusClients)
      Packet.builder().putBytes(packet.getBytes(StandardCharsets.UTF_8)).putByte(0).writeAndFlush(aClient.getClient());
  }
}