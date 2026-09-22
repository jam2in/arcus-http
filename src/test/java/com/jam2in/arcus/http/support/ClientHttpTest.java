package com.jam2in.arcus.http.support;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.spy.memcached.ArcusClientPool;
import net.spy.memcached.collection.CollectionResponse;
import net.spy.memcached.internal.CollectionFuture;
import net.spy.memcached.ops.CollectionOperationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class ClientHttpTest {

  @Autowired
  protected MockMvc mvc;

  @MockitoBean
  protected ArcusClientPool client;

  @SuppressWarnings("unchecked")
  protected <T> CollectionFuture<T> collection(
      T value, CollectionResponse status
  ) throws Exception {
    CollectionFuture<T> future = mock(CollectionFuture.class);
    when(future.get()).thenReturn(value);

    boolean success = switch (status.name()) {
      case "OK", "CREATED", "STORED", "CREATED_STORED",
           "REPLACED", "UPDATED", "END",
           "DELETED", "DELETED_DROPPED", "EXIST",
           "TRIMMED", "DUPLICATED", "DUPLICATED_TRIMMED" -> true;
      default -> false;
    };

    when(future.getOperationStatus())
        .thenReturn(new CollectionOperationStatus(success, status.name(), status));

    return future;
  }
}
