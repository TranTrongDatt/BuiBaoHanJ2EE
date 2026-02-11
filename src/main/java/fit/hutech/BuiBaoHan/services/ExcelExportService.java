package fit.hutech.BuiBaoHan.services;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import fit.hutech.BuiBaoHan.repositories.IBookRepository;
import fit.hutech.BuiBaoHan.repositories.IBorrowRecordRepository;
import fit.hutech.BuiBaoHan.repositories.ICategoryRepository;
import fit.hutech.BuiBaoHan.repositories.IFieldRepository;
import fit.hutech.BuiBaoHan.repositories.ILibraryCardRepository;
import fit.hutech.BuiBaoHan.repositories.IOrderRepository;
import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;

/**
 * Service for exporting data to Excel files
 */
@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private final IUserRepository userRepository;
    private final IBookRepository bookRepository;
    private final IOrderRepository orderRepository;
    private final ICategoryRepository categoryRepository;
    private final IFieldRepository fieldRepository;
    private final ILibraryCardRepository libraryCardRepository;
    private final IBorrowRecordRepository borrowRecordRepository;
    private final OrderService orderService;

    /**
     * Export dashboard statistics to Excel file
     */
    public byte[] exportDashboardToExcel() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);

            // Sheet 1: Overview Statistics
            createOverviewSheet(workbook, headerStyle, titleStyle, dataStyle, numberStyle);

            // Sheet 2: Category Statistics
            createCategorySheet(workbook, headerStyle, titleStyle, dataStyle, numberStyle);

            // Sheet 3: Field Statistics
            createFieldSheet(workbook, headerStyle, titleStyle, dataStyle, numberStyle);

            // Sheet 4: Library Statistics
            createLibrarySheet(workbook, headerStyle, titleStyle, dataStyle, numberStyle);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createOverviewSheet(Workbook workbook, CellStyle headerStyle, CellStyle titleStyle, 
                                      CellStyle dataStyle, CellStyle numberStyle) {
        Sheet sheet = workbook.createSheet("Tổng quan");
        sheet.setColumnWidth(0, 8000);
        sheet.setColumnWidth(1, 5000);

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("BÁO CÁO THỐNG KÊ HỆ THỐNG MINIVERSE");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        // Date
        Row dateRow = sheet.createRow(rowNum++);
        Cell dateCell = dateRow.createCell(0);
        dateCell.setCellValue("Ngày xuất báo cáo: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        dateCell.setCellStyle(dataStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 1));

        rowNum++; // Empty row

        // Header
        Row headerRow = sheet.createRow(rowNum++);
        createCell(headerRow, 0, "Chỉ số", headerStyle);
        createCell(headerRow, 1, "Giá trị", headerStyle);

        // Data rows
        createDataRow(sheet, rowNum++, "Tổng người dùng", userRepository.count(), dataStyle, numberStyle);
        createDataRow(sheet, rowNum++, "Người dùng hoạt động", userRepository.countActiveUsers(), dataStyle, numberStyle);
        createDataRow(sheet, rowNum++, "Tổng sản phẩm (sách)", bookRepository.count(), dataStyle, numberStyle);
        createDataRow(sheet, rowNum++, "Sách sắp hết (< 5)", bookRepository.countLowStock(5), dataStyle, numberStyle);
        createDataRow(sheet, rowNum++, "Tổng thể loại", categoryRepository.count(), dataStyle, numberStyle);
        createDataRow(sheet, rowNum++, "Tổng lĩnh vực", fieldRepository.count(), dataStyle, numberStyle);
        createDataRow(sheet, rowNum++, "Tổng đơn hàng", orderRepository.count(), dataStyle, numberStyle);
        createDataRow(sheet, rowNum++, "Đơn chờ xử lý", orderRepository.countPendingOrders(), dataStyle, numberStyle);

        rowNum++; // Empty row

        // Revenue section header
        Row revHeader = sheet.createRow(rowNum++);
        createCell(revHeader, 0, "DOANH THU", headerStyle);
        createCell(revHeader, 1, "VNĐ", headerStyle);

        createDataRow(sheet, rowNum++, "Doanh thu hôm nay", orderService.getTodayRevenue().longValue(), dataStyle, numberStyle);
        createDataRow(sheet, rowNum++, "Doanh thu tháng này", orderService.getMonthRevenue().longValue(), dataStyle, numberStyle);
        createDataRow(sheet, rowNum++, "Tổng doanh thu", orderService.getTotalRevenue().longValue(), dataStyle, numberStyle);
    }

    private void createCategorySheet(Workbook workbook, CellStyle headerStyle, CellStyle titleStyle, 
                                      CellStyle dataStyle, CellStyle numberStyle) {
        Sheet sheet = workbook.createSheet("Thể loại");
        sheet.setColumnWidth(0, 2000);
        sheet.setColumnWidth(1, 8000);
        sheet.setColumnWidth(2, 4000);

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("THỐNG KÊ THEO THỂ LOẠI");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

        rowNum++; // Empty row

        // Header
        Row headerRow = sheet.createRow(rowNum++);
        createCell(headerRow, 0, "STT", headerStyle);
        createCell(headerRow, 1, "Tên thể loại", headerStyle);
        createCell(headerRow, 2, "Số sách", headerStyle);

        // Data
        var categories = categoryRepository.findAll();
        int stt = 1;
        for (var category : categories) {
            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, String.valueOf(stt++), dataStyle);
            createCell(row, 1, category.getName(), dataStyle);
            Cell countCell = row.createCell(2);
            countCell.setCellValue(category.getBooks() != null ? category.getBooks().size() : 0);
            countCell.setCellStyle(numberStyle);
        }

        // Total row
        Row totalRow = sheet.createRow(rowNum);
        createCell(totalRow, 0, "", headerStyle);
        createCell(totalRow, 1, "TỔNG CỘNG", headerStyle);
        Cell totalCell = totalRow.createCell(2);
        totalCell.setCellValue(bookRepository.count());
        totalCell.setCellStyle(headerStyle);
    }

    private void createFieldSheet(Workbook workbook, CellStyle headerStyle, CellStyle titleStyle, 
                                   CellStyle dataStyle, CellStyle numberStyle) {
        Sheet sheet = workbook.createSheet("Lĩnh vực");
        sheet.setColumnWidth(0, 2000);
        sheet.setColumnWidth(1, 8000);
        sheet.setColumnWidth(2, 4000);
        sheet.setColumnWidth(3, 4000);

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("THỐNG KÊ THEO LĨNH VỰC");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        rowNum++; // Empty row

        // Header
        Row headerRow = sheet.createRow(rowNum++);
        createCell(headerRow, 0, "STT", headerStyle);
        createCell(headerRow, 1, "Tên lĩnh vực", headerStyle);
        createCell(headerRow, 2, "Số thể loại", headerStyle);
        createCell(headerRow, 3, "Số sách", headerStyle);

        // Data
        var fields = fieldRepository.findAll();
        int stt = 1;
        for (var field : fields) {
            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, String.valueOf(stt++), dataStyle);
            createCell(row, 1, field.getName(), dataStyle);
            
            int categoryCount = field.getCategories() != null ? field.getCategories().size() : 0;
            Cell catCell = row.createCell(2);
            catCell.setCellValue(categoryCount);
            catCell.setCellStyle(numberStyle);
            
            long bookCount = field.getCategories() != null 
                ? field.getCategories().stream().mapToLong(c -> c.getBooks() != null ? c.getBooks().size() : 0).sum() 
                : 0;
            Cell bookCell = row.createCell(3);
            bookCell.setCellValue(bookCount);
            bookCell.setCellStyle(numberStyle);
        }
    }

    private void createLibrarySheet(Workbook workbook, CellStyle headerStyle, CellStyle titleStyle, 
                                     CellStyle dataStyle, CellStyle numberStyle) {
        Sheet sheet = workbook.createSheet("Thư viện");
        sheet.setColumnWidth(0, 8000);
        sheet.setColumnWidth(1, 5000);

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("THỐNG KÊ THƯ VIỆN");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        rowNum++; // Empty row

        // Header
        Row headerRow = sheet.createRow(rowNum++);
        createCell(headerRow, 0, "Chỉ số", headerStyle);
        createCell(headerRow, 1, "Giá trị", headerStyle);

        // Data
        createDataRow(sheet, rowNum++, "Tổng thẻ thư viện", libraryCardRepository.count(), dataStyle, numberStyle);
        createDataRow(sheet, rowNum++, "Đang mượn sách", borrowRecordRepository.countCurrentlyBorrowed(), dataStyle, numberStyle);
        createDataRow(sheet, rowNum++, "Quá hạn trả sách", borrowRecordRepository.countOverdue(), dataStyle, numberStyle);
    }

    // Helper methods for creating cells and styles
    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createDataRow(Sheet sheet, int rowNum, String label, long value, 
                                CellStyle labelStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowNum);
        createCell(row, 0, label, labelStyle);
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(valueStyle);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.RIGHT);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        return style;
    }
}
