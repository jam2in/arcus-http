package com.jam2in.arcus.http.support;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;
import net.spy.memcached.internal.GetFuture;
import net.spy.memcached.internal.OperationFuture;

public final class TestClientFutures {

  private TestClientFutures() {
  }

  @SuppressWarnings("unchecked")
  public static OperationFuture<Boolean> storeSuccess(boolean value) throws Exception {
    OperationFuture<Boolean> future = mock(OperationFuture.class);
    when(future.get()).thenReturn(value);
    return future;
  }

  @SuppressWarnings("unchecked")
  public static GetFuture<Object> getSuccess(Object value) throws Exception {
    GetFuture<Object> future = mock(GetFuture.class);
    when(future.get()).thenReturn(value);
    return future;
  }

  @SuppressWarnings("unchecked")
  public static GetFuture<Object> getFailure(Throwable cause) throws Exception {
    GetFuture<Object> future = mock(GetFuture.class);
    when(future.get()).thenThrow(new ExecutionException(cause));
    return future;
  }
}
