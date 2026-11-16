package com.example.network.exception;

import com.example.network.exception.code.NetErrorCode;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class NetError {
    private NetErrorCode errCode;
    private List<Object> args;

    public NetError(NetErrorCode errCode){
        super();
        this.errCode = errCode;
        this.args = new ArrayList<>();
    }
}
