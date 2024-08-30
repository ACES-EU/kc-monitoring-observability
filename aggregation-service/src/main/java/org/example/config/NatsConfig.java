package org.example.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "nats.server")
public interface NatsConfig {

  @WithName("url")
  String url();

  @WithName("stream")
  String stream();

  @WithName("input-subject.prefix")
  String inputSubjectPrefix();

  @WithName("output-subject.prefix")
  String outputSubjectPrefix();
}
