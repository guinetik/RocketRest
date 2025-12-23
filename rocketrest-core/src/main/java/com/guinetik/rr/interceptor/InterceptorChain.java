package com.guinetik.rr.interceptor;

import com.guinetik.rr.http.RocketRestException;
import com.guinetik.rr.request.RequestSpec;

/**
 * Represents the interceptor chain during request execution.
 *
 * <p>This interface is passed to {@link RequestInterceptor#onError} to allow
 * interceptors to retry failed requests or query chain state.
 *
 * <h2>Retry Example</h2>
 * <pre class="language-java"><code>
 * public &lt;Req, Res&gt; Res onError(RocketRestException e, RequestSpec&lt;Req, Res&gt; request,
 *                                InterceptorChain chain) throws RocketRestException {
 *     if (isRetryable(e) &amp;&amp; chain.getRetryCount() &lt; 3) {
 *         return chain.retry(request);
 *     }
 *     throw e;
 * }
 * </code></pre>
 *
 * @author guinetik &lt;guinetik@gmail.com&gt;
 * @see RequestInterceptor
 * @since 1.1.0
 */
public interface InterceptorChain {

    /**
     * Retries the request from the beginning of the chain.
     *
     * <p>This increments the retry count and re-executes the full interceptor
     * chain, including all beforeRequest/afterResponse hooks.
     *
     * @param request The request to retry (can be modified from original)
     * @param <Req> The request body type
     * @param <Res> The response type
     * @return The response from the retried request
     * @throws RocketRestException If the retry also fails
     */
    <Req, Res> Res retry(RequestSpec<Req, Res> request) throws RocketRestException;

    /**
     * Gets the current retry count for this execution.
     *
     * <p>Starts at 0 for the initial request, increments with each retry.
     * Use this to implement retry limits.
     *
     * @return The number of retries attempted so far
     */
    int getRetryCount();

    /**
     * Gets the maximum retry count configured for this chain.
     *
     * <p>Returns 0 if no retry limit is configured.
     *
     * @return The maximum retry count, or 0 if unlimited
     */
    int getMaxRetries();
}
