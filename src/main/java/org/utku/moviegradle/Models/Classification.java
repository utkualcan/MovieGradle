package org.utku.moviegradle.Models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity(name= "classification")
public class Classification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "classification_id")
    private int classificationId;

    @Column(name="movie_id")
    private int movieId;

    @Column(name="category_id")
    private int categoryId;

    @Column(name="date")
    private LocalDate date = LocalDate.now();

    @Column(name ="isdeleted")
    private boolean isdeleted = false;
}
