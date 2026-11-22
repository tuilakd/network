package com.example.network.repository;

import com.example.network.entity.DataItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DataRepository extends JpaRepository<DataItem, Long> {
    List<DataItem> findByOwnerOrderByCreatedAtDesc(String owner);
    void deleteByOwner(String owner);
}