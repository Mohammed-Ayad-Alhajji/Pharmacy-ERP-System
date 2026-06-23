package com.pharmacy.utils.export;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

// استيراد كلاسات الإعدادات الخاصة بك
import com.pharmacy.dao.impl.system.SystemSettingsDAOImpl;
import com.pharmacy.models.system.SystemSettings;
import com.pharmacy.services.impl.system.SystemSettingsServiceImpl;
import com.pharmacy.services.interfaces.system.SystemSettingsService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

public class ExportUtils {

    // دالة مساعدة لجلب إعدادات الصيدلية الحالية من قاعدة البيانات
    private static SystemSettings getPharmacySettings() {
        SystemSettingsService settingsService = new SystemSettingsServiceImpl(new SystemSettingsDAOImpl());
        return settingsService.getSettings();
    }

    // ==========================================
    // 1. تصدير البيانات إلى ملف Excel (.xlsx) مع الترويسة والشعار
    // ==========================================
    public static <T> void exportToExcel(TableView<T> table, String fileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("حفظ كملف Excel");
        
        String cleanFileName = fileName.replace(" ", "_").replace(":", "");
        fileChooser.setInitialFileName(cleanFileName + ".xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Data");
                sheet.setRightToLeft(true); 

                // جلب بيانات الصيدلية
                SystemSettings settings = getPharmacySettings();
                
                // --- 1. ترويسة الصيدلية في الإكسل ---
                // دمج الخلايا للاسم
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
                Row phNameRow = sheet.createRow(0);
                Cell phNameCell = phNameRow.createCell(0);
                phNameCell.setCellValue(settings.getPharmacy_name() != null ? settings.getPharmacy_name() : "صيدلية عامة");
                
                CellStyle phNameStyle = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font phNameFont = workbook.createFont();
                phNameFont.setBold(true);
                phNameFont.setFontHeightInPoints((short) 16);
                phNameStyle.setFont(phNameFont);
                phNameCell.setCellStyle(phNameStyle);

                // العنوان والهاتف
                Row addressRow = sheet.createRow(1);
                addressRow.createCell(0).setCellValue("العنوان: " + (settings.getAddress() != null ? settings.getAddress() : "---"));
                Row phoneRow = sheet.createRow(2);
                phoneRow.createCell(0).setCellValue("الهاتف: " + (settings.getPhone() != null ? settings.getPhone() : "---"));

                // --- 2. إدراج الشعار (Logo) في الإكسل ---
                String logoPath = settings.getLogo_path();
                if (logoPath != null && !logoPath.trim().isEmpty()) {
                    try (FileInputStream is = new FileInputStream(logoPath)) {
                        byte[] bytes = IOUtils.toByteArray(is);
                        int pictureIdx = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG); // يدعم JPG و PNG
                        CreationHelper helper = workbook.getCreationHelper();
                        Drawing<?> drawing = sheet.createDrawingPatriarch();
                        ClientAnchor anchor = helper.createClientAnchor();
                        
                        // وضع الصورة في الخلية (العمود 5، الصف 0)
                        anchor.setCol1(5);
                        anchor.setRow1(0);
                        Picture pict = drawing.createPicture(anchor, pictureIdx);
                        pict.resize(2.0, 3.0); // تكبير الصورة لتناسب مساحة 3 صفوف
                    } catch (Exception e) {
                        System.err.println("تعذر تحميل صورة الشعار للإكسل: " + e.getMessage());
                    }
                }

                // --- 3. إنشاء جدول البيانات ---
                int startRowIndex = 5; // تركنا 5 أسطر فارغة للترويسة العلوية
                Row headerRow = sheet.createRow(startRowIndex);
                List<TableColumn<T, ?>> columns = table.getColumns();
                CellStyle headerStyle = workbook.createCellStyle();
                org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                headerStyle.setAlignment(HorizontalAlignment.CENTER);
                headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                for (int i = 0; i < columns.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns.get(i).getText());
                    cell.setCellStyle(headerStyle);
                }

                CellStyle dataStyle = workbook.createCellStyle();
                dataStyle.setAlignment(HorizontalAlignment.CENTER);

                CellStyle currencyStyle = workbook.createCellStyle();
                currencyStyle.setAlignment(HorizontalAlignment.CENTER);
                DataFormat format = workbook.createDataFormat();
                currencyStyle.setDataFormat(format.getFormat("#,##0.00")); 

                ObservableList<T> items = table.getItems();
                for (int i = 0; i < items.size(); i++) {
                    Row row = sheet.createRow(startRowIndex + 1 + i);
                    for (int j = 0; j < columns.size(); j++) {
                        TableColumn<T, ?> col = columns.get(j);
                        String cellValue = extractCellValue(col, items.get(i));
                        Cell cell = row.createCell(j);

                        if (cellValue.contains("ل.س") || cellValue.contains("$")) {
                            try {
                                String cleanNum = cellValue.replaceAll("[^\\d.\\-]", "").trim();
                                double numericValue = Double.parseDouble(cleanNum);
                                cell.setCellValue(numericValue); 
                                cell.setCellStyle(currencyStyle); 
                            } catch (Exception e) {
                                cell.setCellValue(cellValue);
                                cell.setCellStyle(dataStyle);
                            }
                        } else if (cellValue.matches("-?\\d+(\\.\\d+)?")) {
                            try {
                                cell.setCellValue(Double.parseDouble(cellValue));
                                cell.setCellStyle(dataStyle);
                            } catch (Exception e) {
                                cell.setCellValue(cellValue);
                                cell.setCellStyle(dataStyle);
                            }
                        } else {
                            cell.setCellValue(cellValue);
                            cell.setCellStyle(dataStyle);
                        }
                    }
                }

                for (int i = 0; i < columns.size(); i++) {
                    sheet.autoSizeColumn(i);
                    int currentWidth = sheet.getColumnWidth(i);
                    sheet.setColumnWidth(i, currentWidth + 1200); 
                }

                try (FileOutputStream fileOut = new FileOutputStream(file)) {
                    workbook.write(fileOut);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ==========================================
    // 2. تصدير البيانات إلى ملف PDF مع الترويسة والشعار
    // ==========================================
    public static <T> void exportToPDF(TableView<T> table, String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("حفظ كملف PDF");
        
        String cleanFileName = title.replace(" ", "_").replace(":", "");
        fileChooser.setInitialFileName(cleanFileName + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try {
                Document document = new Document(PageSize.A4.rotate());
                PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                com.itextpdf.text.Font font;
                com.itextpdf.text.Font boldFont;
                com.itextpdf.text.Font titleFont;
                try {
                    BaseFont bf = BaseFont.createFont("c:/windows/fonts/arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                    font = new com.itextpdf.text.Font(bf, 11);
                    boldFont = new com.itextpdf.text.Font(bf, 12, com.itextpdf.text.Font.BOLD);
                    titleFont = new com.itextpdf.text.Font(bf, 16, com.itextpdf.text.Font.BOLD);
                } catch (Exception ex) {
                    font = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 11);
                    boldFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.BOLD);
                    titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 16, com.itextpdf.text.Font.BOLD);
                }

                // جلب بيانات الصيدلية
                SystemSettings settings = getPharmacySettings();

                // --- 1. ترويسة الصيدلية في الـ PDF (جدول من عمودين) ---
                PdfPTable headerTable = new PdfPTable(2);
                headerTable.setWidthPercentage(100);
                headerTable.setWidths(new float[]{3f, 1f}); // 3 أجزاء للنص، وجزء للشعار
                headerTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);

                // معلومات الصيدلية (يمين)
                PdfPTable infoTable = new PdfPTable(1);
                infoTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
                
                PdfPCell nameCell = new PdfPCell(new Phrase(settings.getPharmacy_name() != null ? settings.getPharmacy_name() : "صيدلية عامة", titleFont));
                nameCell.setBorder(Rectangle.NO_BORDER);
                nameCell.setPaddingBottom(5);
                infoTable.addCell(nameCell);
                
                PdfPCell addressCell = new PdfPCell(new Phrase("العنوان: " + (settings.getAddress() != null ? settings.getAddress() : "---"), font));
                addressCell.setBorder(Rectangle.NO_BORDER);
                infoTable.addCell(addressCell);
                
                PdfPCell phoneCell = new PdfPCell(new Phrase("رقم الهاتف: " + (settings.getPhone() != null ? settings.getPhone() : "---"), font));
                phoneCell.setBorder(Rectangle.NO_BORDER);
                infoTable.addCell(phoneCell);

                PdfPCell leftHeaderCell = new PdfPCell(infoTable);
                leftHeaderCell.setBorder(Rectangle.NO_BORDER);
                headerTable.addCell(leftHeaderCell);

                // الشعار (يسار)
                PdfPCell logoCell = new PdfPCell();
                logoCell.setBorder(Rectangle.NO_BORDER);
                logoCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                String logoPath = settings.getLogo_path();
                if (logoPath != null && !logoPath.trim().isEmpty()) {
                    try {
                        Image logo = Image.getInstance(logoPath);
                        logo.scaleToFit(80, 80); // تحجيم الشعار ليكون متناسقاً
                        logoCell.addElement(logo);
                    } catch (Exception e) {
                        System.err.println("تعذر تحميل صورة الشعار للـ PDF: " + e.getMessage());
                    }
                }
                headerTable.addCell(logoCell);
                
                document.add(headerTable);

                // خط فاصل أنيق
                PdfPTable lineTable = new PdfPTable(1);
                lineTable.setWidthPercentage(100);
                PdfPCell lineCell = new PdfPCell();
                lineCell.setBorder(Rectangle.BOTTOM);
                lineCell.setBorderWidthBottom(1.5f);
                lineCell.setBorderColorBottom(new BaseColor(189, 195, 199)); // لون رمادي
                lineCell.setPaddingTop(10);
                lineCell.setPaddingBottom(15);
                lineTable.addCell(lineCell);
                document.add(lineTable);

                // --- 2. عنوان الفاتورة الأساسي ---
                PdfPTable titleDocTable = new PdfPTable(1);
                titleDocTable.setWidthPercentage(100);
                titleDocTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL); 
                PdfPCell titleDocCell = new PdfPCell(new Phrase(title, titleFont));
                titleDocCell.setBorder(Rectangle.NO_BORDER);
                titleDocCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                titleDocCell.setPaddingBottom(15);
                titleDocTable.addCell(titleDocCell);
                document.add(titleDocTable);

                // --- 3. جدول البيانات ---
                PdfPTable pdfTable = new PdfPTable(table.getColumns().size());
                pdfTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL); 
                pdfTable.setWidthPercentage(100);

                for (TableColumn<T, ?> col : table.getColumns()) {
                    PdfPCell cell = new PdfPCell(new Phrase(col.getText(), boldFont));
                    cell.setBackgroundColor(new BaseColor(230, 230, 230));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    cell.setPadding(8);
                    cell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
                    pdfTable.addCell(cell);
                }

                for (T item : table.getItems()) {
                    for (TableColumn<T, ?> col : table.getColumns()) {
                        String cellValue = extractCellValue(col, item);
                        PdfPCell cell = new PdfPCell(new Phrase(cellValue, font));
                        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        cell.setPadding(6);
                        cell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
                        pdfTable.addCell(cell);
                    }
                }

                document.add(pdfTable);
                document.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static <T> String extractCellValue(TableColumn<T, ?> col, T item) {
        try {
            ObservableValue<?> obsValue = col.getCellObservableValue(item);
            if (obsValue != null && obsValue.getValue() != null) {
                if (obsValue.getValue() instanceof javafx.scene.Node) return ""; 
                return obsValue.getValue().toString();
            }
        } catch (Exception e) {}
        return "";
    }
}