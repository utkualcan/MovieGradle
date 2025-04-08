package org.utku.moviegradle.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.utku.moviegradle.Models.Movie;

import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Integer> {
}
