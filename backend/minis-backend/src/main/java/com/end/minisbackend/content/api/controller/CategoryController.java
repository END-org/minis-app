package com.end.minisbackend.content.api.controller;

import com.end.minisbackend.content.api.dto.feed.CategoryResponse;
import com.end.minisbackend.content.domain.enums.ContentCategory;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
class CategoryController {
  @GetMapping
  List<CategoryResponse> categories() {
    return ContentCategory.catalog().stream()
        .map(category -> new CategoryResponse(category.name(), category.displayName()))
        .toList();
  }
}
