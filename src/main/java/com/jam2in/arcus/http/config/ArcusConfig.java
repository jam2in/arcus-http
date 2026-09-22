package com.jam2in.arcus.http.config;

import net.spy.memcached.ArcusClient;
import net.spy.memcached.ArcusClientPool;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.transcoders.SerializingTranscoder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ArcusProperties.class)
public class ArcusConfig {

  @Bean(destroyMethod = "shutdown")
  public ArcusClientPool arcusClient(ArcusProperties properties) {
    var factory = new ConnectionFactoryBuilder()
        .setOpTimeout(properties.operationTimeoutMs())
        .setCollectionTranscoder(SerializingTranscoder.forCollection()
            .forceJDKSerializationForCollection()
            .build());

    return ArcusClient.createArcusClientPool(
        properties.zookeeperAddress(), properties.serviceCode(), factory, properties.poolSize()
    );
  }

}
