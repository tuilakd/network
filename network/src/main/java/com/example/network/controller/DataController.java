package com.example.network.controller;

import com.example.network.dto.DataRequest;
import com.example.network.entity.DataItem;
import com.example.network.service.DataService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
public class DataController {

    private final DataService dataService;

    public DataController(DataService dataService) {
        this.dataService = dataService;
    }

    @GetMapping("/hello")
    public String hello(Authentication auth) {
        return "Xin chào, " + auth.getName();
    }

    @GetMapping
    public List<DataItem> list(Authentication auth) {
        return dataService.list(auth.getName());
    }

    @GetMapping("/{id}")
    public DataItem get(@PathVariable Long id, Authentication auth) {
        return dataService.get(id, auth.getName());
    }

    @PostMapping
    public ResponseEntity<DataItem> create(@Valid @RequestBody DataRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dataService.create(req, auth.getName()));
    }

    @PutMapping("/{id}")
    public DataItem update(@PathVariable Long id, @Valid @RequestBody DataRequest req, Authentication auth) {
        return dataService.update(id, req, auth.getName());
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id, Authentication auth) {
        dataService.delete(id, auth.getName());
        return Map.of("message", "Đã xóa");
    }
}