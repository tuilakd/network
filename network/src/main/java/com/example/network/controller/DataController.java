package com.example.network.controller;

import com.example.network.entity.request.DataRequest;
import com.example.network.entity.DataItem;
import com.example.network.service.DataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class DataController {

    private final DataService dataService;

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
    public ResponseEntity<DataItem> create(@Valid @RequestBody DataRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dataService.create(request, auth.getName()));
    }

    @PutMapping("/{id}")
    public DataItem update(@PathVariable Long id, @Valid @RequestBody DataRequest request, Authentication auth) {
        return dataService.update(id, request, auth.getName());
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id, Authentication auth) {
        dataService.delete(id, auth.getName());
        Map<String, String> body = new HashMap<>();
        body.put("message", "Đã xóa");
        return body;
    }
}