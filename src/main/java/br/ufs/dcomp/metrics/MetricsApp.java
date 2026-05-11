package br.ufs.dcomp.metrics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.List;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

public class MetricsApp {

  private static final String HOST = "ec2-32-192-187-159.compute-1.amazonaws.com";
  private static final int PORT = 9001;
  private static final int TIMEOUT = 5000; // ms

  public static void main(String[] args) throws IOException {
    SystemInfo si = new SystemInfo();
    HardwareAbstractionLayer hal = si.getHardware();
    OperatingSystem os = si.getOperatingSystem();

    Metrics metrics = new Metrics();

    // CPU
    CentralProcessor cpu = hal.getProcessor();
    CentralProcessor.ProcessorIdentifier pid = cpu.getProcessorIdentifier();
    metrics.cpu.model = pid.getName();
    metrics.cpu.logicalProcessorCount = cpu.getLogicalProcessorCount();
    metrics.cpu.physicalPackageCount = cpu.getPhysicalPackageCount();

    // Nova forma de medir uso de CPU (precisa de duas leituras)
    long[] prevTicks = cpu.getSystemCpuLoadTicks();
    try {
      Thread.sleep(500); // intervalo curto para amostragem
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    metrics.cpu.systemLoad = cpu.getSystemCpuLoadBetweenTicks(prevTicks) * 100.0;

    // Memória
    GlobalMemory memory = hal.getMemory();
    metrics.memory.total = memory.getTotal();
    metrics.memory.available = memory.getAvailable();
    metrics.memory.used = metrics.memory.total - metrics.memory.available;

    // Discos
    List<HWDiskStore> diskStores = hal.getDiskStores();
    for (HWDiskStore ds : diskStores) {
      Metrics.Disk d = new Metrics.Disk();
      d.name = ds.getName();
      d.model = ds.getModel();
      d.size = ds.getSize();
      d.reads = ds.getReads();
      d.writes = ds.getWrites();
      metrics.disks.add(d);
    }

    // File system
    FileSystem fs = os.getFileSystem();
    for (OSFileStore fsStore : fs.getFileStores()) {
      Metrics.Mount m = new Metrics.Mount();
      m.name = fsStore.getName();
      m.mountPoint = fsStore.getMount();
      m.totalSpace = fsStore.getTotalSpace();
      m.freeSpace = fsStore.getUsableSpace();
      metrics.mounts.add(m);
    }

    // Interfaces de rede
    List<NetworkIF> nifs = hal.getNetworkIFs();
    for (NetworkIF nif : nifs) {
      Metrics.NetIf ni = new Metrics.NetIf();
      ni.name = nif.getName();
      ni.displayName = nif.getDisplayName();
      ni.mac = nif.getMacaddr();
      nif.updateAttributes(); // atualiza contadores RX/TX
      ni.rxBytes = nif.getBytesRecv();
      ni.txBytes = nif.getBytesSent();
      metrics.netIfs.add(ni);
    }

    // Sistema operacional
    metrics.os.family = os.getFamily();
    metrics.os.version = os.getVersionInfo().getVersion();
    metrics.os.name = os.getVersionInfo().getCodeName();

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String json = gson.toJson(metrics);
    byte[] buf = json.getBytes();

    try (DatagramSocket udpSocket = new DatagramSocket()) {
      udpSocket.setSoTimeout(TIMEOUT);
      InetAddress address = InetAddress.getByName(HOST);

      DatagramPacket packet = new DatagramPacket(buf, buf.length, address, PORT);

      udpSocket.send(packet);
      System.out.printf("JSON Enviado para %s:%d\n", HOST, PORT);
      byte[] recvBuf = new byte[1024];
      DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);
      try {
        udpSocket.receive(recvPacket);
        String reply = new String(recvPacket.getData(), 0, recvPacket.getLength(), "UTF-8");

        System.out.printf(
            "Resposta de %s:%d → %s\n",
            recvPacket.getAddress().getHostAddress(), recvPacket.getPort(), reply);
      } catch (SocketTimeoutException e) {
        System.err.println("Timeout: nenhuma resposta em " + TIMEOUT + "ms");
      }
    }
  }
}
