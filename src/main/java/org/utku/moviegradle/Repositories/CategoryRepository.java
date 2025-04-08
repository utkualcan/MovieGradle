package org.utku.moviegradle.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.utku.moviegradle.Models.Category;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}