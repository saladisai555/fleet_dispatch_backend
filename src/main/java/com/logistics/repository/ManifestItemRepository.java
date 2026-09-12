package com.logistics.repository;

import com.logistics.entity.ManifestItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface ManifestItemRepository extends JpaRepository<ManifestItem, Long> {

    // Needed to render a manifest's full sequenced item list
    List<ManifestItem> findByManifestIdOrderBySequenceNumberAsc(Long manifestId);

    // Needed by ComplianceService: is this order already committed to a manifest?
    // (DB does not enforce this uniqueness itself — see Step 5 note — so the
    // service layer relies on this query to enforce "one active manifest per order.")
    List<ManifestItem> findByOrderId(Long orderId);

    Optional<ManifestItem> findByManifestIdAndOrderId(Long manifestId, Long orderId);
}