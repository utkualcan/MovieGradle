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
import org.utku.moviegradle.Models.Movie;
import org.utku.moviegradle.Repositories.ClassificationRepository;
import org.utku.moviegradle.Repositories.MovieRepository;
import org.utku.moviegradle.Models.Classification;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/movies")
@Tag(name = "Movie Management", description = "APIs for managing movies")
public class MovieController {

    private static final Logger log = LoggerFactory.getLogger(MovieController.class);
    private final MovieRepository movieRepository;
    private final ClassificationRepository classificationRepository;

    @Autowired
    public MovieController(MovieRepository movieRepository, ClassificationRepository classificationRepository) {
        this.movieRepository = movieRepository;
        this.classificationRepository = classificationRepository;
    }

    // GET, POST, PUT metotları aynı kalır...

    @GetMapping
    @Operation(summary = "Get all movies", description = "Returns a list of all movies.")
    public List<Movie> getAllMovies() {
        log.info("GET /api/v1/movies called");
        return movieRepository.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get movie by ID", description = "Returns a single movie by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved movie",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Movie.class))),
            @ApiResponse(responseCode = "404", description = "Movie not found with the given ID", content = @Content)
    })
    public ResponseEntity<Movie> getMovieById(
            @Parameter(description = "ID of the movie to retrieve", required = true)
            @PathVariable int id) {
        log.info("GET /api/v1/movies/{} called", id);
        Optional<Movie> movie = movieRepository.findById(id);
        if (movie.isPresent()) {
            log.info("Movie found with ID: {}", id);
            return ResponseEntity.ok(movie.get());
        } else {
            log.warn("Movie not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "Create a new movie", description = "Creates a new movie. The 'movie_id' in the request body is ignored.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Movie created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Movie.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error during movie creation", content = @Content)
    })
    public ResponseEntity<Movie> createMovie(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Movie object to be created. 'movie_id' will be ignored.", required = true,
                    content = @Content(schema = @Schema(implementation = Movie.class)))
            @RequestBody Movie movie) {
        log.info("POST /api/v1/movies called with body: {}", movie);
        try {
            movie.setMovie_id(0);
            Movie savedMovie = movieRepository.save(movie);
            log.info("Movie created successfully with ID: {}", savedMovie.getMovie_id());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedMovie);
        } catch (Exception e) {
            log.error("Error creating movie: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing movie", description = "Updates the movie with the given ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Movie updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Movie.class))),
            @ApiResponse(responseCode = "404", description = "Movie not found with the given ID", content = @Content)
    })
    public ResponseEntity<Movie> updateMovie(
            @Parameter(description = "ID of the movie to update", required = true)
            @PathVariable int id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated movie object.", required = true,
                    content = @Content(schema = @Schema(implementation = Movie.class)))
            @RequestBody Movie movieDetails) {
        log.info("PUT /api/v1/movies/{} called with body: {}", id, movieDetails);
        Optional<Movie> optionalMovie = movieRepository.findById(id);
        if (optionalMovie.isPresent()) {
            Movie existingMovie = optionalMovie.get();
            if (movieDetails.getTitle() != null) {
                existingMovie.setTitle(movieDetails.getTitle());
            }
            if (movieDetails.getDirector() != null) {
                existingMovie.setDirector(movieDetails.getDirector());
            }
            existingMovie.setYear(movieDetails.getYear());

            Movie updatedMovie = movieRepository.save(existingMovie);
            log.info("Movie updated successfully for ID: {}", id);
            return ResponseEntity.ok(updatedMovie);
        } else {
            log.warn("Update failed. Movie not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    // --- DELETE Metodu Güncellendi ---
    @Transactional // Birden fazla DB işlemi olduğu için transactional kalmalı
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a movie and all its classifications", description = "Deletes the movie with the given ID and *permanently deletes all* associated classifications (active or not).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Movie and associated classifications deleted successfully", content = @Content),
            @ApiResponse(responseCode = "404", description = "Movie not found with the given ID", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error during deletion", content = @Content)
            // 409 Conflict artık bu senaryoda dönülmeyecek (aktif bağlantı kontrolü kaldırıldı)
    })
    public ResponseEntity<Void> deleteMovie(
            @Parameter(description = "ID of the movie to delete", required = true)
            @PathVariable int id) {
        log.info("DELETE /api/v1/movies/{} called (Cascade Delete for Classifications)", id);

        // 1. Film var mı kontrol et
        if (!movieRepository.existsById(id)) {
            log.warn("Delete failed. Movie not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }

        // 2. Aktif classification kontrolü KALDIRILDI.

        try {
            // 3. Filme bağlı TÜM classification'ları bul (aktif/pasif farketmez)
            List<Classification> associatedClassifications = classificationRepository.findByMovieId(id);

            if (!associatedClassifications.isEmpty()) {
                log.info("Found {} classifications associated with movie ID {}. Deleting them (HARD DELETE)...", associatedClassifications.size(), id);
                // 4. Bulunan TÜM classification'ları HARD DELETE et
                classificationRepository.deleteAllInBatch(associatedClassifications);
                log.info("Associated classifications deleted successfully.");
            } else {
                log.info("No classifications found associated with movie ID {}.", id);
            }

            // 5. Filmi sil
            movieRepository.deleteById(id);
            log.info("Movie deleted successfully with ID: {}", id);
            return ResponseEntity.noContent().build(); // Başarılı

        } catch (Exception e) {
            // Beklenmedik bir hata olursa (örn. DB bağlantı sorunu)
            log.error("Error during deletion process for movie ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
        }
    }
}