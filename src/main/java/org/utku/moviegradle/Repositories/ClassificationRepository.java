package org.utku.moviegradle.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.utku.moviegradle.Models.Classification;
import java.util.List;
import java.util.Optional;

public interface ClassificationRepository extends JpaRepository<Classification, Integer> {

    // --- Aktif Kayıtlarla İlgili Metotlar (GET, CREATE, UPDATE için hala gerekli) ---
    /**
     * Belirtilen film ve kategori ID'sine sahip aktif (isdeleted=false) Classification kaydını bulur.
     */
    Optional<Classification> findByMovieIdAndCategoryIdAndIsdeletedFalse(int movieId, int categoryId);

    /**
     * Tüm aktif (isdeleted=false) Classification kayıtlarını listeler.
     */
    List<Classification> findByIsdeletedFalse();

    /**
     * Belirtilen ID'ye sahip aktif (isdeleted=false) Classification kaydını bulur.
     */
    @Query("SELECT c FROM classification c WHERE c.classificationId = :id AND c.isdeleted = false")
    Optional<Classification> findActiveById(@Param("id") int classificationId);

    /**
     * Belirtilen film ve kategori ID'sine sahip ve silinmiş olarak işaretlenmiş (isdeleted=true)
     * classification kaydını bulur (varsa). (Classification oluşturma/aktive etme işlemi için kullanılır)
     */
    Optional<Classification> findByMovieIdAndCategoryIdAndIsdeletedTrue(int movieId, int categoryId);


    // --- Cascade Delete İçin Yeni Metotlar ---
    /**
     * Belirtilen film ID'sine sahip TÜM Classification kayıtlarını bulur (aktif veya silinmiş farketmez).
     * @param movieId Film ID'si
     * @return Classification listesi.
     */
    List<Classification> findByMovieId(int movieId);

    /**
     * Belirtilen kategori ID'sine sahip TÜM Classification kayıtlarını bulur (aktif veya silinmiş farketmez).
     * @param categoryId Kategori ID'si
     * @return Classification listesi.
     */
    List<Classification> findByCategoryId(int categoryId);


    // --- Artık Gerekli Olmayan Metotlar (Kaldırıldı) ---
    // boolean existsByMovieIdAndIsdeletedFalse(int movieId);
    // boolean existsByCategoryIdAndIsdeletedFalse(int categoryId);
    // List<Classification> findByMovieIdAndIsdeletedTrue(int movieId);
    // List<Classification> findByCategoryIdAndIsdeletedTrue(int categoryId);

}