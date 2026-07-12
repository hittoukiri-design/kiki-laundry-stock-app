@file:DependsOn("org.apache.poi:poi-ooxml:5.2.5")

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileInputStream

val file = FileInputStream("/Users/christambayong/Downloads/20260616 - ITEM OUTLET.xlsx")
val workbook = XSSFWorkbook(file)

for (i in 0 until workbook.numberOfSheets) {
    val sheet = workbook.getSheetAt(i)
    println("Sheet: ${sheet.sheetName}")
    val rowCount = sheet.physicalNumberOfRows
    println("Total rows: $rowCount")
    for (rowIdx in 0 until minOf(rowCount, 5)) {
        val row = sheet.getRow(rowIdx)
        if (row != null) {
            val cellValues = mutableListOf<String>()
            for (cellIdx in 0 until row.lastCellNum) {
                val cell = row.getCell(cellIdx)
                cellValues.add(cell?.toString() ?: "null")
            }
            println(cellValues.joinToString(" | "))
        }
    }
    println("---")
}
workbook.close()
file.close()
