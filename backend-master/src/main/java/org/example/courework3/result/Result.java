package org.example.courework3.result;

import lombok.Data;

@Data
public class Result<T> {

    /**
     * 业务状态码，例如 OK / ERROR
     */
    private String code;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 返回数据
     */
    private T data;

    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode("OK");
        result.setMessage("success");
        result.setData(null);
        return result;
    }

    public static <T> Result<T> success(String message) {
        Result<T> result = new Result<>();
        result.setCode("OK");
        result.setMessage(message);
        result.setData(null);
        return result;
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode("OK");
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode("OK");
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(String code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(null);
        return result;
    }
}