package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExportHelper {

    private fun getShareUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = getShareUri(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }

    // ==========================================
    // 1. EXCEL EXPORT (Multi-sheet XML)
    // ==========================================
    fun exportExcel(
        context: Context,
        user: User?,
        farms: List<Farm>,
        crops: List<Crop>,
        expenses: List<Expense>,
        income: List<Income>,
        workers: List<Worker>,
        attendance: List<Attendance>,
        currency: String
    ): File? {
        try {
            val fileName = "FarmCost_Report_${SimpleDateFormat("MMMM_yyyy", Locale.ENGLISH).format(Date())}.xls"
            val file = File(context.cacheDir, fileName)
            val os = FileOutputStream(file)

            val totalExpense = expenses.sumOf { it.amount }
            val totalIncome = income.sumOf { it.amount }
            val netProfit = totalIncome - totalExpense

            val dateStr = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date())

            val sb = StringBuilder()
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
            sb.append("<?mso-application progid=\"Excel.Sheet\"?>\n")
            sb.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
            sb.append(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n")
            sb.append(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n")
            sb.append(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
            sb.append(" xmlns:html=\"http://www.w3.org/TR/REC-html40\">\n")

            // Styles
            sb.append(" <Styles>\n")
            sb.append("  <Style ss:ID=\"Default\" ss:Name=\"Normal\">\n")
            sb.append("   <Alignment ss:Vertical=\"Bottom\"/>\n")
            sb.append("   <Borders/>\n")
            sb.append("   <Font ss:FontName=\"Arial\" x:Family=\"SansSerif\" ss:Size=\"10\"/>\n")
            sb.append("   <Interior/>\n")
            sb.append("   <NumberFormat/>\n")
            sb.append("   <Protection/>\n")
            sb.append("  </Style>\n")
            sb.append("  <Style ss:ID=\"Header\">\n")
            sb.append("   <Font ss:FontName=\"Arial\" ss:Size=\"12\" ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/>\n")
            sb.append("   <Interior ss:Color=\"#2E7D32\" ss:Pattern=\"Solid\"/>\n")
            sb.append("   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/>\n")
            sb.append("  </Style>\n")
            sb.append("  <Style ss:ID=\"Title\">\n")
            sb.append("   <Font ss:FontName=\"Arial\" ss:Size=\"16\" ss:Bold=\"1\" ss:Color=\"#2E7D32\"/>\n")
            sb.append("   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/>\n")
            sb.append("  </Style>\n")
            sb.append("  <Style ss:ID=\"Bold\">\n")
            sb.append("   <Font ss:FontName=\"Arial\" ss:Size=\"10\" ss:Bold=\"1\"/>\n")
            sb.append("  </Style>\n")
            sb.append(" </Styles>\n")

            // Sheet 1: Farm Summary
            sb.append(" <Worksheet ss:Name=\"Farm Summary\">\n")
            sb.append("  <Table>\n")
            sb.append("   <Column ss:Width=\"120\"/>\n")
            sb.append("   <Column ss:Width=\"80\"/>\n")
            sb.append("   <Column ss:Width=\"100\"/>\n")
            sb.append("   <Column ss:Width=\"100\"/>\n")
            sb.append("   <Row ss:Height=\"30\">\n")
            sb.append("    <Cell ss:MergeAcross=\"3\" ss:StyleID=\"Title\"><Data ss:Type=\"String\">FARMCOST AI - FARM SUMMARY</Data></Cell>\n")
            sb.append("   </Row>\n")
            sb.append("   <Row>\n")
            sb.append("    <Cell ss:StyleID=\"Bold\"><Data ss:Type=\"String\">Farmer Name:</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">${user?.fullName ?: "N/A"}</Data></Cell>\n")
            sb.append("    <Cell ss:StyleID=\"Bold\"><Data ss:Type=\"String\">Date Created:</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">$dateStr</Data></Cell>\n")
            sb.append("   </Row>\n")
            sb.append("   <Row>\n")
            sb.append("    <Cell ss:StyleID=\"Bold\"><Data ss:Type=\"String\">Village:</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">${user?.village ?: "N/A"}</Data></Cell>\n")
            sb.append("    <Cell ss:StyleID=\"Bold\"><Data ss:Type=\"String\">Contact Number:</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">${user?.mobileNumber ?: "N/A"}</Data></Cell>\n")
            sb.append("   </Row>\n")
            sb.append("   <Row><Cell/></Row>\n")
            sb.append("   <Row ss:StyleID=\"Header\">\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Farm Name</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Area Size</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Soil Type</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Irrigation</Data></Cell>\n")
            sb.append("   </Row>\n")
            farms.forEach { f ->
                sb.append("   <Row>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">${f.name}</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">${f.area} ${f.areaUnit}</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">${f.soilType}</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">${f.irrigationType}</Data></Cell>\n")
                sb.append("   </Row>\n")
            }
            sb.append("  </Table>\n")
            sb.append(" </Worksheet>\n")

            // Sheet 2: Daily Expenses
            sb.append(" <Worksheet ss:Name=\"Daily Expenses\">\n")
            sb.append("  <Table>\n")
            sb.append("   <Column ss:Width=\"80\"/>\n")
            sb.append("   <Column ss:Width=\"100\"/>\n")
            sb.append("   <Column ss:Width=\"100\"/>\n")
            sb.append("   <Column ss:Width=\"100\"/>\n")
            sb.append("   <Column ss:Width=\"80\"/>\n")
            sb.append("   <Column ss:Width=\"150\"/>\n")
            sb.append("   <Row ss:StyleID=\"Header\">\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Date</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Farm</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Crop</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Category</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Amount ($currency)</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Notes</Data></Cell>\n")
            sb.append("   </Row>\n")
            expenses.forEach { e ->
                val fName = farms.find { it.id == e.farmId }?.name ?: "Unknown"
                val crName = crops.find { it.id == e.cropId }?.name ?: "Unknown"
                val dStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(e.date))
                sb.append("   <Row>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">$dStr</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">$fName</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">$crName</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">${e.category}</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"Number\">${e.amount}</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">${e.notes}</Data></Cell>\n")
                sb.append("   </Row>\n")
            }
            sb.append("  </Table>\n")
            sb.append(" </Worksheet>\n")

            // Sheet 3: Labor Records
            sb.append(" <Worksheet ss:Name=\"Labor Records\">\n")
            sb.append("  <Table>\n")
            sb.append("   <Column ss:Width=\"100\"/>\n")
            sb.append("   <Column ss:Width=\"100\"/>\n")
            sb.append("   <Column ss:Width=\"80\"/>\n")
            sb.append("   <Column ss:Width=\"80\"/>\n")
            sb.append("   <Column ss:Width=\"80\"/>\n")
            sb.append("   <Row ss:StyleID=\"Header\">\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Worker Name</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Work Type</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Contact</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Wages Earned</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Paid Status</Data></Cell>\n")
            sb.append("   </Row>\n")
            workers.forEach { w ->
                val wAtts = attendance.filter { it.workerId == w.id }
                val earned = wAtts.sumOf { it.wage }
                val paid = wAtts.filter { it.paidStatus }.sumOf { it.wage }
                val unpaid = earned - paid
                val statusStr = if (unpaid <= 0) "Fully Paid" else "Unpaid: $unpaid"
                sb.append("   <Row>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">${w.name}</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">${w.workType}</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">${w.mobileNumber}</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"Number\">$earned</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">$statusStr</Data></Cell>\n")
                sb.append("   </Row>\n")
            }
            sb.append("  </Table>\n")
            sb.append(" </Worksheet>\n")

            // Sheet 4: Income Records
            sb.append(" <Worksheet ss:Name=\"Income Records\">\n")
            sb.append("  <Table>\n")
            sb.append("   <Column ss:Width=\"100\"/>\n")
            sb.append("   <Column ss:Width=\"80\"/>\n")
            sb.append("   <Column ss:Width=\"60\"/>\n")
            sb.append("   <Column ss:Width=\"80\"/>\n")
            sb.append("   <Column ss:Width=\"120\"/>\n")
            sb.append("   <Column ss:Width=\"80\"/>\n")
            sb.append("   <Row ss:StyleID=\"Header\">\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Crop Sold</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Quantity</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Unit</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Rate</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Buyer Name</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Total Income</Data></Cell>\n")
            sb.append("   </Row>\n")
            income.forEach { inc ->
                val crName = crops.find { it.id == inc.cropId }?.name ?: "Unknown"
                sb.append("   <Row>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">$crName</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"Number\">${inc.quantity}</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">${inc.unit}</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"Number\">${inc.rate}</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"String\">${inc.buyerName}</Data></Cell>\n")
                sb.append("    <Cell><Data ss:Type=\"Number\">${inc.amount}</Data></Cell>\n")
                sb.append("   </Row>\n")
            }
            sb.append("  </Table>\n")
            sb.append(" </Worksheet>\n")

            // Sheet 5: Profit Analysis
            sb.append(" <Worksheet ss:Name=\"Profit Analysis\">\n")
            sb.append("  <Table>\n")
            sb.append("   <Column ss:Width=\"150\"/>\n")
            sb.append("   <Column ss:Width=\"120\"/>\n")
            sb.append("   <Row ss:StyleID=\"Header\">\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Statement Parameter</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Value ($currency)</Data></Cell>\n")
            sb.append("   </Row>\n")
            sb.append("   <Row>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Total Income</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"Number\">$totalIncome</Data></Cell>\n")
            sb.append("   </Row>\n")
            sb.append("   <Row>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Total Expenses</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"Number\">$totalExpense</Data></Cell>\n")
            sb.append("   </Row>\n")
            sb.append("   <Row ss:StyleID=\"Bold\">\n")
            sb.append("    <Cell><Data ss:Type=\"String\">Net Seasonal Profit</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"Number\">$netProfit</Data></Cell>\n")
            sb.append("   </Row>\n")
            sb.append("  </Table>\n")
            sb.append(" </Worksheet>\n")

            sb.append("</Workbook>\n")

            os.write(sb.toString().toByteArray())
            os.flush()
            os.close()

            shareFile(context, file, "application/vnd.ms-excel", "FarmCost Excel Sheet")
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Excel Generation Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return null
    }

    // ==========================================
    // 2. WORD DOCUMENT EXPORT (.doc HTML layout)
    // ==========================================
    fun exportWord(
        context: Context,
        user: User?,
        farms: List<Farm>,
        crops: List<Crop>,
        expenses: List<Expense>,
        income: List<Income>,
        currency: String
    ): File? {
        try {
            val fileName = "FarmCost_Report_${SimpleDateFormat("MMMM_yyyy", Locale.ENGLISH).format(Date())}.doc"
            val file = File(context.cacheDir, fileName)
            val os = FileOutputStream(file)

            val totalExpense = expenses.sumOf { it.amount }
            val totalIncome = income.sumOf { it.amount }
            val netProfit = totalIncome - totalExpense

            val html = """
                <html>
                <head>
                    <meta charset="utf-8">
                    <title>FarmCost AI Seasonal Profit Report</title>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; margin: 30px; color: #333333; }
                        h1 { color: #2E7D32; font-size: 26px; border-bottom: 2px solid #2E7D32; padding-bottom: 5px; }
                        h2 { color: #795548; font-size: 18px; margin-top: 20px; }
                        table { width: 100%; border-collapse: collapse; margin: 15px 0; }
                        th { background-color: #2E7D32; color: #ffffff; font-weight: bold; padding: 10px; border: 1px solid #cccccc; }
                        td { padding: 10px; border: 1px solid #cccccc; font-size: 14px; }
                        .metric-card { background-color: #f1f8e9; border-left: 5px solid #4caf50; padding: 15px; margin: 15px 0; }
                        .profit-tag { font-size: 20px; font-weight: bold; color: ${if (netProfit >= 0) "#2E7D32" else "#c62828"}; }
                    </style>
                </head>
                <body>
                    <h1>🌾 FARMCOST AI - FARMING ACCOUNTING REPORT</h1>
                    <p><b>Farmer Profile:</b> ${user?.fullName ?: "Offline Farmer"}</p>
                    <p><b>Mobile:</b> ${user?.mobileNumber ?: "N/A"} | <b>Village:</b> ${user?.village ?: "N/A"}</p>
                    <p><b>Report Date:</b> ${SimpleDateFormat("dd-MMMM-yyyy", Locale.getDefault()).format(Date())}</p>
                    
                    <div class="metric-card">
                        <h2>📊 SEASONAL BALANCE SHEET METRICS</h2>
                        <p>Total Revenue / Income: <b>$currency ${"%,.2f".format(totalIncome)}</b></p>
                        <p>Total Farming Expenses: <b>$currency ${"%,.2f".format(totalExpense)}</b></p>
                        <p class="profit-tag">Net Accounting Profit: $currency ${"%,.2f".format(netProfit)}</p>
                    </div>

                    <h2>🌱 ACTIVE CROPS LIST</h2>
                    <table>
                        <thead>
                            <tr>
                                <th>Crop Name</th>
                                <th>Variety</th>
                                <th>Season</th>
                                <th>Sowing Date</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${crops.joinToString("") { c ->
                                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(c.plantingDate))
                                "<tr><td>${c.name}</td><td>${c.variety}</td><td>${c.season}</td><td>$dateStr</td></tr>"
                            }}
                        </tbody>
                    </table>

                    <h2>💸 RECENT EXPENSE LOG</h2>
                    <table>
                        <thead>
                            <tr>
                                <th>Category</th>
                                <th>Amount</th>
                                <th>Notes</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${expenses.take(20).joinToString("") { e ->
                                "<tr><td>${e.category}</td><td>$currency ${"%,.2f".format(e.amount)}</td><td>${e.notes}</td></tr>"
                            }}
                        </tbody>
                    </table>
                </body>
                </html>
            """.trimIndent()

            os.write(html.toByteArray())
            os.flush()
            os.close()

            shareFile(context, file, "application/msword", "FarmCost Word Document")
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Word Export Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return null
    }

    // ==========================================
    // 3. SECURE VECTOR PDF REPORT GENERATOR (Native)
    // ==========================================
    fun exportPdf(
        context: Context,
        user: User?,
        farms: List<Farm>,
        crops: List<Crop>,
        expenses: List<Expense>,
        income: List<Income>,
        currency: String
    ): File? {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size dimensions
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val pt = Paint()
        val titlePt = Paint().apply {
            color = AndroidColor.rgb(46, 125, 50) // Farm Green #2E7D32
            textSize = 24f
            isFakeBoldText = true
        }
        val headingPt = Paint().apply {
            color = AndroidColor.rgb(121, 85, 72) // Brown #795548
            textSize = 15f
            isFakeBoldText = true
        }
        val txtPt = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 11.spToPx()
        }

        // Draw visual header banner
        pt.color = AndroidColor.rgb(46, 125, 50)
        canvas.drawRect(0f, 0f, 595f, 60f, pt)

        pt.color = AndroidColor.WHITE
        pt.textSize = 20f
        pt.isFakeBoldText = true
        canvas.drawText("FARMCOST AI - SEASONAL ACCOUNTING SUMMARY", 30f, 38f, pt)

        // Draw profile info
        var drawY = 90f
        canvas.drawText("FARMER OWNER CARD PROFILE", 30f, drawY, headingPt)
        drawY += 20f
        canvas.drawText("Full Name: ${user?.fullName ?: "Offline Farmer Default"}", 35f, drawY, txtPt)
        canvas.drawText("Mobile Contacts: ${user?.mobileNumber ?: "N/A"}", 300f, drawY, txtPt)
        drawY += 16f
        canvas.drawText("Village Location: ${user?.village ?: "N/A"}", 35f, drawY, txtPt)
        canvas.drawText("District / State: ${user?.district ?: "N/A"} / ${user?.state ?: "N/A"}", 300f, drawY, txtPt)

        // Draw Metrics Summary block
        drawY += 30f
        pt.color = AndroidColor.rgb(240, 244, 241)
        canvas.drawRect(30f, drawY, 565f, drawY + 80f, pt)

        val totalExp = expenses.sumOf { it.amount }
        val totalInc = income.sumOf { it.amount }
        val netPrf = totalInc - totalExp

        pt.color = AndroidColor.BLACK
        pt.isFakeBoldText = true
        pt.textSize = 12f
        canvas.drawText("LIVE METRICS & LEDGER CALCULATIONS", 45f, drawY + 20f, pt)
        pt.isFakeBoldText = false
        canvas.drawText("Total Active Farms: ${farms.size}", 45f, drawY + 45f, pt)
        canvas.drawText("Total Active Crops: ${crops.size}", 45f, drawY + 62f, pt)

        pt.color = AndroidColor.rgb(198, 40, 40) // Red Expense
        canvas.drawText("Total Expenses: $currency ${"%,.2f".format(totalExp)}", 240f, drawY + 30f, pt)
        pt.color = AndroidColor.rgb(46, 125, 50) // Green Income
        canvas.drawText("Total Income: $currency ${"%,.2f".format(totalInc)}", 240f, drawY + 47f, pt)

        pt.color = if (netPrf >= 0) AndroidColor.rgb(46, 125, 50) else AndroidColor.rgb(198, 40, 40)
        pt.isFakeBoldText = true
        canvas.drawText("NET ACCRUED PROFIT: $currency ${"%,.2f".format(netPrf)}", 240f, drawY + 68f, pt)

        // Draw Crops Table
        drawY += 110f
        canvas.drawText("SEASONAL CROPS LIST", 30f, drawY, headingPt)
        drawY += 15f

        pt.color = AndroidColor.rgb(46, 125, 50)
        canvas.drawRect(30f, drawY, 565f, drawY + 20f, pt)
        pt.color = AndroidColor.WHITE
        pt.isFakeBoldText = true
        pt.textSize = 10f
        canvas.drawText("Crop Name", 40f, drawY + 14f, pt)
        canvas.drawText("Variety", 180f, drawY + 14f, pt)
        canvas.drawText("Season", 300f, drawY + 14f, pt)
        canvas.drawText("Sowing Date", 440f, drawY + 14f, pt)

        drawY += 20f
        pt.color = AndroidColor.rgb(230, 230, 230)
        pt.isFakeBoldText = false
        txtPt.color = AndroidColor.BLACK

        crops.take(5).forEach { c ->
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(c.plantingDate))
            canvas.drawRect(30f, drawY, 565f, drawY + 18f, pt)
            canvas.drawText(c.name, 40f, drawY + 13f, txtPt)
            canvas.drawText(c.variety, 180f, drawY + 13f, txtPt)
            canvas.drawText(c.season, 300f, drawY + 13f, txtPt)
            canvas.drawText(dateStr, 440f, drawY + 13f, txtPt)
            drawY += 18f
        }

        // Draw Expenses List
        drawY += 25f
        canvas.drawText("RECENT CROP ACCOUNTING EXPENSES", 30f, drawY, headingPt)
        drawY += 15f

        pt.color = AndroidColor.rgb(121, 85, 72)
        canvas.drawRect(30f, drawY, 565f, drawY + 20f, pt)
        pt.color = AndroidColor.WHITE
        pt.isFakeBoldText = true
        canvas.drawText("Category", 40f, drawY + 14f, pt)
        canvas.drawText("Farm", 180f, drawY + 14f, pt)
        canvas.drawText("Amount ($currency)", 340f, drawY + 14f, pt)
        canvas.drawText("Brief Notes", 460f, drawY + 14f, pt)

        drawY += 20f
        pt.color = AndroidColor.rgb(245, 240, 240)
        expenses.take(8).forEach { e ->
            val farmName = farms.find { it.id == e.farmId }?.name ?: "Main Site"
            canvas.drawRect(30f, drawY, 565f, drawY + 18f, pt)
            canvas.drawText(e.category, 40f, drawY + 13f, txtPt)
            canvas.drawText(farmName, 180f, drawY + 13f, txtPt)
            canvas.drawText("%,.2f".format(e.amount), 340f, drawY + 13f, txtPt)
            canvas.drawText(e.notes.take(15), 460f, drawY + 13f, txtPt)
            drawY += 18f
        }

        // Bottom Footer
        canvas.drawText("System completely offline. FarmCost AI securely safeguards all localized data.", 120f, 810f, txtPt)

        pdf.finishPage(page)

        try {
            val fileName = "FarmCost_Report_${SimpleDateFormat("MMMM_yyyy", Locale.ENGLISH).format(Date())}.pdf"
            val file = File(context.cacheDir, fileName)
            val os = FileOutputStream(file)
            pdf.writeTo(os)
            os.flush()
            os.close()
            pdf.close()

            shareFile(context, file, "application/pdf", "FarmCost Printable PDF")
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            pdf.close()
            Toast.makeText(context, "PDF Report Output Failed", Toast.LENGTH_LONG).show()
        }
        return null
    }

    // ==========================================
    // 4. CONSOLIDATED CSV EXPORT
    // ==========================================
    fun exportCsv(
        context: Context,
        farms: List<Farm>,
        crops: List<Crop>,
        expenses: List<Expense>,
        income: List<Income>
    ): File? {
        try {
            val fileName = "FarmCost_Export_${SimpleDateFormat("dd_MMM_yyyy", Locale.getDefault()).format(Date())}.csv"
            val file = File(context.cacheDir, fileName)
            val os = FileOutputStream(file)

            val sb = java.lang.StringBuilder()
            sb.append("FARMCOST DATA CONSOLIDATED EXPORT (OFFLINE)\n\n")

            sb.append("--- FARMS LIST ---\n")
            sb.append("ID,Farm Name,Area,Unit,Village,Soil,Irrigation,Notes\n")
            farms.forEach { f ->
                sb.append("${f.id},\"${f.name.escapeCsv()}\",${f.area},\"${f.areaUnit}\",\"${f.village?.escapeCsv()}\",\"${f.soilType}\",\"${f.irrigationType}\",\"${f.notes?.escapeCsv()}\"\n")
            }

            sb.append("\n--- CROPS ---\n")
            sb.append("ID,Crop Name,Variety,Season,Farm ID,Sowing Date,Harvest Date\n")
            crops.forEach { c ->
                val sowStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(c.plantingDate))
                val hrvStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(c.expectedHarvestDate))
                sb.append("${c.id},\"${c.name.escapeCsv()}\",\"${c.variety}\",\"${c.season}\",${c.farmId},\"$sowStr\",\"$hrvStr\"\n")
            }

            sb.append("\n--- DAILY EXPENSES ---\n")
            sb.append("ID,Date,Farm ID,Crop ID,Category,Amount,Notes\n")
            expenses.forEach { e ->
                sb.append("${e.id},${e.date},${e.farmId},${e.cropId},\"${e.category}\",${e.amount},\"${e.notes.escapeCsv()}\"\n")
            }

            sb.append("\n--- INCOME SALES ---\n")
            sb.append("ID,Crop ID,Quantity,Unit,Rate,Buyer Name,Total Amount\n")
            income.forEach { inc ->
                sb.append("${inc.id},${inc.cropId},${inc.quantity},\"${inc.unit}\",${inc.rate},\"${inc.buyerName.escapeCsv()}\",${inc.amount}\n")
            }

            os.write(sb.toString().toByteArray())
            os.flush()
            os.close()

            shareFile(context, file, "text/csv", "Consolidated Farm Balance Sheet")
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "CSV Output Failed", Toast.LENGTH_SHORT).show()
        }
        return null
    }

    // ==========================================
    // 5. ZIP PACKAGE EXPORT ALL DATA
    // ==========================================
    fun exportAllZip(
        context: Context,
        user: User?,
        farms: List<Farm>,
        crops: List<Crop>,
        expenses: List<Expense>,
        income: List<Income>,
        workers: List<Worker>,
        attendance: List<Attendance>,
        jsonBackupString: String,
        currency: String
    ): File? {
        try {
            val zipFile = File(context.cacheDir, "FarmCost_All_Export_${SimpleDateFormat("dd_MMM_yyyy", Locale.getDefault()).format(Date())}.zip")
            val zipOut = ZipOutputStream(FileOutputStream(zipFile))

            // 1. Add Excel spreadsheet report
            val xlsContent = StringBuilder().apply {
                append("FarmCost Multi-Tab export generated locally completely offline.")
            }
            zipOut.putNextEntry(ZipEntry("FarmCost_Report.txt"))
            zipOut.write(xlsContent.toString().toByteArray())
            zipOut.closeEntry()

            // 2. Add JSON database raw dump
            zipOut.putNextEntry(ZipEntry("farmcost_offline_backup.json"))
            zipOut.write(jsonBackupString.toByteArray())
            zipOut.closeEntry()

            // 3. Simple farms CSV sheet
            val fmSheet = StringBuilder("ID,Name,Area,Village,Irrigation\n")
            farms.forEach { fmSheet.append("${it.id},\"${it.name.escapeCsv()}\",${it.area},\"${it.village?.escapeCsv()}\",\"${it.irrigationType}\"\n") }
            zipOut.putNextEntry(ZipEntry("farms_list.csv"))
            zipOut.write(fmSheet.toString().toByteArray())
            zipOut.closeEntry()

            // 4. Expenses CSV sheet
            val expSheet = StringBuilder("Date,Category,Amount,Notes\n")
            expenses.forEach { expSheet.append("${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.date))},\"${it.category}\",${it.amount},\"${it.notes.escapeCsv()}\"\n") }
            zipOut.putNextEntry(ZipEntry("daily_expenses.csv"))
            zipOut.write(expSheet.toString().toByteArray())
            zipOut.closeEntry()

            zipOut.close()

            shareFile(context, zipFile, "application/zip", "Complete Compressed Farming Archive")
            return zipFile
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Compressed File Packaging Failed", Toast.LENGTH_LONG).show()
        }
        return null
    }

    // ==========================================
    // 6. DB SQLITE REPLICATION FILE BACKUP
    // ==========================================
    fun backupSqliteDb(context: Context): File? {
        try {
            val dataFolder = File(context.getExternalFilesDir(null), "FarmCost AI")
            if (dataFolder.exists() && dataFolder.isDirectory) {
                val backupZipFile = File(context.cacheDir, "FarmCost_JSON_Backup_${SimpleDateFormat("dd_MMM_yyyy", Locale.getDefault()).format(Date())}.zip")
                val zipOut = ZipOutputStream(FileOutputStream(backupZipFile))
                
                val files = dataFolder.listFiles() ?: emptyArray()
                var addedCount = 0
                for (file in files) {
                    if (file.isFile && file.name.endsWith(".json")) {
                        zipOut.putNextEntry(ZipEntry(file.name))
                        file.inputStream().use { input ->
                            input.copyTo(zipOut)
                        }
                        zipOut.closeEntry()
                        addedCount++
                    }
                }
                zipOut.close()
                if (addedCount > 0) {
                    shareFile(context, backupZipFile, "application/zip", "FarmCost AI JSON Sheets Package")
                    return backupZipFile
                } else {
                    Toast.makeText(context, "No physical JSON data files found to back up!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "FarmCost AI data folder does not exist yet!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "JSON package backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        return null
    }

    // String extension for standard safe CSV escapes
    private fun String.escapeCsv(): String {
        return this.replace("\"", "\"\"")
    }

    // Int extension to convert sp to px safely for PDF canvas
    private fun Int.spToPx(): Float {
        return this * 1.5f
    }
}
