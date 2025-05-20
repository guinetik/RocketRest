package com.guinetik.rr.result;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link Result} utility class.
 */
public class ResultTest {

    @Test
    public void testSuccessCreation() {
        Result<Integer, String> ok = Result.success(42);
        assertTrue(ok.isSuccess());
        assertFalse(ok.isFailure());
        assertEquals(Integer.valueOf(42), ok.getValue());
        assertEquals(Integer.valueOf(42), ok.getOrElse(-1));
        assertEquals(Integer.valueOf(42), ok.getOrElseGet(() -> -1));
    }

    @Test
    public void testFailureCreation() {
        Result<Integer, String> err = Result.failure("boom");
        assertTrue(err.isFailure());
        assertFalse(err.isSuccess());
        assertEquals("boom", err.getError());
        assertEquals(Integer.valueOf(-1), err.getOrElse(-1));
    }

    @Test(expected = RuntimeException.class)
    public void testUnwrapFailureThrows() {
        Result.failure("err").unwrap();
    }

    @Test
    public void testMap() {
        Result<Integer, String> ok = Result.success(2);
        Result<Integer, String> mapped = ok.map(x -> x * 2);
        assertEquals(Integer.valueOf(4), mapped.getValue());
    }

    @Test
    public void testMatchConsumers() {
        AtomicBoolean successCalled = new AtomicBoolean(false);
        AtomicBoolean errorCalled = new AtomicBoolean(false);

        Result.success("hi").match(v -> successCalled.set(true), e -> errorCalled.set(true));
        assertTrue(successCalled.get());
        assertFalse(errorCalled.get());

        successCalled.set(false);
        Result.failure("err").match(v -> successCalled.set(true), e -> errorCalled.set(true));
        assertFalse(successCalled.get());
        assertTrue(errorCalled.get());
    }
} 