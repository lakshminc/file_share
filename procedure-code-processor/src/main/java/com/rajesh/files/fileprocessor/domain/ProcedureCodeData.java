package com.rajesh.files.fileprocessor.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "PROCEDURE_CODES")
public class ProcedureCodeData {
    Integer geographyId;
    @Id
    @Column(name = "proc_code")
    Integer procedureCode;
    @Column(name = "proc_code_desc")
    String procedureCodeDescription;
    String modifier;
    @Column(name = "actual_derived_ind")
    String actualDerivedIndicator;
    String geographicLevel;
    @Column(name = "geographic_desc")
    String geographicDescription;
    Double referenceAmount;
    @Column(name = "eff_dt")
    LocalDate effectiveDate;
    @Column(name = "end_dt")
    LocalDate endDate;

    public String content() {
        return geographyId +
                " " + procedureCode +
                " " + procedureCodeDescription +
                " " + modifier +
                " " + actualDerivedIndicator +
                " " + geographicLevel +
                " " + geographicDescription +
                " " + referenceAmount +
                " " + effectiveDate +
                " " + endDate;
    }
}
