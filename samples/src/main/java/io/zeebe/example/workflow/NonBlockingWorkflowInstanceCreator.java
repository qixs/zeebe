/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.example.workflow;

import io.zeebe.client.ZeebeClient;
import io.zeebe.client.ZeebeClientBuilder;
import io.zeebe.client.api.ZeebeFuture;
import io.zeebe.client.api.response.WorkflowInstanceEvent;

/**
 * Example application that connects to a cluster on Camunda Cloud, or a locally deployed cluster.
 *
 * <p>When connecting to a cluster in Camunda Cloud, this application assumes that the following
 * environment variables are set:
 *
 * <ul>
 *   <li>ZEEBE_ADDRESS
 *   <li>ZEEBE_CLIENT_ID
 *   <li>ZEEBE_CLIENT_SECRET
 *   <li>ZEEBE_AUTHORIZATION_SERVER_URL
 * </ul>
 *
 * <p><strong>Hint:</strong> When you create client credentials in Camunda Cloud you have the option
 * to download a file with above lines filled out for you.
 *
 * <p>When {@code ZEEBE_ADDRESS} is not set, it connects to a broker running on localhost with
 * default ports
 */
public final class NonBlockingWorkflowInstanceCreator {
  public static void main(final String[] args) {
    final String defaultAddress = "localhost:26500";
    final String envVarAddress = System.getenv("ZEEBE_ADDRESS");

    final ZeebeClientBuilder clientBuilder;
    if (envVarAddress != null) {
      /* Connect to Camunda Cloud Cluster, assumes that credentials are set in environment variables.
       * See JavaDoc on class level for details
       */
      clientBuilder = ZeebeClient.newClientBuilder().gatewayAddress(envVarAddress);
    } else {
      // connect to local deployment; assumes that authentication is disabled
      clientBuilder = ZeebeClient.newClientBuilder().gatewayAddress(defaultAddress).usePlaintext();
    }

    final int numberOfInstances = 100_000;
    final String bpmnProcessId = "demoProcess";

    try (final ZeebeClient client = clientBuilder.build()) {
      System.out.println("Creating " + numberOfInstances + " workflow instances");

      final long startTime = System.currentTimeMillis();

      long instancesCreating = 0;

      while (instancesCreating < numberOfInstances) {
        // this is non-blocking/async => returns a future
        final ZeebeFuture<WorkflowInstanceEvent> future =
            client.newCreateInstanceCommand().bpmnProcessId(bpmnProcessId).latestVersion().send();

        // could put the future somewhere and eventually wait for its completion

        instancesCreating++;
      }

      // creating one more instance; joining on this future ensures
      // that all the other create commands were handled
      client.newCreateInstanceCommand().bpmnProcessId(bpmnProcessId).latestVersion().send().join();

      System.out.println("Took: " + (System.currentTimeMillis() - startTime));
    }
  }
}
