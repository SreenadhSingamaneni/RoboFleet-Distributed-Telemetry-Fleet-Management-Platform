package com.roboverse.fleet.application.port.out;

public interface ClusterLockPort {
    boolean tryAcquire(String lockName);
}

