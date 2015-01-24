package ru.tech_mail.forum.responses;

public class BaseResponse<Type> {
    private byte code;
    private Type response;

    public BaseResponse(byte code, Type response) {
        this.code = code;
        this.response = response;
    }

    public byte getCode() {
        return code;
    }

    public Type getResponse() {
        return response;
    }
}
