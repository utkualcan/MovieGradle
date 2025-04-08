package org.utku.moviegradle.Models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity(name= "category")

public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int category_id;

    public String name;

}
