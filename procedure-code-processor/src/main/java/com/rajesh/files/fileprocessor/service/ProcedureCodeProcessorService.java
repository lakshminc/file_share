package com.rajesh.files.fileprocessor.service;

import com.rajesh.files.fileprocessor.domain.ProcedureCodeData;
import com.rajesh.files.fileprocessor.domain.ProcessStatus;
import com.rajesh.files.fileprocessor.exception.*;
import com.rajesh.files.fileprocessor.repository.ProcedureCodeDataRepository;
import org.hibernate.type.IntegerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProcedureCodeProcessorService {

    public static final String EFF_END_DATE_FORMAT = "yyyy-MM-dd";
    public static final String INFINITE_DATE = "9999-12-31";
    public static final int DATA_ELEMENT_SIZE = 8;
    public static final int EFF_DATE_POSITION = 8;
    public static final int END_DATE_POSITION = 9;

    private static final String expectedHeader = "GEOGRAPHY ID|PROCEDURE CODE|PROCEDURE CODE DESCRIPTION|MODIFIER|ACTUAL/DERIVED INDICATOR|GEOGRAPHIC LEVEL|GEOGRAPHIC DESCRIPTION|REFERENCE AMOUNT";
    @Autowired
    ProcedureCodeDataRepository repository;

    @Value("${input.file.location}")
    private String inputFileLocation;

    @Value("${output.file.location}")
    private String outputFileLocation;

    /**
     * Process the procedure data and generate the data file
     *
     * @return
     * @throws IOException
     */
    public ProcessStatus processProcData() throws IOException {
        ProcessStatus status = new ProcessStatus("OK", "Process started...");
        // Check if input file exists
        //Path inputFilePath = Path.of(inputFileLocation);
        Path inputFilePath = Paths.get(inputFileLocation);
        if (Files.notExists(inputFilePath)) {
            throw new InputFileMissingException("Input file ( " + inputFileLocation + " ) doesn't exist");
        } else {
            try {
                List<String> lines = Files.readAllLines(Paths.get(inputFileLocation));
                if (lines.size() == IntegerType.ZERO) {
                    throw new EmptyInputFileException("Input file is empty.");
                }
                String firstLine = lines.stream().findFirst().get();
                if (!firstLine.equalsIgnoreCase(expectedHeader)) {
                    throw new MissingHeaderInputFileException("First line should be a valid header line");
                }

                //Remove the header
                lines.remove(IntegerType.ZERO.intValue());
                // generate output data
                matchRecordsAndGenerateOutputData(lines);

                status = ProcessStatus.builder()
                        .code("SUCCESS")
                        .description(String.format("Processed %s lines of data and output file generated", lines.size())).build();
            } catch (IOException e) {
                throw new BadInputDataException("Bad process code data");
            }
        }

        return status;
    }


    /**
     * Match input proc code records with existing records and then generate output file
     *
     * @param lines
     * @throws IOException
     */
    private void matchRecordsAndGenerateOutputData(List<String> lines) {
        List<ProcedureCodeData> outputProcCodeRecords = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(EFF_END_DATE_FORMAT);
        LocalDate today = LocalDate.now();
        LocalDate infiniteDate = LocalDate.parse(INFINITE_DATE, formatter);
        List<ProcedureCodeData> inputProcCodeRecords = lines.stream()

                .map(dataLine -> dataLine.split("[|]")).map(dataElements -> {
                            if (dataElements.length < DATA_ELEMENT_SIZE) {
                                throw new BadInputDataException("One or more elements are missing in proc code data");
                            } else {
                                ProcedureCodeData inputProcCodeRecord = ProcedureCodeData.builder()
                                        .geographyId(Integer.valueOf(dataElements[0]))
                                        .procedureCode(Integer.valueOf(dataElements[1]))
                                        .procedureCodeDescription(dataElements[2])
                                        .modifier(dataElements[3])
                                        .actualDerivedIndicator(dataElements[4])
                                        .geographicLevel(dataElements[5])
                                        .geographicDescription(dataElements[6])
                                        .referenceAmount(Double.valueOf(dataElements[7]))
                                        .effectiveDate(null)
                                        .endDate(null).build();

                                if (dataElements.length > EFF_DATE_POSITION) {
                                    LocalDate effDate = LocalDate.parse(dataElements[EFF_DATE_POSITION + 1], formatter);
                                    inputProcCodeRecord.setEffectiveDate(effDate);
                                }
                                if (dataElements.length > END_DATE_POSITION) {
                                    LocalDate endDate = LocalDate.parse(dataElements[END_DATE_POSITION + 1], formatter);
                                    inputProcCodeRecord.setEndDate(endDate);
                                }
                                return inputProcCodeRecord;
                            }
                        }
                ).collect(Collectors.toList());

        // Get procedure codes from input data
        List<Integer> inputProcCodes = inputProcCodeRecords.stream()
                .map(inputProcCodeData -> inputProcCodeData.getProcedureCode())
                .collect(Collectors.toList());

        // Get proc code data for given proc codes from database
        List<ProcedureCodeData> procCodesFromDb = repository.findByProcedureCodeIn(inputProcCodes);

        // Derive output proc code records from input and existing proc code records
        inputProcCodeRecords.stream().forEach(dataFromFile -> {
            // check if procedure code exists in database
            Optional<ProcedureCodeData> procedureCodeRecordFromDBOpt = Optional.ofNullable(procCodesFromDb).orElseGet(Collections::emptyList).stream()
                    .filter(dataFromDB -> dataFromDB.getProcedureCode().intValue() == dataFromFile.getProcedureCode().intValue())
                    .findAny();

            // Output record from existing input record
            ProcedureCodeData outputProcCodeRecordFromFile = ProcedureCodeData.builder()
                    .geographyId(dataFromFile.getGeographyId())
                    .procedureCode(dataFromFile.getProcedureCode())
                    .procedureCodeDescription(dataFromFile.getProcedureCodeDescription())
                    .modifier(dataFromFile.getModifier())
                    .actualDerivedIndicator(dataFromFile.getActualDerivedIndicator())
                    .geographicLevel(dataFromFile.getGeographicLevel())
                    .geographicDescription(dataFromFile.getGeographicDescription())
                    .referenceAmount(dataFromFile.getReferenceAmount())
                    .effectiveDate(dataFromFile.getEffectiveDate())
                    .endDate(dataFromFile.getEndDate()).build();

            // Derive new record and/or update the amount and dates if (not) match found
            if (procedureCodeRecordFromDBOpt.isPresent()) {
                ProcedureCodeData procedureCodeRecordFromDB = procedureCodeRecordFromDBOpt.get();
                // Output record from existing matching record
                ProcedureCodeData outputProcCodeRecordFromDB = ProcedureCodeData.builder()
                        .geographyId(procedureCodeRecordFromDB.getGeographyId())
                        .procedureCode(procedureCodeRecordFromDB.getProcedureCode())
                        .procedureCodeDescription(procedureCodeRecordFromDB.getProcedureCodeDescription())
                        .modifier(procedureCodeRecordFromDB.getModifier())
                        .actualDerivedIndicator(procedureCodeRecordFromDB.getActualDerivedIndicator())
                        .geographicLevel(procedureCodeRecordFromDB.getGeographicLevel())
                        .geographicDescription(procedureCodeRecordFromDB.getGeographicDescription())
                        .referenceAmount(procedureCodeRecordFromDB.getReferenceAmount())
                        .effectiveDate(procedureCodeRecordFromDB.getEffectiveDate())
                        .endDate(procedureCodeRecordFromDB.getEndDate()).build();

                // update existing amount from input record and end date to today's date
                outputProcCodeRecordFromDB.setEndDate(today);

                // update dates for input record, effective data s today and end date as infinite date
                outputProcCodeRecordFromFile.setEffectiveDate(today);
                outputProcCodeRecordFromFile.setEndDate(infiniteDate);
                outputProcCodeRecords.add(outputProcCodeRecordFromDB);
            } else {
                outputProcCodeRecordFromFile.setEffectiveDate(today);
                outputProcCodeRecordFromFile.setEndDate(infiniteDate);
            }
            outputProcCodeRecords.add(outputProcCodeRecordFromFile);
        });

        // Write output records to file
        Path outputFilePath = Paths.get(outputFileLocation);
        try {
            Files.deleteIfExists(outputFilePath);
            // Write the output records to file

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileLocation))) {
                for (ProcedureCodeData data : outputProcCodeRecords) {
                    writer.write(data.content() + System.lineSeparator());
                }
            } catch (IOException e) {
                throw new BadOutputDataException("Failed to write output records to file: " + outputFileLocation);
            }
        } catch (IOException e) {
            System.out.println("Error in deleting file...");
        }


    }

}
