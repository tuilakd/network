package com.example.network.controller;

import com.example.network.entity.DataItem;
import com.example.network.entity.request.DataRequest;
import com.example.network.entity.response.ApiMessageResponse;
import com.example.network.service.DataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xử lý các thao tác CRUD dữ liệu cá nhân của người dùng.
 */
@Slf4j
@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class DataController {

    private final DataService dataService;

    @GetMapping("/hello")
    public ResponseEntity<ApiMessageResponse> hello(Authentication auth) {
        log.info("Người dùng '{}' truy cập endpoint chào hỏi", auth.getName());
        return ResponseEntity.status(HttpStatus.OK).body(new ApiMessageResponse("Xin chào, " + auth.getName()));
    }

    @GetMapping
    public ResponseEntity<List<DataItem>> list(Authentication auth) {
        log.info("Lấy danh sách dữ liệu của người dùng '{}'", auth.getName());
        List<DataItem> items = dataService.list(auth.getName());
        return ResponseEntity.status(HttpStatus.OK).body(items);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataItem> get(@PathVariable Long id, Authentication auth) {
        log.info("Người dùng '{}' truy vấn dữ liệu id={}", auth.getName(), id);
        DataItem item = dataService.get(id, auth.getName());
        return ResponseEntity.status(HttpStatus.OK).body(item);
    }

    @PostMapping
    public ResponseEntity<DataItem> create(@Valid @RequestBody DataRequest request, Authentication auth) {
        log.info("Người dùng '{}' tạo dữ liệu mới với tiêu đề '{}'", auth.getName(), request.getTitle());
        DataItem created = dataService.create(request, auth.getName());
        log.info("Tạo thành công dữ liệu id={} cho người dùng '{}'", created.getId(), auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DataItem> update(@PathVariable Long id,
                                           @Valid @RequestBody DataRequest request,
                                           Authentication auth) {
        log.info("Người dùng '{}' cập nhật dữ liệu id={}", auth.getName(), id);
        DataItem updated = dataService.update(id, request, auth.getName());
        return ResponseEntity.status(HttpStatus.OK).body(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiMessageResponse> delete(@PathVariable Long id, Authentication auth) {
        log.info("Người dùng '{}' yêu cầu xóa dữ liệu id={}", auth.getName(), id);
        dataService.delete(id, auth.getName());
        log.info("Xóa thành công dữ liệu id={}", id);
        return ResponseEntity.status(HttpStatus.OK).body(new ApiMessageResponse("Đã xóa"));
    }
}