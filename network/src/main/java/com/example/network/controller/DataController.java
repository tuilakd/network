package com.example.network.controller;

import com.example.network.entity.DataItem;
import com.example.network.entity.request.DataRequest;
import com.example.network.entity.response.ApiMessageResponse;
import com.example.network.service.DataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class DataController {

    private final DataService dataService;

    @GetMapping
    public ResponseEntity<List<DataItem>> list(Authentication auth) {
        return ResponseEntity.ok(dataService.list(auth.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataItem> get(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(dataService.get(id, auth.getName()));
    }

    @PostMapping
    public ResponseEntity<DataItem> create(@Valid @RequestBody DataRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dataService.create(request, auth.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DataItem> update(@PathVariable Long id,
                                           @Valid @RequestBody DataRequest request,
                                           Authentication auth) {
        return ResponseEntity.ok(dataService.update(id, request, auth.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiMessageResponse> delete(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(dataService.delete(id, auth.getName()));
    }
}