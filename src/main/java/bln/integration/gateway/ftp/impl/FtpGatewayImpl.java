package bln.integration.gateway.ftp.impl;

import bln.integration.entity.ConnectionConfig;
import bln.integration.entity.ExportData;
import bln.integration.gateway.ftp.FtpGateway;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;


@Service
public class FtpGatewayImpl implements FtpGateway {
    private static final Logger logger = LoggerFactory.getLogger(FtpGatewayImpl.class);
    private Map<String, List<ExportData>> exportData;
    private String path;
    private String fileName;
    private ConnectionConfig config;

    public FtpGateway exportData(Map<String, List<ExportData>>  exportData) {
        this.exportData = exportData;
        return this;
    }
    public FtpGateway config(ConnectionConfig config) {
        this.config = config;
        return this;
    }

    public FtpGateway path(String path) {
        this.path = path;
        return this;
    }

    public FtpGateway fileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public void send() throws Exception {
        Workbook book = buildWorkBook();
        try {
            String fullFileName = path.toLowerCase() + "/" + fileName + ".xls";
            book.write(new FileOutputStream(fullFileName));
            book.close();
            upload(fullFileName, "/");
        }
        catch (Exception e) {
            throw e;
        }
    }

    public void upload(String localFilePath, String remoteFilePath) throws Exception {
        logger.info("upload started");

        int port = 21;
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(config.getUrl(), port);
            ftpClient.login(config.getUserName(), config.getPwd());
            ftpClient.enterLocalPassiveMode();
            logger.info("Connected");

            File localFile = new File(localFilePath);
            InputStream inputStream = new FileInputStream(localFile);
            try {
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                ftpClient.storeFile(remoteFilePath, inputStream);
                logger.info("Stored");
            }
            finally {
                inputStream.close();
            }

            ftpClient.logout();
            ftpClient.disconnect();
        }
        catch (Exception e) {
            logger.info("Failed: " + e.getMessage());
            throw e;
        }

        logger.info("upload completed");
    }

    private Workbook buildWorkBook() {
        logger.info("buildWorkBook started");

        Workbook book = new HSSFWorkbook();
        Sheet sheet = book.createSheet("Лист 1");

        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue("Тип шаблона");
        cell = row.createCell(1);
        cell.setCellValue("Ручной импорт данных");

        row = sheet.createRow(1);
        cell = row.createCell(0);
        cell.setCellValue("Субъект");
        cell = row.createCell(1);
        cell.setCellValue("СБРЭ");

        int rowNum = 1;
        for (String pointCode: exportData.keySet()) {
            rowNum++;
            sheet.createRow(rowNum);

            rowNum++;
            row = sheet.createRow(rowNum);
            cell = row.createCell(0);
            cell.setCellValue("Код ТУ");

            cell = row.createCell(1);
            cell.setCellValue(pointCode);

            cell = row.createCell(2);
            cell.setCellValue("Дискретность");

            cell = row.createCell(3);
            cell.setCellValue(3600);

            rowNum++;
            row = sheet.createRow(rowNum);
            cell = row.createCell(0);
            cell.setCellValue("timestamp");

            cell = row.createCell(1);
            cell.setCellValue(709);

            cell = row.createCell(2);
            cell.setCellValue(710);

            cell = row.createCell(3);
            cell = row.createCell(4);

            rowNum++;
            row = sheet.createRow(rowNum);
            cell = row.createCell(0);
            cell.setCellValue("Время");

            cell = row.createCell(1);
            cell.setCellValue("A+ энергия за час");

            cell = row.createCell(2);
            cell.setCellValue("A- энергия за час");

            cell = row.createCell(3);
            cell.setCellValue("Forced");

            cell = row.createCell(4);
            cell.setCellValue("Status");

            for (ExportData exportData : exportData.get(pointCode)) {
                rowNum++;
                row = sheet.createRow(rowNum);

                cell = row.createCell(0);
                DataFormat format = book.createDataFormat();
                CellStyle dateStyle = book.createCellStyle();
                dateStyle.setDataFormat(format.getFormat("DD.MM.YYYY HH:MM:SS"));
                cell.setCellStyle(dateStyle);
                cell.setCellValue(toDate(exportData.getMeteringDate()));

                cell = row.createCell(1);
                cell.setCellValue(exportData.getValAp());

                cell = row.createCell(2);
                cell.setCellValue(exportData.getValAm());

                cell = row.createCell(3);
                cell.setCellValue(1);

                cell = row.createCell(4);
                cell.setCellValue(1);
            }
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);

        logger.info("buildWorkBook completed");
        return book;
    }


    private Date toDate(LocalDateTime localDate) {
        return Date.from(localDate.atZone(ZoneId.systemDefault()).toInstant());
    }
}
