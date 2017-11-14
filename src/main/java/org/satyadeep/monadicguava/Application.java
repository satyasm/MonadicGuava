package org.satyadeep.monadicguava;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Application {

    public static void main(String[] args) throws Exception {
        final ExecutorService executor = Executors.newFixedThreadPool(4);

        Function<Integer, Integer> add2 = x -> x + 2;
        Function<Integer, Integer> mul8 = x -> x * 8;
        Function<Integer, Integer> fail = x -> {
            throw new RuntimeException("Dam broke and number overflowed!");
        };

        // composing computations

        printResult(
                MonadicListenableFuture.hoist(2, executor)
                        .compose(add2).compose(mul8)
        );
        printResult(
                MonadicListenableFuture.hoist(2, executor)
                        .compose(add2).compose(mul8).compose(add2).compose(mul8)
        );

        // simulating failure

        printResult(
                MonadicListenableFuture.hoist(2, executor)
                        .compose(add2).compose(mul8).compose(fail).compose(add2).compose(mul8)
        );

        executor.shutdown();
    }

    static <V> void printResult(MonadicListenableFuture<V> m) {
        try {
            V v = m.getFuture().get();
            System.out.println(v);
        } catch (ExecutionException e) {
            System.out.println("Error executing chain: " + e.getCause());
        } catch (InterruptedException ie) {
            System.out.println("Execution interrupted: " + ie);
        }
    }

    public static class MonadicListenableFuture<V> {
        final ListenableFuture<V> future;
        final Executor exec;

        private MonadicListenableFuture(ListenableFuture<V> f, Executor e) {
            future = f;
            exec = e;
        }

        public static <V> MonadicListenableFuture<V> hoist(V v, Executor e) {
            return new MonadicListenableFuture<>(Futures.immediateFuture(v), e);
        }

        public <O> MonadicListenableFuture<O> compose(Function<V, ? extends O> func) {
            return new MonadicListenableFuture<>(Futures.transform(future, func, exec), exec);
        }

        public ListenableFuture<V> getFuture() {
            return future;
        }
    }
}
