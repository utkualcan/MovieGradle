package org.utku.moviegradle.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.utku.moviegradle.Models.Category;
import org.utku.moviegradle.Repositories.CategoryRepository;
import org.utku.moviegradle.Repositories.ClassificationRepository;


import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Category Management", description = "APIs for managing movie categories")
public class CategoryController {

    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);
    private final CategoryRepository categoryRepository;
    private final ClassificationRepository classificationRepository;


    @Autowired
    public CategoryController(CategoryRepository categoryRepository, ClassificationRepository classificationRepository) {
        this.categoryRepository = categoryRepository;
        this.classificationRepository = classificationRepository;
    }

    @GetMapping
    @Operation(summary = "Get all categories", description = "Returns a list of all movie categories.")
    public List<Category> getAllCategories() {
        log.info("GET /api/v1/categories called");
        return categoryRepository.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID", description = "Returns a single category by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved category",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Category.class))),
            @ApiResponse(responseCode = "404", description = "Category not found with the given ID", content = @Content)
    })
    public ResponseEntity<Category> getCategoryById(
            @Parameter(description = "ID of the category to retrieve", required = true)
            @PathVariable int id) {
        log.info("GET /api/v1/categories/{} called", id);
        Optional<Category> category = categoryRepository.findById(id);
        if (category.isPresent()) {
            log.info("Category found with ID: {}", id);
            return ResponseEntity.ok(category.get());
        } else {
            log.warn("Category not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "Create a new category", description = "Creates a new movie category. The 'category_id' in the request body is ignored.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Category created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Category.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input, category name cannot be empty", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error during category creation", content = @Content)
    })
    public ResponseEntity<Category> createCategory(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Category object to be created. 'category_id' will be ignored.", required = true,
                    content = @Content(schema = @Schema(implementation = Category.class)))
            @RequestBody Category category) {
        log.info("POST /api/v1/categories called with body: {}", category);
        try {
            if (category.getName() == null || category.getName().trim().isEmpty()) {
                log.warn("Attempted to create category with empty name.");
                return ResponseEntity.badRequest().build();
            }
            category.setCategory_id(0);
            Category savedCategory = categoryRepository.save(category);
            log.info("Category created successfully with ID: {}", savedCategory.getCategory_id());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedCategory);
        } catch (Exception e) {
            log.error("Error creating category: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing category", description = "Updates the category with the given ID. Only the 'name' field is updated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Category.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input, category name cannot be empty", content = @Content),
            @ApiResponse(responseCode = "404", description = "Category not found with the given ID", content = @Content)
    })
    public ResponseEntity<Category> updateCategory(
            @Parameter(description = "ID of the category to update", required = true)
            @PathVariable int id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated category object. Only 'name' will be used.", required = true,
                    content = @Content(schema = @Schema(implementation = Category.class)))
            @RequestBody Category categoryDetails) {
        log.info("PUT /api/v1/categories/{} called with body: {}", id, categoryDetails);
        Optional<Category> optionalCategory = categoryRepository.findById(id);
        if (optionalCategory.isPresent()) {
            Category existingCategory = optionalCategory.get();
            if (categoryDetails.getName() == null || categoryDetails.getName().trim().isEmpty()) {
                log.warn("Attempted to update category ID {} with empty name.", id);
                return ResponseEntity.badRequest().build();
            }
            existingCategory.setName(categoryDetails.getName());
            Category updatedCategory = categoryRepository.save(existingCategory);
            log.info("Category updated successfully for ID: {}", id);
            return ResponseEntity.ok(updatedCategory);
        } else {
            log.warn("Update failed. Category not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category", description = "Deletes the category with the given ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Category deleted successfully", content = @Content),
            @ApiResponse(responseCode = "404", description = "Category not found with the given ID", content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflict, category cannot be deleted (e.g., due to existing classifications)", content = @Content)
    })
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "ID of the category to delete", required = true)
            @PathVariable int id) {
        log.info("DELETE /api/v1/categories/{} called", id);
        if (categoryRepository.existsById(id)) {
            try {
                boolean hasClassifications = classificationRepository.existsByCategoryId(id);
                if (hasClassifications) {
                    log.warn("Conflict deleting category ID {}: It has existing classifications.", id);
                    return ResponseEntity.status(HttpStatus.CONFLICT).build();
                }

                categoryRepository.deleteById(id);
                log.info("Category deleted successfully with ID: {}", id);
                return ResponseEntity.noContent().build();
            } catch (Exception e) {
                log.error("Error deleting category ID {}: {}", id, e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
        } else {
            log.warn("Delete failed. Category not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
}
