package com.example.network.service.impl;

import com.example.network.entity.request.DataRequest;
import com.example.network.entity.DataItem;
import com.example.network.exception.ResourceNotFoundException;
import com.example.network.repository.DataRepository;
import com.example.network.service.DataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DataServiceImpl implements DataService {

    private final DataRepository dataRepository;

    @Override
    public List<DataItem> list(String owner) {
        return dataRepository.findByOwnerOrderByCreatedAtDesc(owner);
    }

    @Override
    public DataItem get(Long id, String owner) {
        return findOwned(id, owner);
    }

    @Override
    public DataItem create(DataRequest request, String owner) {
        DataItem item = new DataItem();
        item.setTitle(request.getTitle());
        item.setContent(request.getContent());
        item.setOwner(owner);
        return dataRepository.save(item);
    }

    @Override
    public DataItem update(Long id, DataRequest request, String owner) {
        DataItem item = findOwned(id, owner);
        item.setTitle(request.getTitle());
        item.setContent(request.getContent());
        return dataRepository.save(item);
    }

    @Override
    public void delete(Long id, String owner) {
        DataItem item = findOwned(id, owner);
        dataRepository.delete(item);
    }

    private DataItem findOwned(Long id, String owner) {
        Optional<DataItem> found = dataRepository.findById(id);
        if (found.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy dữ liệu id=" + id);
        }
        DataItem item = found.get();
        if (!item.getOwner().equals(owner)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền với dữ liệu này");
        }
        return item;
    }
}