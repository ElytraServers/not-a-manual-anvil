package cn.elytra.not_a_manual.anvil.util;

import java.util.function.Supplier;

public record Result<T>(boolean success, T errorMessage) {

    public static <T> Result<T> ok() {
        return new Result<>(true, null);
    }

    public static <T> Result<T> fail(T s) {
        return new Result<>(false, s);
    }

    public Result<T> then(Supplier<Result<T>> supplier) {
        if(success) {
            return supplier.get();
        } else {
            return this;
        }
    }
}
