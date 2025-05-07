package com.lzb.gateway.constants;

import lombok.Data;

/**
 * http请求结果
 * @author Lijin
 * @qq 1175572685
 * @time 2025/3/25 17:26
 */
@Data
public class Result<D> {
    private int status;
    private D data;
    private String message;

    public Result(int status, D data) {
        this.status = status;
        this.data = data;
    }

    public Result(int status, D data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public static <D> Result<D> success(D data) {
        return new Result<>(200, data);
    }

    public static <D> Result<D> failure() {
        return new Result<>(-1, null);
    }

    public static <D> Result<D> failure(String message) {
        return new Result<>(-1, null, message);
    }
}
