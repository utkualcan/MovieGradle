package org.utku.moviegradle.DTOs;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationRequestDTO {
    private int movieId;
    private int categoryId;
}
