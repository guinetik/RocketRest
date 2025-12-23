package com.guinetik.rr.interceptor;

import com.guinetik.rr.http.RocketClient;
import com.guinetik.rr.http.RocketRestException;
import com.guinetik.rr.request.RequestSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Decorator that adds interceptor chain support to any {@link RocketClient}.
 *
 * <p>This client wraps another RocketClient and applies a chain of interceptors
 * to every request. Interceptors can modify requests, transform responses,
 * and handle errors including retry logic.
 *
 * <h2>Interceptor Execution Order</h2>
 * <pre>
 * Request Flow:
 *   beforeRequest(Interceptor 1) → beforeRequest(Interceptor 2) → ... → delegate.execute()
 *
 * Response Flow:
 *   delegate.execute() → ... → afterResponse(Interceptor 2) → afterResponse(Interceptor 1)
 *
 * Error Flow:
 *   exception → onError(Interceptor 1) → onError(Interceptor 2) → ...
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre class="language-java"><code>
 * RocketClient baseClient = new DefaultHttpClient("https://api.example.com");
 *
 * List&lt;RequestInterceptor&gt; interceptors = new ArrayList&lt;&gt;();
 * interceptors.add(new LoggingInterceptor());
 * interceptors.add(new RetryInterceptor(3, 1000));
 *
 * RocketClient client = new InterceptingClient(baseClient, interceptors);
 * </code></pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see RequestInterceptor
 * @see RocketClient
 * @since 1.1.0
 */
public class InterceptingClient implements RocketClient {

    private static final Logger logger = LoggerFactory.getLogger(InterceptingClient.class);

    private final RocketClient delegate;
    private final List<RequestInterceptor> interceptors;
    private final int maxRetries;

    /**
     * Creates an intercepting client with the given interceptors.
     *
     * @param delegate The underlying client to wrap
     * @param interceptors The interceptors to apply (will be sorted by order)
     */
    public InterceptingClient(RocketClient delegate, List<RequestInterceptor> interceptors) {
        this(delegate, interceptors, 3);
    }

    /**
     * Creates an intercepting client with the given interceptors and retry limit.
     *
     * @param delegate The underlying client to wrap
     * @param interceptors The interceptors to apply (will be sorted by order)
     * @param maxRetries The maximum number of retries allowed
     */
    public InterceptingClient(RocketClient delegate, List<RequestInterceptor> interceptors, int maxRetries) {
        if (delegate == null) {
            throw new NullPointerException("delegate must not be null");
        }
        this.delegate = delegate;
        this.maxRetries = maxRetries;

        // Sort interceptors by order and make immutable copy
        List<RequestInterceptor> sorted = new ArrayList<RequestInterceptor>(
            interceptors != null ? interceptors : Collections.<RequestInterceptor>emptyList()
        );
        Collections.sort(sorted, new Comparator<RequestInterceptor>() {
            @Override
            public int compare(RequestInterceptor a, RequestInterceptor b) {
                return Integer.compare(a.getOrder(), b.getOrder());
            }
        });
        this.interceptors = Collections.unmodifiableList(sorted);
    }

    @Override
    public <Req, Res> Res execute(RequestSpec<Req, Res> requestSpec) throws RocketRestException {
        return executeWithRetry(requestSpec, 0);
    }

    /**
     * Executes the request with retry support.
     */
    private <Req, Res> Res executeWithRetry(RequestSpec<Req, Res> requestSpec, int retryCount)
            throws RocketRestException {

        final int currentRetry = retryCount;

        // Create chain context for this execution
        InterceptorChain chain = new InterceptorChain() {
            @Override
            public <R, S> S retry(RequestSpec<R, S> request) throws RocketRestException {
                if (currentRetry >= maxRetries) {
                    throw new RocketRestException("Maximum retry count exceeded: " + maxRetries);
                }
                logger.debug("Retrying request (attempt {}): {} {}",
                    currentRetry + 1, request.getMethod(), request.getEndpoint());
                return executeWithRetry(request, currentRetry + 1);
            }

            @Override
            public int getRetryCount() {
                return currentRetry;
            }

            @Override
            public int getMaxRetries() {
                return maxRetries;
            }
        };

        // Apply beforeRequest interceptors (in order)
        RequestSpec<Req, Res> currentRequest = requestSpec;
        for (RequestInterceptor interceptor : interceptors) {
            currentRequest = interceptor.beforeRequest(currentRequest);
        }

        try {
            // Execute the actual request
            Res response = delegate.execute(currentRequest);

            // Apply afterResponse interceptors (in reverse order)
            for (int i = interceptors.size() - 1; i >= 0; i--) {
                response = interceptors.get(i).afterResponse(response, currentRequest);
            }

            return response;

        } catch (RocketRestException e) {
            // Apply onError interceptors (in order) - first one to return wins
            for (RequestInterceptor interceptor : interceptors) {
                try {
                    Res recovered = interceptor.onError(e, currentRequest, chain);
                    // If we get here, the interceptor recovered - apply afterResponse
                    for (int i = interceptors.size() - 1; i >= 0; i--) {
                        recovered = interceptors.get(i).afterResponse(recovered, currentRequest);
                    }
                    return recovered;
                } catch (RocketRestException rethrown) {
                    // Interceptor rethrew (possibly modified) exception, continue to next
                    e = rethrown;
                }
            }
            // All interceptors rethrew, propagate the last exception
            throw e;
        }
    }

    @Override
    public void configureSsl(SSLContext sslContext) {
        delegate.configureSsl(sslContext);
    }

    @Override
    public void setBaseUrl(String baseUrl) {
        delegate.setBaseUrl(baseUrl);
    }

    /**
     * Gets the list of interceptors in execution order.
     *
     * @return Unmodifiable list of interceptors
     */
    public List<RequestInterceptor> getInterceptors() {
        return interceptors;
    }
}
