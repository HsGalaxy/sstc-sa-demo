package com.sstc.platform.dashboard.repo;

import com.sstc.platform.dashboard.domain.FileRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * S02 - File Tracker index accessor (US01 query, US02 storage).
 */
public interface FileRecordsRepo extends JpaRepository<FileRecord, Long> {

    @Query("select f from FileRecord f where (:bucket is null or f.bucket = :bucket) "
            + "and (:taskId is null or f.taskId = :taskId) "
            + "and (:ownerId is null or f.ownerId = :ownerId) "
            + "and (:archived is null or f.archived = :archived) "
            + "and (:keyword is null or lower(f.name) like lower(concat('%', :keyword, '%'))) "
            + "order by f.uploadedAt desc")
    List<FileRecord> search(@Param("bucket") FileRecord.Bucket bucket,
                            @Param("taskId") Long taskId,
                            @Param("ownerId") Long ownerId,
                            @Param("archived") Boolean archived,
                            @Param("keyword") String keyword);

    List<FileRecord> findByTaskId(Long taskId);
}
