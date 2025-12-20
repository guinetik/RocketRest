package com.guinetik.rr.interceptor;

import com.guinetik.rr.http.RocketRestException;
import com.guinetik.rr.request.RequestSpec;

/**
 * Interceptor for adding cross-cutting behavior to HTTP request execution.
 *
 * <p>Interceptors provide lifecycle hooks that can modify requests before execution,
 * transform responses after execution, and handle errors (including retry logic).
 * They form a chain where each interceptor can delegate to the next.
 *
 * <h2>Interceptor Chain</h2>
 * <pre>
 * Request → [Interceptor 1] → [Interceptor 2] → ... → [HTTP Client] → Response
 *              ↓ beforeRequest()                           ↓
 *              ↓ onError() if exception                    ↓
 *              ← afterResponse() ←←←←←←←←←←←←←←←←←←←←←←←←←←
 * </pre>
 *
 * <h2>Creating an Interceptor</h2>
 * <pre class="language-java">{@code
 * public class LoggingInterceptor implements RequestInterceptor {
 *
 *     @Override
 *     public <Req, Res> RequestSpec<Req, Res> beforeRequest(RequestSpec<Req, Res> request) {
 *         System.out.println("→ " + request.getMethod() + " " + request.getEndpoint());
 *         return request;
 *     }
 *
 *     @Override
 *     public <Res> Res afterResponse(Res response, RequestSpec<?, Res> request) {
 *         System.out.println("← Response received");
 *         return response;
 *     }
 * }
 * }</pre>
 *
 * <h2>Retry Interceptor Example</h2>
 * <pre class="language-java">{@code
 * public class RetryInterceptor implements RequestInterceptor {
 *     private final int maxRetries = 3;
 *
 *     @Override
 *     public <Req, Res> Res onError(RocketRestException e, RequestSpec<Req, Res> request,
 *                                    InterceptorChain chain) throws RocketRestException {
 *         if (isRetryable(e) && chain.getRetryCount() < maxRetries) {
 *             Thread.sleep(1000 * chain.getRetryCount()); // Exponential backoff
 *             return chain.retry(request);
 *         }
 *         throw e;
 *     }
 * }
 * }</pre>
 *
 * <h2>Using Interceptors</h2>
 * <pre class="language-java">{@code
 * RocketClient client = RocketClientFactory.builder("https://api.example.com")
 *     .withInterceptor(new LoggingInterceptor())
 *     .withInterceptor(new RetryInterceptor(3, 1000))
 *     .withInterceptor(new MetricsInterceptor())
 *     .build();
 * }</pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see InterceptorChain
 * @see RetryInterceptor
 * @since 1.1.0
 */
public interface RequestInterceptor {

    /**
     * Called before a request is executed.
     *
     * <p>This method can modify the request (e.g., add headers, transform body)
     * or return the same request unchanged. To short-circuit the chain and
     * prevent execution, throw an exception.
     *
     * @param request The request specification
     * @param <Req> The request body type
     * @param <Res> The response type
     * @return The (possibly modified) request to execute
     */
    default <Req, Res> RequestSpec<Req, Res> beforeRequest(RequestSpec<Req, Res> request) {
        return request;
    }

    /**
     * Called after a successful response is received.
     *
     * <p>This method can transform the response or perform side effects
     * like logging or metrics collection.
     *
     * @param response The response from the server
     * @param request The original request specification
     * @param <Res> The response type
     * @return The (possibly transformed) response
     */
    default <Res> Res afterResponse(Res response, RequestSpec<?, Res> request) {
        return response;
    }

    /**
     * Called when an exception occurs during request execution.
     *
     * <p>This method can:
     * <ul>
     *   <li>Retry the request using {@link InterceptorChain#retry(RequestSpec)}</li>
     *   <li>Transform the exception into a different one</li>
     *   <li>Recover and return a fallback response</li>
     *   <li>Rethrow the exception (default behavior)</li>
     * </ul>
     *
     * @param e The exception that occurred
     * @param request The request that failed
     * @param chain The interceptor chain (use for retry)
     * @param <Req> The request body type
     * @param <Res> The response type
     * @return A recovered response, or throws an exception
     * @throws RocketRestException If the error cannot be handled
     */
    default <Req, Res> Res onError(RocketRestException e, RequestSpec<Req, Res> request,
                                    InterceptorChain chain) throws RocketRestException {
        throw e;
    }

    /**
     * Returns the order of this interceptor in the chain.
     *
     * <p>Lower values execute first. Use negative values for interceptors
     * that must run early (e.g., authentication), and positive values
     * for interceptors that should run late (e.g., logging).
     *
     * <p>Suggested ordering:
     * <ul>
     *   <li>-100: Authentication/Authorization</li>
     *   <li>0: Default (most interceptors)</li>
     *   <li>100: Retry logic</li>
     *   <li>200: Logging/Metrics</li>
     * </ul>
     *
     * @return The order value (lower = earlier)
     */
    default int getOrder() {
        return 0;
    }
}
