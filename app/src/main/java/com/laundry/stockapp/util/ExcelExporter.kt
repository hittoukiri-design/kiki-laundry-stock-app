package com.laundry.stockapp.util

import android.content.Context
import com.laundry.stockapp.data.model.Item
import com.laundry.stockapp.data.model.Outlet
import com.laundry.stockapp.data.model.Transaction
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import com.laundry.stockapp.data.repository.FirestoreRepository

object ExcelExporter {

    fun exportToExcel(
        context: Context,
        items: List<Item>,
        outlets: List<Outlet>,
        transactions: List<Transaction>,
        firestoreRepository: FirestoreRepository? = null
    ): File? {
        try {
            // Load template from assets
            val assetManager = context.assets
            val inputStream: InputStream = assetManager.open("template_item_outlet.xlsx")
            val workbook = XSSFWorkbook(inputStream)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID"))

            // Sheet: Master Stok
            val sheetItems = workbook.getSheet("Master Stok")
            if (sheetItems != null) {
                // Data starts at row index 4
                items.forEachIndexed { index, item ->
                    val rowIdx = 4 + index
                    var row = sheetItems.getRow(rowIdx)
                    if (row == null) row = sheetItems.createRow(rowIdx)

                    val cell0 = row.getCell(0) ?: row.createCell(0)
                    cell0.setCellValue(item.name.orEmpty())

                    val cell1 = row.getCell(1) ?: row.createCell(1)
                    cell1.setCellValue((item.startingStock ?: 0).toDouble())

                    val cell2 = row.getCell(2) ?: row.createCell(2)
                    cell2.setCellValue((item.totalOut ?: 0).toDouble())

                    val cell3 = row.getCell(3) ?: row.createCell(3)
                    cell3.setCellValue(item.remainingStock.toDouble())
                }
            }

            // Sheet: Input Transaksi
            val sheetTrans = workbook.getSheet("Input Transaksi")
            if (sheetTrans != null) {
                // Data starts at row index 4
                transactions.sortedBy { it.date ?: Date() }.forEachIndexed { index, trans ->
                    val rowIdx = 4 + index
                    var row = sheetTrans.getRow(rowIdx)
                    if (row == null) row = sheetTrans.createRow(rowIdx)

                    // Tanggal
                    val cell0 = row.getCell(0) ?: row.createCell(0)
                    cell0.setCellValue(sdf.format(trans.date ?: Date()))

                    // Outlet
                    val cell1 = row.getCell(1) ?: row.createCell(1)
                    cell1.setCellValue(trans.outletName.orEmpty())

                    // Skip col 2 (Wilayah) as it contains formula in template

                    // Item
                    val cell3 = row.getCell(3) ?: row.createCell(3)
                    cell3.setCellValue(trans.itemName.orEmpty())

                    // Qty Keluar
                    val cell4 = row.getCell(4) ?: row.createCell(4)
                    cell4.setCellValue((trans.qtyOut ?: 0).toDouble())

                    // Catatan
                    val cell5 = row.getCell(5) ?: row.createCell(5)
                    cell5.setCellValue(trans.notes.orEmpty())
                }
            }

            // Sheet: Pemeliharaan & Ceklist (Dynamic Sheet for Maintenance Backup)
            if (firestoreRepository != null) {
                // Setup fonts
                val titleFont = workbook.createFont().apply {
                    fontName = "Arial"
                    fontHeightInPoints = 11
                    bold = true
                    color = org.apache.poi.ss.usermodel.IndexedColors.WHITE.index
                }
                
                val subtitleFont = workbook.createFont().apply {
                    fontName = "Arial"
                    fontHeightInPoints = 9
                    italic = true
                    color = org.apache.poi.ss.usermodel.IndexedColors.GREEN.index
                }
                
                val headerFont = workbook.createFont().apply {
                    fontName = "Arial"
                    fontHeightInPoints = 10
                    bold = true
                    color = org.apache.poi.ss.usermodel.IndexedColors.WHITE.index
                }
                
                val bodyFont = workbook.createFont().apply {
                    fontName = "Arial"
                    fontHeightInPoints = 10
                }

                // Setup cell styles
                val titleStyle = workbook.createCellStyle().apply {
                    setFont(titleFont)
                    alignment = org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER
                    verticalAlignment = org.apache.poi.ss.usermodel.VerticalAlignment.CENTER
                    fillForegroundColor = org.apache.poi.ss.usermodel.IndexedColors.SEA_GREEN.index
                    fillPattern = org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
                    borderLeft = org.apache.poi.ss.usermodel.BorderStyle.THIN
                    borderRight = org.apache.poi.ss.usermodel.BorderStyle.THIN
                    borderTop = org.apache.poi.ss.usermodel.BorderStyle.THIN
                    borderBottom = org.apache.poi.ss.usermodel.BorderStyle.THIN
                }
                
                val subtitleStyle = workbook.createCellStyle().apply {
                    setFont(subtitleFont)
                    alignment = org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER
                    verticalAlignment = org.apache.poi.ss.usermodel.VerticalAlignment.CENTER
                    fillForegroundColor = org.apache.poi.ss.usermodel.IndexedColors.LIGHT_TURQUOISE.index
                    fillPattern = org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
                    borderLeft = org.apache.poi.ss.usermodel.BorderStyle.THIN
                    borderRight = org.apache.poi.ss.usermodel.BorderStyle.THIN
                    borderTop = org.apache.poi.ss.usermodel.BorderStyle.THIN
                    borderBottom = org.apache.poi.ss.usermodel.BorderStyle.THIN
                }
                
                val headerStyle = workbook.createCellStyle().apply {
                    setFont(headerFont)
                    alignment = org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER
                    verticalAlignment = org.apache.poi.ss.usermodel.VerticalAlignment.CENTER
                    fillForegroundColor = org.apache.poi.ss.usermodel.IndexedColors.CORNFLOWER_BLUE.index
                    fillPattern = org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
                    borderLeft = org.apache.poi.ss.usermodel.BorderStyle.THIN
                    borderRight = org.apache.poi.ss.usermodel.BorderStyle.THIN
                    borderTop = org.apache.poi.ss.usermodel.BorderStyle.THIN
                    borderBottom = org.apache.poi.ss.usermodel.BorderStyle.THIN
                }
                
                val bodyStyleLeft = workbook.createCellStyle().apply {
                    setFont(bodyFont)
                    alignment = org.apache.poi.ss.usermodel.HorizontalAlignment.LEFT
                    verticalAlignment = org.apache.poi.ss.usermodel.VerticalAlignment.CENTER
                    borderLeft = org.apache.poi.ss.usermodel.BorderStyle.THIN
                    borderRight = org.apache.poi.ss.usermodel.BorderStyle.THIN
                    borderTop = org.apache.poi.ss.usermodel.BorderStyle.THIN
                    borderBottom = org.apache.poi.ss.usermodel.BorderStyle.THIN
                }
                
                val bodyStyleCenter = workbook.createCellStyle().apply {
                    setFont(bodyFont)
                    alignment = org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER
                    verticalAlignment = org.apache.poi.ss.usermodel.VerticalAlignment.CENTER
                    borderLeft = org.apache.poi.ss.usermodel.BorderStyle.THIN
                    borderRight = org.apache.poi.ss.usermodel.BorderStyle.THIN
                    borderTop = org.apache.poi.ss.usermodel.BorderStyle.THIN
                    borderBottom = org.apache.poi.ss.usermodel.BorderStyle.THIN
                }

                // Status Styles
                val statusGreenStyle = workbook.createCellStyle().apply {
                    setFont(workbook.createFont().apply {
                        fontName = "Arial"
                        fontHeightInPoints = 9
                        bold = true
                        color = org.apache.poi.ss.usermodel.IndexedColors.DARK_GREEN.index
                    })
                    alignment = org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER
                    verticalAlignment = org.apache.poi.ss.usermodel.VerticalAlignment.CENTER
                    fillForegroundColor = org.apache.poi.ss.usermodel.IndexedColors.LIGHT_GREEN.index
                    fillPattern = org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
                    borderLeft = org.apache.poi.ss.usermodel.BorderStyle.THIN
                    borderRight = org.apache.poi.ss.usermodel.BorderStyle.THIN
                    borderTop = org.apache.poi.ss.usermodel.BorderStyle.THIN
                    borderBottom = org.apache.poi.ss.usermodel.BorderStyle.THIN
                }
                
                val statusYellowStyle = workbook.createCellStyle().apply {
                    setFont(workbook.createFont().apply {
                        fontName = "Arial"
                        fontHeightInPoints = 9
                        bold = true
                        color = org.apache.poi.ss.usermodel.IndexedColors.DARK_YELLOW.index
                    })
                    alignment = org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER
                    verticalAlignment = org.apache.poi.ss.usermodel.VerticalAlignment.CENTER
                    fillForegroundColor = org.apache.poi.ss.usermodel.IndexedColors.LIGHT_YELLOW.index
                    fillPattern = org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
                    borderLeft = org.apache.poi.ss.usermodel.BorderStyle.THIN
                    borderRight = org.apache.poi.ss.usermodel.BorderStyle.THIN
                    borderTop = org.apache.poi.ss.usermodel.BorderStyle.THIN
                    borderBottom = org.apache.poi.ss.usermodel.BorderStyle.THIN
                }
                
                val statusRedStyle = workbook.createCellStyle().apply {
                    setFont(workbook.createFont().apply {
                        fontName = "Arial"
                        fontHeightInPoints = 9
                        bold = true
                        color = org.apache.poi.ss.usermodel.IndexedColors.RED.index
                    })
                    alignment = org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER
                    verticalAlignment = org.apache.poi.ss.usermodel.VerticalAlignment.CENTER
                    fillForegroundColor = org.apache.poi.ss.usermodel.IndexedColors.ROSE.index
                    fillPattern = org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
                    borderLeft = org.apache.poi.ss.usermodel.BorderStyle.THIN
                    borderRight = org.apache.poi.ss.usermodel.BorderStyle.THIN
                    borderTop = org.apache.poi.ss.usermodel.BorderStyle.THIN
                    borderBottom = org.apache.poi.ss.usermodel.BorderStyle.THIN
                }

                outlets.forEach { outlet ->
                    val sheetName = outlet.name.orEmpty()
                    // Resilient sheet lookup
                    val sheet = workbook.getSheet(sheetName) 
                        ?: workbook.getSheet("JCL $sheetName")
                        ?: workbook.getSheet(sheetName.replace("JCL ", ""))
                        
                    if (sheet != null) {
                        sheet.setDisplayGridlines(true) // Ensure gridlines are visible

                        // Fetch maintenance data from database
                        val mItems = kotlinx.coroutines.runBlocking {
                            try { firestoreRepository.getMaintenanceItems(outlet.id) } catch (e: Exception) { emptyList() }
                        }
                        val regulator = kotlinx.coroutines.runBlocking {
                            try { firestoreRepository.getRegulatorCheck(outlet.id) } catch (e: Exception) { null }
                        }
                        val apar = kotlinx.coroutines.runBlocking {
                            try { firestoreRepository.getAparCheck(outlet.id) } catch (e: Exception) { null }
                        }

                        // 1. Draw Title Banner (Row 1, columns I to N, indices 8 to 13)
                        val row0 = sheet.getRow(0) ?: sheet.createRow(0)
                        row0.heightInPoints = 24f
                        
                        // Merge columns I to N in Row 1
                        sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(0, 0, 8, 13))
                        val titleCell = row0.getCell(8) ?: row0.createCell(8)
                        titleCell.setCellValue("CEKLIST & PEMELIHARAAN ALAT OUTLET")
                        titleCell.cellStyle = titleStyle
                        
                        for (col in 8..13) {
                            val cell = row0.getCell(col) ?: row0.createCell(col)
                            cell.cellStyle = titleStyle
                        }

                        // 2. Draw Subtitle (Row 2, columns I to N)
                        val row1 = sheet.getRow(1) ?: sheet.createRow(1)
                        row1.heightInPoints = 18f
                        sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(1, 1, 8, 13))
                        val subCell = row1.getCell(8) ?: row1.createCell(8)
                        subCell.setCellValue("Status kelayakan mesin, regulator gas, dan tabung APAR di outlet ini")
                        subCell.cellStyle = subtitleStyle
                        
                        for (col in 8..13) {
                            val cell = row1.getCell(col) ?: row1.createCell(col)
                            cell.cellStyle = subtitleStyle
                        }

                        // 3. Spacing / Borders for Row 3
                        val row2 = sheet.getRow(2) ?: sheet.createRow(2)
                        row2.heightInPoints = 12f
                        for (col in 8..13) {
                            val cell = row2.getCell(col) ?: row2.createCell(col)
                            cell.cellStyle = bodyStyleCenter
                        }

                        // 4. Draw Headers (Row 4, columns I to N)
                        val row3 = sheet.getRow(3) ?: sheet.createRow(3)
                        row3.heightInPoints = 26f
                        val headers = listOf("Kategori", "Nama Item/Alat", "Parameter/Detail", "Terakhir Dicek", "Jatuh Tempo", "Status Kelayakan")
                        headers.forEachIndexed { colIdx, header ->
                            val cell = row3.getCell(8 + colIdx) ?: row3.createCell(8 + colIdx)
                            cell.setCellValue(header)
                            cell.cellStyle = headerStyle
                        }

                        // 5. Populate Rows
                        var rowIdx = 4

                        // A. Weekly items
                        val defaultWeeklyItems = if (mItems.isEmpty()) {
                            listOf("Kipas Angin", "Kebersihan Area Belakang Mesin", "Rolling Door / Pintu Depan", "Bagian Bawah Mesin")
                        } else emptyList()

                        val itemsToDraw = if (mItems.isEmpty()) {
                            defaultWeeklyItems.map { name ->
                                Triple("Ceklist Mingguan", name, "Belum Dicek")
                            }
                        } else {
                            mItems.map { item ->
                                val isCheckedRecently = item.lastMaintenanceAt != null &&
                                        (Date().time - item.lastMaintenanceAt.time < 7 * 24 * 60 * 60 * 1000)
                                val lastCheckStr = item.lastMaintenanceAt?.let { sdf.format(it) } ?: "Belum pernah dicek"
                                val statusStr = if (isCheckedRecently) "Selesai dicek" else "Belum Dicek"
                                Triple("Ceklist Mingguan", item.name.orEmpty(), statusStr)
                            }
                        }

                        itemsToDraw.forEach { (cat, name, status) ->
                            val r = sheet.getRow(rowIdx) ?: sheet.createRow(rowIdx)
                            r.heightInPoints = 20f
                            
                            val c0 = r.getCell(8) ?: r.createCell(8)
                            c0.setCellValue(cat)
                            c0.cellStyle = bodyStyleCenter
                            
                            val c1 = r.getCell(9) ?: r.createCell(9)
                            c1.setCellValue(name)
                            c1.cellStyle = bodyStyleLeft
                            
                            val c2 = r.getCell(10) ?: r.createCell(10)
                            c2.setCellValue("-")
                            c2.cellStyle = bodyStyleCenter
                            
                            val c3 = r.getCell(11) ?: r.createCell(11)
                            val lastCheck = if (mItems.isEmpty()) "Belum pernah dicek" else {
                                mItems.find { it.name == name }?.lastMaintenanceAt?.let { sdf.format(it) } ?: "Belum pernah dicek"
                            }
                            c3.setCellValue(lastCheck)
                            c3.cellStyle = bodyStyleCenter
                            
                            val c4 = r.getCell(12) ?: r.createCell(12)
                            c4.setCellValue("-")
                            c4.cellStyle = bodyStyleCenter
                            
                            val c5 = r.getCell(13) ?: r.createCell(13)
                            c5.setCellValue(status)
                            c5.cellStyle = if (status == "Selesai dicek") statusGreenStyle else statusRedStyle
                            
                            rowIdx++
                        }

                        // B. Regulator Gas
                        val rReg = sheet.getRow(rowIdx) ?: sheet.createRow(rowIdx)
                        rReg.heightInPoints = 20f
                        
                        rReg.createCell(8).apply { setCellValue("Uji Gas"); cellStyle = bodyStyleCenter }
                        rReg.createCell(9).apply { setCellValue("Regulator Gas (Sunlight)"); cellStyle = bodyStyleLeft }
                        rReg.createCell(10).apply { setCellValue("Uji air sabun"); cellStyle = bodyStyleCenter }
                        
                        val regLastCheck = if (regulator != null && regulator.lastTestDay != null && regulator.lastTestMonth != null && regulator.lastTestYear != null) {
                            val cal = Calendar.getInstance()
                            cal.set(regulator.lastTestYear, regulator.lastTestMonth - 1, regulator.lastTestDay)
                            sdf.format(cal.time)
                        } else "Belum pernah diuji"
                        rReg.createCell(11).apply { setCellValue(regLastCheck); cellStyle = bodyStyleCenter }
                        rReg.createCell(12).apply { setCellValue("-"); cellStyle = bodyStyleCenter }
                        
                        val regStatus = if (regulator != null) "Uji Gas Sukses" else "Belum Diuji"
                        rReg.createCell(13).apply {
                            setCellValue(regStatus)
                            cellStyle = if (regulator != null) statusGreenStyle else statusRedStyle
                        }
                        rowIdx++

                        // C. APAR
                        val rApar = sheet.getRow(rowIdx) ?: sheet.createRow(rowIdx)
                        rApar.heightInPoints = 20f
                        
                        rApar.createCell(8).apply { setCellValue("APAR"); cellStyle = bodyStyleCenter }
                        rApar.createCell(9).apply { setCellValue("Tabung APAR"); cellStyle = bodyStyleLeft }
                        
                        val aparDetail = if (apar != null) "Masa berlaku: ${apar.intervalMonths} bulan" else "Masa berlaku: 36 bulan"
                        rApar.createCell(10).apply { setCellValue(aparDetail); cellStyle = bodyStyleCenter }
                        
                        val aparLast = if (apar?.lastRefillDate != null) sdf.format(apar.lastRefillDate) else "Belum pernah diisi"
                        rApar.createCell(11).apply { setCellValue(aparLast); cellStyle = bodyStyleCenter }
                        
                        val aparDue = if (apar?.lastRefillDate != null) {
                            val cal = Calendar.getInstance()
                            cal.time = apar.lastRefillDate
                            cal.add(Calendar.MONTH, apar.intervalMonths ?: 36)
                            sdf.format(cal.time)
                        } else "-"
                        rApar.createCell(12).apply { setCellValue(aparDue); cellStyle = bodyStyleCenter }
                        
                        val aparStatus = if (apar != null && apar.lastRefillDate != null) {
                            val cal = Calendar.getInstance()
                            cal.time = apar.lastRefillDate
                            cal.add(Calendar.MONTH, apar.intervalMonths ?: 36)
                            val diffTime = cal.time.time - Date().time
                            val diffDays = (diffTime.toDouble() / (1000 * 60 * 60 * 24)).toInt()
                            if (diffDays < 0) {
                                "Kedalwarsa. (Terlewat ${-diffDays} hari)"
                            } else if (diffDays <= 30) {
                                "Peringatan. (Jatuh tempo $diffDays hari)"
                            } else {
                                "Aman (Sisa $diffDays hari)"
                            }
                        } else "Belum Dikonfigurasi"
                        
                        rApar.createCell(13).apply {
                            setCellValue(aparStatus)
                            cellStyle = when {
                                "Aman" in aparStatus -> statusGreenStyle
                                "Peringatan" in aparStatus -> statusYellowStyle
                                else -> statusRedStyle
                            }
                        }
                        rowIdx++

                        // Set explicit widths for columns I to N for best layout
                        sheet.setColumnWidth(8, 16 * 256)
                        sheet.setColumnWidth(9, 30 * 256)
                        sheet.setColumnWidth(10, 24 * 256)
                        sheet.setColumnWidth(11, 20 * 256)
                        sheet.setColumnWidth(12, 16 * 256)
                        sheet.setColumnWidth(13, 32 * 256)
                    }
                }
            }

            // Force formula recalculation on open
            workbook.setForceFormulaRecalculation(true)

            val fileName = "Laporan_Stok_LONDRI_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.xlsx"
            val cacheDir = File(context.cacheDir, "exports")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            
            val file = File(cacheDir, fileName)
            val fileOut = FileOutputStream(file)
            workbook.write(fileOut)
            fileOut.close()
            workbook.close()
            inputStream.close()

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
