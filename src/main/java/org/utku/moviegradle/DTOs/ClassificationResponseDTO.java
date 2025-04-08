package org.utku.moviegradle.DTOs;

import lombok.*;
import org.utku.moviegradle.Models.Category;
import org.utku.moviegradle.Models.Classification;
import org.utku.moviegradle.Models.Movie;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClassificationResponseDTO {

    private int classificationId;
    private Movie movie;
    private Category category;
    private LocalDate date;

    public static ClassificationResponseDTO fromEntities(Classification classification, Movie movie, Category category) {
        return new ClassificationResponseDTO(
                classification.getClassificationId(),
                movie,
                category,
                classification.getDate()
        );
    }
}
