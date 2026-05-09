package br.ufs.dcomp.metrics;

import java.util.ArrayList;
import java.util.List;

public class Metrics {
  public CPU cpu = new CPU();
  public Memory memory = new Memory();
  public List<Disk> disks = new ArrayList<>();
  public List<Mount> mounts = new ArrayList<>();
  public List<NetIf> netIfs = new ArrayList<>();
  public OSInfo os = new OSInfo();

  public static class CPU {
    public String model;
    public int logicalProcessorCount;
    public int physicalPackageCount;
    public double systemLoad; // percentage
  }

  public static class Memory {
    public long total;
    public long available;
    public long used;
  }

  public static class Disk {
    public String name;
    public String model;
    public long size;
    public long reads;
    public long writes;
  }

  public static class Mount {
    public String name;
    public String mountPoint;
    public long totalSpace;
    public long freeSpace;
  }

  public static class NetIf {
    public String name;
    public String displayName;
    public String mac;
    public long rxBytes;
    public long txBytes;
  }

  public static class OSInfo {
    public String family;
    public String version;
    public String name;
  }
}
