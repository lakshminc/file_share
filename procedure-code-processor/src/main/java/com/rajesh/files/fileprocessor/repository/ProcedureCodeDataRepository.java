package com.rajesh.files.fileprocessor.repository;

import java.util.List;
import java.util.Optional;

import com.rajesh.files.fileprocessor.domain.ProcedureCodeData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcedureCodeDataRepository extends JpaRepository<ProcedureCodeData, Integer> {
    Optional<ProcedureCodeData> findByProcedureCode(Integer procedureCode);

    List<ProcedureCodeData> findByProcedureCodeIn(List<Integer> procedureCodes);
}