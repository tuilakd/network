package com.example.network.service;

import com.example.network.entity.RequestLog;

import java.util.List;

public interface LogService {

    List<RequestLog> latest(int size);
}