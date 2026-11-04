package com.example.network.service;

import com.example.network.dto.DataRequest;
import com.example.network.entity.DataItem;
import com.example.network.exception.ResourceNotFoundException;
import com.example.network.repository.DataRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DataService {

    private final DataRepository dataRepository;

    public DataService(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    public List<DataItem> list(String owner) {
        return dataRepository.findByOwnerOrderByCreatedAtDesc(owner);
    }

    public DataItem get(Long id, String owner) {
        return findOwned(id, owner);
    }

    public DataItem create(DataRequest req, String owner) {
        DataItem item = new DataItem();
        item.setTitle(req.getTitle());
        item.setContent(req.getContent());
        item.setOwner(owner);
        return dataRepository.save(item);
    }

    public DataItem update(Long id, DataRequest req, String owner) {
        DataItem item = findOwned(id, owner);
        item.setTitle(req.getTitle());
        item.setContent(req.getContent());
        return dataRepository.save(item);
    }

    public void delete(Long id, String owner) {
        dataRepository.delete(findOwned(id, owner));
    }

    private DataItem findOwned(Long id, String owner) {
        DataItem item = dataRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dữ liệu id=" + id));
        if (!item.getOwner().equals(owner)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền với dữ liệu này");
        }
        return item;
    }
}