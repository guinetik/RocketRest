package com.guinetik.rr.result;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A container object which may contain either a result value (success) or an error.
 * Similar to Rust's Result or Scala's Either type, this class provides a way to
 * handle errors without exceptions.
 *
 * @param <T> The type of the value contained in this Result
 * @param <E> The type of the error contained in this Result
 */
public class Result<T, E> {

    private final T value;
    private final E error;
    private final boolean isSuccess;

    private Result(T value, E error, boolean isSuccess) {
        this.value = value;
        this.error = error;
        this.isSuccess = isSuccess;
    }

    /**
     * Creates a successful Result containing the given value.
     *
     * @param value the value
     * @param <T> the type of the value
     * @param <E> the type of the error
     * @return a successful Result containing the value
     */
    public static <T, E> Result<T, E> success(T value) {
        return new Result<>(value, null, true);
    }

    /**
     * Creates a failed Result containing the given error.
     *
     * @param error the error
     * @param <T> the type of the value
     * @param <E> the type of the error
     * @return a failed Result containing the error
     */
    public static <T, E> Result<T, E> failure(E error) {
        return new Result<>(null, error, false);
    }

    /**
     * Returns whether this Result is a success.
     *
     * @return true if this Result is a success, false otherwise
     */
    public boolean isSuccess() {
        return isSuccess;
    }

    /**
     * Returns whether this Result is a failure.
     *
     * @return true if this Result is a failure, false otherwise
     */
    public boolean isFailure() {
        return !isSuccess;
    }

    /**
     * Gets the value contained in this Result.
     *
     * @return the value
     * @throws NoSuchElementException if this Result is a failure
     */
    public T getValue() {
        if (!isSuccess) {
            throw new NoSuchElementException("Cannot get value from a failure Result");
        }
        return value;
    }

    /**
     * Gets the error contained in this Result.
     *
     * @return the error
     * @throws NoSuchElementException if this Result is a success
     */
    public E getError() {
        if (isSuccess) {
            throw new NoSuchElementException("Cannot get error from a success Result");
        }
        return error;
    }

    /**
     * Gets the value contained in this Result or the given default value if this Result is a failure.
     *
     * @param defaultValue the value to return if this Result is a failure
     * @return the value if this Result is a success, otherwise the default value
     */
    public T getOrElse(T defaultValue) {
        return isSuccess ? value : defaultValue;
    }

    /**
     * Gets the value contained in this Result or the value supplied by the given Supplier if this Result is a failure.
     *
     * @param supplier the Supplier to provide the default value
     * @return the value if this Result is a success, otherwise the value from the supplier
     */
    public T getOrElseGet(Supplier<? extends T> supplier) {
        return isSuccess ? value : supplier.get();
    }

    /**
     * Gets the value contained in this Result or throws the given exception if this Result is a failure.
     *
     * @param exceptionSupplier the Supplier to provide the exception to throw
     * @param <X> the type of the exception to throw
     * @return the value if this Result is a success
     * @throws X if this Result is a failure
     */
    public <X extends Throwable> T getOrElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (isSuccess) {
            return value;
        }
        throw exceptionSupplier.get();
    }
    
    /**
     * Returns the value if this Result is a success, or unwraps the error by throwing an exception.
     * This is similar to Rust's unwrap() method.
     * 
     * @return the contained value
     * @throws RuntimeException if this is a failure, with the error toString() as the message
     */
    public T unwrap() {
        if (isSuccess) {
            return value;
        }
        throw new RuntimeException("Unwrapped a failure Result: " + error);
    }

    /**
     * Maps the value of this Result if it's a success, using the given mapping function.
     *
     * @param mapper the function to apply to the value
     * @param <U> the type of the result of the mapping function
     * @return a new Result with the mapped value if this Result is a success, otherwise a new Result with the same error
     */
    public <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
        if (isSuccess) {
            return Result.success(mapper.apply(value));
        }
        return Result.failure(error);
    }

    /**
     * Maps the error of this Result if it's a failure, using the given mapping function.
     *
     * @param mapper the function to apply to the error
     * @param <F> the type of the result of the mapping function
     * @return a new Result with the mapped error if this Result is a failure, otherwise a new Result with the same value
     */
    public <F> Result<T, F> mapError(Function<? super E, ? extends F> mapper) {
        if (isSuccess) {
            return Result.success(value);
        }
        return Result.failure(mapper.apply(error));
    }

    /**
     * Executes the given consumer if this Result is a success.
     *
     * @param consumer the consumer to execute
     * @return this Result
     */
    public Result<T, E> ifSuccess(Consumer<? super T> consumer) {
        if (isSuccess) {
            consumer.accept(value);
        }
        return this;
    }

    /**
     * Executes the given consumer if this Result is a failure.
     *
     * @param consumer the consumer to execute
     * @return this Result
     */
    public Result<T, E> ifFailure(Consumer<? super E> consumer) {
        if (!isSuccess) {
            consumer.accept(error);
        }
        return this;
    }

    /**
     * Converts this Result to an Optional containing the value if this Result is a success,
     * or an empty Optional if this Result is a failure.
     *
     * @return an Optional containing the value if this Result is a success, otherwise an empty Optional
     */
    public Optional<T> toOptional() {
        return isSuccess ? Optional.ofNullable(value) : Optional.empty();
    }

    /**
     * Pattern-matches over this Result, executing one of the consumers depending on success/failure.
     * <pre>
     * result.match(value -&gt; System.out.println(value), err -&gt; log.error(err));
     * </pre>
     *
     * @param successConsumer runs if this result is a success (receives the value)
     * @param errorConsumer   runs if this result is a failure (receives the error)
     * @return this Result for fluent chaining
     */
    public Result<T, E> match(Consumer<? super T> successConsumer, Consumer<? super E> errorConsumer) {
        if (isSuccess) {
            successConsumer.accept(value);
        } else {
            errorConsumer.accept(error);
        }
        return this;
    }

    @Override
    public String toString() {
        if (isSuccess) {
            return "Success[" + value + "]";
        }
        return "Failure[" + error + "]";
    }
} 