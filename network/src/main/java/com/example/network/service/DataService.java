package com.example.network.service;

import com.example.network.dto.DataRequest;
import com.example.network.entity.DataItem;

import java.util.List;

public interface DataService {

    List<DataItem> list(String owner);

    DataItem get(Long id, String owner);

    DataItem create(DataRequest request, String owner);

    DataItem update(Long id, DataRequest request, String owner);

    void delete(Long id, String owner);
}