package com.rajesh.files.fileprocessor.repository;

import com.rajesh.files.fileprocessor.domain.ProcedureCodeData;
import org.springframework.jdbc.core.RowMapper;

import javax.persistence.Column;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class ProcedureCodeRowMapper implements RowMapper<ProcedureCodeData> {
   @Override
   public ProcedureCodeData mapRow(ResultSet rs, int rowNum) throws SQLException {
       return new ProcedureCodeData(
               rs.getInt("geography_id"),
               rs.getInt("proc_code"),
               rs.getString("proc_code_desc"),
               rs.getString("modifier"),
               rs.getString("actual_derived_ind"),
               rs.getString("geographic_level"),
               rs.getString("geographic_desc"),
               rs.getDouble("reference_amount"),
               rs.getDate("eff_dt").toLocalDate(),
               rs.getDate("end_dt").toLocalDate()
       );
   }
}
