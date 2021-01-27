/**
 * Copyright 2019 The original authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.dekorate.example;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HTTPGetAction;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.dekorate.utils.Serialization;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThorntailOnKubernetesTest {
  @Test
  void shouldContainDeployment() {
    KubernetesList list = Serialization.unmarshalAsList(ThorntailOnKubernetesTest.class.getClassLoader().getResourceAsStream("META-INF/dekorate/kubernetes.yml"));
    assertNotNull(list);

    Optional<Deployment> deployment = findFirst(list, Deployment.class);
    assertTrue(deployment.isPresent());

    Deployment d = deployment.get();
    List<Container> containers = d.getSpec().getTemplate().getSpec().getContainers();
    assertEquals(1, containers.size());
    Container container = containers.get(0);

    assertEquals(1, container.getPorts().size());
    assertEquals("http", container.getPorts().get(0).getName());
    assertEquals(9090, container.getPorts().get(0).getContainerPort());

    HTTPGetAction livenessProbe = container.getLivenessProbe().getHttpGet();
    assertNotNull(livenessProbe);
    assertEquals("/health", livenessProbe.getPath());
    assertEquals(9090, livenessProbe.getPort().getIntVal());
    assertEquals(180, container.getLivenessProbe().getInitialDelaySeconds());

    HTTPGetAction readinessProbe = container.getReadinessProbe().getHttpGet();
    assertNotNull(readinessProbe);
    assertEquals("/health", readinessProbe.getPath());
    assertEquals(9090, readinessProbe.getPort().getIntVal());
    assertEquals(20, container.getReadinessProbe().getInitialDelaySeconds());

    Optional<Service> service = findFirst(list, Service.class);
    assertTrue(service.isPresent());
    ServiceSpec s = service.get().getSpec();
    assertEquals(1, s.getPorts().size());
    assertEquals("http", s.getPorts().get(0).getName());
    assertEquals(9090, s.getPorts().get(0).getPort());
    assertEquals(80, s.getPorts().get(0).getTargetPort().getIntVal());
  }

  <T extends HasMetadata> Optional<T> findFirst(KubernetesList list, Class<T> type) {
    return list.getItems()
      .stream()
      .filter(type::isInstance)
      .map(type::cast)
      .findFirst();
  }
}
