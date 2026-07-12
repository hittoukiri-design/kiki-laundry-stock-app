package com.laundry.stockapp

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Test
import java.io.FileInputStream
import java.io.File

class ExcelReaderTest {
    @Test
    fun readExcel() {
        val path = "src/main/assets/template_item_outlet.xlsx"
        println("Path: ${File(path).absolutePath}")
        val file = FileInputStream(path)
        val workbook = XSSFWorkbook(file)

        for (i in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(i)
            println("Sheet: ${sheet.sheetName}")
            for (rowIdx in 0..4) {
                val row = sheet.getRow(rowIdx)
                if (row != null) {
                    val cellValues = mutableListOf<String>()
                    for (cellIdx in 0 until 10) {
                        val cell = row.getCell(cellIdx)
                        if (cell != null) {
                            cellValues.add(cell.toString())
                        } else {
                            cellValues.add("null")
                        }
                    }
                    println(cellValues.joinToString(" | "))
                } else {
                    println("Row is null")
                }
            }
            println("---")
        }
        workbook.close()
        file.close()
    }
}
