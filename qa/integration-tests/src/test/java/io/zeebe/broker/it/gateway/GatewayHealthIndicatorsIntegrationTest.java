/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.broker.it.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.zeebe.broker.it.gateway.GatewayHealthIndicatorsIntegrationTest.Config;
import io.zeebe.gateway.Gateway.Status;
import io.zeebe.gateway.impl.SpringGatewayBridge;
import io.zeebe.gateway.impl.broker.cluster.BrokerClusterState;
import io.zeebe.gateway.impl.probes.health.ClusterAwarenessHealthIndicator;
import io.zeebe.gateway.impl.probes.health.PartitionLeaderAwarenessHealthIndicator;
import io.zeebe.gateway.impl.probes.health.StartedHealthIndicator;
import io.zeebe.util.health.MemoryHealthIndicator;
import java.util.List;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Config.class)
public class GatewayHealthIndicatorsIntegrationTest {

  @Autowired MemoryHealthIndicator memoryHealthIndicator;

  @Autowired StartedHealthIndicator startedHealthIndicator;

  @Autowired ClusterAwarenessHealthIndicator clusterAwarenessHealthIndicator;

  @Autowired PartitionLeaderAwarenessHealthIndicator partitionLeaderAwarenessHealthIndicator;

  @Autowired SpringGatewayBridge springGatewayBridge;

  @After
  public void tearDown() {
    // reset suppliers
    springGatewayBridge.registerClusterStateSupplier(null);
    springGatewayBridge.registerGatewayStatusSupplier(null);
  }

  @Test
  public void shouldInitializeMemoryHealthIndicatorWithDefaults() {
    assertThat(memoryHealthIndicator.getThreshold()).isEqualTo(0.1, offset(0.001));
  }

  @Test
  public void shouldCreateGatewayStartedHealthIndicatorThatIsBackedBySpringGatewayBridge() {
    // precondition
    assertThat(startedHealthIndicator).isNotNull();
    assertThat(springGatewayBridge).isNotNull();

    // given
    final Supplier<Status> statusSupplier = () -> Status.SHUTDOWN;

    // when
    final Health actualHealthBeforeRegisteringStatusSupplier = startedHealthIndicator.health();
    springGatewayBridge.registerGatewayStatusSupplier(statusSupplier);
    final Health actualHealthAfterRegisteringStatusSupplier = startedHealthIndicator.health();

    // then
    assertThat(actualHealthBeforeRegisteringStatusSupplier.getStatus())
        .isSameAs(org.springframework.boot.actuate.health.Status.UNKNOWN);
    assertThat(actualHealthAfterRegisteringStatusSupplier.getStatus())
        .isSameAs(org.springframework.boot.actuate.health.Status.OUT_OF_SERVICE);
  }

  @Test
  public void
      shouldCreateGatewayClusterAwarenessHealthIndicatorThatIsBackedBySpringGatewayBridge() {
    // precondition
    assertThat(clusterAwarenessHealthIndicator).isNotNull();
    assertThat(springGatewayBridge).isNotNull();

    // given
    final BrokerClusterState mockClusterState = mock(BrokerClusterState.class);
    when(mockClusterState.getBrokers()).thenReturn(List.of(1));

    final Supplier<BrokerClusterState> stateSupplier = () -> mockClusterState;

    // when
    final Health actualHealthBeforeRegisteringStatusSupplier =
        clusterAwarenessHealthIndicator.health();
    springGatewayBridge.registerClusterStateSupplier(stateSupplier);
    final Health actualHealthAfterRegisteringStatusSupplier =
        clusterAwarenessHealthIndicator.health();

    // then
    assertThat(actualHealthBeforeRegisteringStatusSupplier.getStatus())
        .isSameAs(org.springframework.boot.actuate.health.Status.DOWN);
    assertThat(actualHealthAfterRegisteringStatusSupplier.getStatus())
        .isSameAs(org.springframework.boot.actuate.health.Status.UP);
  }

  @Test
  public void
      shouldCreateGatewayPartitionLeaderAwarenessHealthIndicatorThatIsBackedBySpringGatewayBridge() {
    // precondition
    assertThat(partitionLeaderAwarenessHealthIndicator).isNotNull();
    assertThat(springGatewayBridge).isNotNull();

    // given
    final BrokerClusterState mockClusterState = mock(BrokerClusterState.class);
    when(mockClusterState.getPartitions()).thenReturn(List.of(1));
    when(mockClusterState.getLeaderForPartition(1)).thenReturn(42);

    final Supplier<BrokerClusterState> stateSupplier = () -> mockClusterState;

    // when
    final Health actualHealthBeforeRegisteringStatusSupplier =
        partitionLeaderAwarenessHealthIndicator.health();
    springGatewayBridge.registerClusterStateSupplier(stateSupplier);
    final Health actualHealthAfterRegisteringStatusSupplier =
        partitionLeaderAwarenessHealthIndicator.health();

    // then
    assertThat(actualHealthBeforeRegisteringStatusSupplier.getStatus())
        .isSameAs(org.springframework.boot.actuate.health.Status.DOWN);
    assertThat(actualHealthAfterRegisteringStatusSupplier.getStatus())
        .isSameAs(org.springframework.boot.actuate.health.Status.UP);
  }

  @Configuration
  @ComponentScan({"io.zeebe.gateway.impl", "io.zeebe.util.health"})
  static class Config {}
}
