package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.time.ZonedDateTime;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with
 * the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method
 * invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final Object delegate;
  private final ProfilingState state;
  private final ZonedDateTime startTime;

  ProfilingMethodInterceptor(
      Clock clock,
      Object delegate,
      ProfilingState state,
      ZonedDateTime startTime) {
    this.clock = Objects.requireNonNull(clock);
    this.delegate = Objects.requireNonNull(delegate);
    this.state = Objects.requireNonNull(state);
    this.startTime = Objects.requireNonNull(startTime);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    Object invokedObject;
    Instant startTime = null;
    boolean isProfiled = method.getAnnotation(Profiled.class) != null;
    if (isProfiled) {
      startTime = clock.instant();
    }
    try {
      invokedObject = method.invoke(this.delegate, args);
    } catch (InvocationTargetException ex) {
      throw ex.getTargetException();
    } catch (IllegalAccessException ex) {
      throw new RuntimeException(ex);
    } finally {
      if (isProfiled) {
        Duration duration = Duration.between(startTime, clock.instant());
        state.record(this.delegate.getClass(), method, duration);
      }
    }
    return invokedObject;
  }
}
