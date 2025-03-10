/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.ethereum.eth.manager.task;

import static org.assertj.core.api.Assertions.assertThat;

import org.hyperledger.besu.ethereum.eth.manager.EthContext;
import org.hyperledger.besu.ethereum.eth.manager.EthProtocolManager;
import org.hyperledger.besu.ethereum.eth.manager.EthProtocolManagerTestBuilder;
import org.hyperledger.besu.ethereum.eth.manager.EthProtocolManagerTestUtil;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.hyperledger.besu.plugin.services.MetricsSystem;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WaitForPeerTaskTest {
  private EthProtocolManager ethProtocolManager;
  private EthContext ethContext;
  private final MetricsSystem metricsSystem = new NoOpMetricsSystem();

  @BeforeEach
  public void setupTest() {
    ethProtocolManager = EthProtocolManagerTestBuilder.builder().build();
    ethContext = ethProtocolManager.ethContext();
  }

  @Test
  public void completesWhenPeerConnects() throws ExecutionException, InterruptedException {
    // Execute task and wait for response
    final AtomicBoolean successful = new AtomicBoolean(false);
    final EthTask<Void> task = WaitForPeerTask.create(ethContext, metricsSystem);
    final CompletableFuture<Void> future = task.run();
    future.whenComplete(
        (result, error) -> {
          if (error == null) {
            successful.compareAndSet(false, true);
          }
        });
    EthProtocolManagerTestUtil.createPeer(ethProtocolManager);
    assertThat(successful).isTrue();
  }

  @Test
  public void doesNotCompleteWhenNoPeerConnects() throws ExecutionException, InterruptedException {
    final AtomicBoolean successful = new AtomicBoolean(false);
    final EthTask<Void> task = WaitForPeerTask.create(ethContext, metricsSystem);
    final CompletableFuture<Void> future = task.run();
    future.whenComplete(
        (result, error) -> {
          if (error == null) {
            successful.compareAndSet(false, true);
          }
        });

    assertThat(successful).isFalse();
  }

  @Test
  public void cancel() throws ExecutionException, InterruptedException {
    // Execute task
    final EthTask<Void> task = WaitForPeerTask.create(ethContext, metricsSystem);
    final CompletableFuture<Void> future = task.run();

    assertThat(future.isDone()).isFalse();
    task.cancel();
    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isTrue();
    assertThat(task.run().isCancelled()).isTrue();
  }
}
