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
import org.utku.moviegradle.DTOs.ClassificationRequestDTO;
import org.utku.moviegradle.DTOs.ClassificationResponseDTO;
import org.utku.moviegradle.Models.Category;
import org.utku.moviegradle.Models.Classification;
import org.utku.moviegradle.Models.Movie;
import org.utku.moviegradle.Repositories.CategoryRepository;
import org.utku.moviegradle.Repositories.ClassificationRepository;
import org.utku.moviegradle.Repositories.MovieRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/api/v1/classifications")
@Tag(name = "Classification Management", description = "APIs for managing movie classifications (linking movies and categories)")
public class ClassificationController {

    private static final Logger log = LoggerFactory.getLogger(ClassificationController.class);

    private final ClassificationRepository classificationRepository;
    private final MovieRepository movieRepository;
    private final CategoryRepository categoryRepository;

    @Autowired
    public ClassificationController(ClassificationRepository classificationRepository,
                                    MovieRepository movieRepository,
                                    CategoryRepository categoryRepository) {
        this.classificationRepository = classificationRepository;
        this.movieRepository = movieRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    @Operation(summary = "Get all active classifications", description = "Returns a list of all classifications that are not marked as deleted. Includes movie and category details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved classifications",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClassificationResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error while retrieving classifications", content = @Content)
    })
    public ResponseEntity<?> getAllClassifications() {
        log.info("==> GET /api/v1/classifications called.");
        try {
            List<Classification> classifications = classificationRepository.findByIsdeletedFalse();
            log.info("Found {} active classifications.", classifications.size());

            List<ClassificationResponseDTO> responseDTOs = new ArrayList<>();
            for (Classification c : classifications) {
                log.debug("Processing classification ID: {}", c.getClassificationId());
                Optional<Movie> movieOpt = movieRepository.findById(c.getMovieId());
                Optional<Category> categoryOpt = categoryRepository.findById(c.getCategoryId());

                if (movieOpt.isPresent() && categoryOpt.isPresent()) {
                    log.debug("Found related Movie ID {} and Category ID {}.", c.getMovieId(), c.getCategoryId());
                    try {
                        ClassificationResponseDTO dto = ClassificationResponseDTO.fromEntities(c, movieOpt.get(), categoryOpt.get());
                        responseDTOs.add(dto);
                        log.debug("DTO created: {}", dto);
                    } catch (Exception e) {
                        log.error("Error creating DTO for Classification ID {}: {}", c.getClassificationId(), e.getMessage(), e);
                        // Optionally skip this DTO or handle differently
                    }
                } else {
                    log.warn("!!! Missing relation for Classification ID {}! Movie found: {}, Category found: {}",
                            c.getClassificationId(), movieOpt.isPresent(), categoryOpt.isPresent());
                }
            }
            log.info("<== Returning {} DTOs.", responseDTOs.size());
            return ResponseEntity.ok(responseDTOs);

        } catch (Exception e) {
            log.error("!!! General error in getAllClassifications: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving classifications: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get active classification by ID", description = "Returns a single active classification by its ID, including movie and category details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved classification",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClassificationResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Active classification not found with the given ID", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error or related data missing", content = @Content)
    })
    public ResponseEntity<?> getClassificationById(
            @Parameter(description = "ID of the classification to retrieve", required = true)
            @PathVariable int id) {
        log.info("==> GET /api/v1/classifications/{} called.", id);
        try {
            Optional<Classification> classificationOpt = classificationRepository.findActiveById(id);

            if (classificationOpt.isPresent()) {
                Classification c = classificationOpt.get();
                log.info("Active classification found: ID {}", c.getClassificationId());
                Optional<Movie> movieOpt = movieRepository.findById(c.getMovieId());
                Optional<Category> categoryOpt = categoryRepository.findById(c.getCategoryId());

                if (movieOpt.isPresent() && categoryOpt.isPresent()) {
                    log.debug("Found related Movie ID {} and Category ID {}.", c.getMovieId(), c.getCategoryId());
                    ClassificationResponseDTO dto = ClassificationResponseDTO.fromEntities(c, movieOpt.get(), categoryOpt.get());
                    log.info("<== Returning DTO: {}", dto);
                    return ResponseEntity.ok(dto);
                } else {
                    log.error("!!! Missing relation for Classification ID {}! Movie found: {}, Category found: {}",
                            c.getClassificationId(), movieOpt.isPresent(), categoryOpt.isPresent());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Classification found, but related movie or category is missing.");
                }
            } else {
                log.warn("!!! Active Classification with ID {} not found.", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("!!! General error in getClassificationById (ID: {}): {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving classification: " + e.getMessage());
        }
    }

    @PostMapping
    @Operation(summary = "Create a new classification", description = "Links a movie to a category. Ensures the combination is unique among active classifications.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Classification created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClassificationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input: Movie ID or Category ID is invalid or missing", content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflict: This movie is already actively classified under this category", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error during classification creation", content = @Content)
    })
    public ResponseEntity<?> createClassification(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Object containing movieId and categoryId to link.", required = true,
                    content = @Content(schema = @Schema(implementation = ClassificationRequestDTO.class)))
            @RequestBody ClassificationRequestDTO requestDTO) {
        log.info("==> POST /api/v1/classifications called. RequestBody: {}", requestDTO);
        try {
            if (requestDTO.getMovieId() <= 0 || requestDTO.getCategoryId() <= 0) {
                log.warn("!!! Invalid (zero or negative) Movie or Category ID received.");
                return ResponseEntity.badRequest().body("Movie ID and Category ID must be positive integers.");
            }

            Optional<Movie> movieOpt = movieRepository.findById(requestDTO.getMovieId());
            Optional<Category> categoryOpt = categoryRepository.findById(requestDTO.getCategoryId());

            if (movieOpt.isEmpty()) {
                log.warn("!!! Invalid Movie ID: {}", requestDTO.getMovieId());
                return ResponseEntity.badRequest().body("Invalid Movie ID provided.");
            }
            if (categoryOpt.isEmpty()) {
                log.warn("!!! Invalid Category ID: {}", requestDTO.getCategoryId());
                return ResponseEntity.badRequest().body("Invalid Category ID provided.");
            }
            log.info("Movie and Category IDs are valid.");

            Optional<Classification> existing = classificationRepository.findByMovieIdAndCategoryIdAndIsdeletedFalse(
                    requestDTO.getMovieId(), requestDTO.getCategoryId());

            if (existing.isPresent()) {
                log.warn("!!! Conflict: Movie ID {} already actively classified under Category ID {} (Active Classification ID: {}).",
                        requestDTO.getMovieId(), requestDTO.getCategoryId(), existing.get().getClassificationId());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("This movie is already actively assigned to this category.");
            }
            log.info("No active conflict found, proceeding to create new classification.");

            Classification newClassification = new Classification();
            newClassification.setMovieId(requestDTO.getMovieId());
            newClassification.setCategoryId(requestDTO.getCategoryId());
            newClassification.setDate(LocalDate.now());
            newClassification.setIsdeleted(false);

            Classification savedClassification = classificationRepository.save(newClassification);
            log.info("New classification saved with ID: {}", savedClassification.getClassificationId());

            ClassificationResponseDTO response = ClassificationResponseDTO.fromEntities(
                    savedClassification, movieOpt.get(), categoryOpt.get());
            log.info("<== Returning created DTO: {}", response);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("!!! General error in createClassification: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating classification: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing classification", description = "Updates the movie and category link for a given classification ID. Ensures the new combination doesn't conflict with other active classifications.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Classification updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClassificationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input: Movie ID or Category ID is invalid or missing", content = @Content),
            @ApiResponse(responseCode = "404", description = "Active classification not found with the given ID", content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflict: The target movie/category combination already exists in another active classification", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error during classification update", content = @Content)
    })
    public ResponseEntity<?> updateClassification(
            @Parameter(description = "ID of the classification to update", required = true)
            @PathVariable int id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Object containing the new movieId and categoryId.", required = true,
                    content = @Content(schema = @Schema(implementation = ClassificationRequestDTO.class)))
            @RequestBody ClassificationRequestDTO requestDTO) {
        log.info("==> PUT /api/v1/classifications/{} called. RequestBody: {}", id, requestDTO);
        try{

            if (requestDTO.getMovieId() <= 0 || requestDTO.getCategoryId() <= 0) {
                log.warn("!!! Invalid (zero or negative) Movie or Category ID received for update.");
                return ResponseEntity.badRequest().body("Movie ID and Category ID must be positive integers.");
            }

            Optional<Classification> classificationOpt = classificationRepository.findActiveById(id);

            if (classificationOpt.isEmpty()) {
                log.warn("!!! Update failed. Active Classification with ID {} not found.", id);
                return ResponseEntity.notFound().build();
            }

            Optional<Movie> movieOpt = movieRepository.findById(requestDTO.getMovieId());
            Optional<Category> categoryOpt = categoryRepository.findById(requestDTO.getCategoryId());
            if (movieOpt.isEmpty()) {
                log.warn("!!! Invalid Movie ID for update: {}", requestDTO.getMovieId());
                return ResponseEntity.badRequest().body("Invalid Movie ID provided for update.");
            }
            if (categoryOpt.isEmpty()) {
                log.warn("!!! Invalid Category ID for update: {}", requestDTO.getCategoryId());
                return ResponseEntity.badRequest().body("Invalid Category ID provided for update.");
            }

            Optional<Classification> existingConflict = classificationRepository.findByMovieIdAndCategoryIdAndIsdeletedFalse(
                    requestDTO.getMovieId(), requestDTO.getCategoryId());
            if(existingConflict.isPresent() && existingConflict.get().getClassificationId() != id) {
                log.warn("!!! Conflict on update: Movie ID {} / Category ID {} combination already exists in active Classification ID {}.",
                        requestDTO.getMovieId(), requestDTO.getCategoryId(), existingConflict.get().getClassificationId());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("The target movie/category combination is already assigned in another active classification.");
            }

            Classification existingClassification = classificationOpt.get();
            existingClassification.setMovieId(requestDTO.getMovieId());
            existingClassification.setCategoryId(requestDTO.getCategoryId());

            Classification updatedClassification = classificationRepository.save(existingClassification);
            log.info("Classification updated successfully for ID: {}", updatedClassification.getClassificationId());

            ClassificationResponseDTO response = ClassificationResponseDTO.fromEntities(
                    updatedClassification, movieOpt.get(), categoryOpt.get());
            log.info("<== Returning updated DTO: {}", response);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("!!! General error in updateClassification (ID: {}): {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating classification: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a classification (Soft Delete)", description = "Marks the classification with the given ID as deleted.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Classification marked as deleted successfully", content = @Content),
            @ApiResponse(responseCode = "404", description = "Classification not found with the given ID", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error during classification deletion", content = @Content)
    })
    public ResponseEntity<Void> deleteClassification(
            @Parameter(description = "ID of the classification to delete (soft delete)", required = true)
            @PathVariable int id) {
        log.info("==> DELETE /api/v1/classifications/{} called (soft delete).", id);
        try {
            Optional<Classification> optionalClassification = classificationRepository.findById(id);

            if (optionalClassification.isPresent()) {
                Classification classification = optionalClassification.get();
                if (classification.isIsdeleted()) {
                    log.info("Classification ID {} was already deleted.", id);
                    return ResponseEntity.noContent().build();
                }
                classification.setIsdeleted(true);
                classificationRepository.save(classification);
                log.info("Classification ID {} marked as deleted (soft delete).", id);
                return ResponseEntity.noContent().build();
            } else {
                log.warn("!!! Delete failed. Classification not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("!!! General error in deleteClassification (ID: {}): {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
