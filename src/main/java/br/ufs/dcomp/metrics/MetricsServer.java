package br.ufs.dcomp.metrics;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class MetricsServer {
  private static final int PORT = 9001;

  public static void main(String[] args) throws Exception {
    try (DatagramSocket udpSocket = new DatagramSocket(PORT)) {
      byte[] buf = new byte[65535];
      System.out.println("Servidor ouvindo na porta 9001...");
      int n = 1;
      while (true) {
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        udpSocket.receive(packet);

        String json = new String(packet.getData(), 0, packet.getLength(), "UTF-8");

        System.out.printf("Coleta[%d] Recebida: %s\n", n, json);

        n++;

        byte[] ok = new String("ok!").getBytes();
        DatagramPacket okPacket =
            new DatagramPacket(ok, ok.length, packet.getAddress(), packet.getPort());

        udpSocket.send(okPacket);
      }
    }
  }
}
