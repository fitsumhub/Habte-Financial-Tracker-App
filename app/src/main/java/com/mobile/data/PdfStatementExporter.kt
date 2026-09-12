package com.mobile.data

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfStatementExporter {

    fun generatePdf(
        context: Context,
        transactions: List<Transaction>,
        accountHolder: String,
        dateFilter: String = "All Transactions",
        typeFilter: String = "All",
        categoryFilter: String = "All Categories",
        sourceFilter: String = "All Sources"
    ): File {
        // 1. Filter Transactions based on options
        val filtered = filterTransactions(transactions, dateFilter, typeFilter, categoryFilter, sourceFilter)

        // 2. Sort chronologically for computations (oldest first)
        val chronological = filtered.sortedBy { tx ->
            runCatching {
                SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US).parse("${tx.date} ${tx.time}")?.time
            }.getOrNull() ?: runCatching {
                SimpleDateFormat("MMM dd, yyyy", Locale.US).parse(tx.date)?.time
            }.getOrNull() ?: 0L
        }
        val document = PdfDocument()
        
        // Page dimensions (A4 size: 595 x 842 points)
        val pageWidth = 595
        val pageHeight = 842
        
        // Create Page 1: Dashboard, Overview, and Charts
        val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page1 = document.startPage(pageInfo1)
        val canvas1 = page1.canvas
        
        drawPage1(context, canvas1, chronological, accountHolder, dateFilter, pageWidth, pageHeight)
        document.finishPage(page1)

        // Create Page 2+: Transaction Table (Paginated)
        paginateTransactions(context, document, chronological, pageWidth, pageHeight)

        // Save file
        val outputDir = File(context.filesDir, "certificates").apply { mkdirs() }
        val outputFile = File(outputDir, "Habte_Financial_Statement_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
        
        return outputFile
    }

    private fun filterTransactions(
        transactions: List<Transaction>,
        dateFilter: String,
        typeFilter: String,
        categoryFilter: String,
        sourceFilter: String
    ): List<Transaction> {
        var list = transactions

        // Date Filter
        if (dateFilter != "All Transactions") {
            val startLimit = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endLimit = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }

            when (dateFilter) {
                "This Month" -> {
                    startLimit.set(Calendar.DAY_OF_MONTH, 1)
                }
                "Last Month" -> {
                    startLimit.add(Calendar.MONTH, -1)
                    startLimit.set(Calendar.DAY_OF_MONTH, 1)
                    endLimit.set(Calendar.DAY_OF_MONTH, 1)
                    endLimit.add(Calendar.DAY_OF_MONTH, -1)
                }
                "Last 3 Months" -> {
                    startLimit.add(Calendar.MONTH, -3)
                }
                "Last 6 Months" -> {
                    startLimit.add(Calendar.MONTH, -6)
                }
            }

            val parseFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            list = list.filter { tx ->
                val txTime = runCatching {
                    parseFormat.parse(tx.date)?.time
                }.getOrNull() ?: 0L
                txTime in startLimit.timeInMillis..endLimit.timeInMillis
            }
        }

        // Type Filter
        if (typeFilter != "All") {
            val typeKey = typeFilter.lowercase()
            list = list.filter { it.type == typeKey }
        }

        // Category Filter
        if (categoryFilter != "All Categories") {
            list = list.filter { it.category == categoryFilter }
        }

        // Source Filter
        if (sourceFilter != "All Sources") {
            list = list.filter { it.bankShortName == sourceFilter }
        }

        return list
    }

    private fun drawPage1(
        context: Context,
        canvas: Canvas,
        transactions: List<Transaction>,
        accountHolder: String,
        period: String,
        width: Int,
        height: Int
    ) {
        // --- 1. COVER / HEADER ---
        val headerPaint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(0f, 0f, 0f, 130f, Color.parseColor("#008F3C"), Color.parseColor("#00C853"), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 130f, headerPaint)

        // Accent Gold line underneath
        val goldPaint = Paint().apply {
            color = Color.parseColor("#FFD54F")
        }
        canvas.drawRect(0f, 130f, width.toFloat(), 134f, goldPaint)

        // Text settings
        val textWhiteBold = Paint().apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val textWhiteSmall = Paint().apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        canvas.drawText("HABTE ሀብቴ", 40f, 55f, textWhiteBold)
        canvas.drawText("PERSONAL FINANCIAL STATEMENT", 40f, 75f, textWhiteSmall)
        canvas.drawText("Statement Period: $period", 40f, 95f, textWhiteSmall)

        // Right side info
        val dateText = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Holder: ${accountHolder.ifBlank { "Client Account" }}", 380f, 55f, textWhiteSmall)
        canvas.drawText("Generated: $dateText", 380f, 75f, textWhiteSmall)
        canvas.drawText("Status: Official Report", 380f, 95f, textWhiteSmall)

        // Compute statement balances mathematically
        val summary = computeStatementBalances(transactions)
        val totalIncome = summary.totalIncome
        val totalExpense = summary.totalExpense
        val netCashFlow = summary.netCashFlow
        val openingBalance = summary.openingBalance
        val closingBalance = summary.closingBalance

        // --- 2. SUMMARY CARDS ---
        val cardLeft1 = 40f
        val cardWidth = 238f
        val cardHeight = 54f
        val cardGap = 38f
        
        drawSummaryCard(canvas, cardLeft1, 155f, cardWidth, cardHeight, "TOTAL INCOME", "ETB " + Data.formatBalance(totalIncome), "↑", "#00C853", "#E8F5E9")
        drawSummaryCard(canvas, cardLeft1 + cardWidth + cardGap, 155f, cardWidth, cardHeight, "TOTAL EXPENSE", "ETB " + Data.formatBalance(totalExpense), "↓", "#EF5350", "#FFEBEE")

        val sectionTitlePaint = Paint().apply {
            color = Color.parseColor("#17201B")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        canvas.drawText("ACCOUNT OVERVIEW", 40f, 305f, sectionTitlePaint)
        
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#DDE5DF")
            strokeWidth = 1f
        }
        canvas.drawLine(40f, 312f, 555f, 312f, dividerPaint)

        // Two-column details
        val labelPaint = Paint().apply {
            color = Color.parseColor("#66736B")
            textSize = 10f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        val valPaint = Paint().apply {
            color = Color.parseColor("#17201B")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // Col 1
        canvas.drawText("Opening Balance", 40f, 332f, labelPaint)
        canvas.drawText("ETB " + Data.formatBalance(openingBalance), 160f, 332f, valPaint)
        
        canvas.drawText("Total Inflow", 40f, 350f, labelPaint)
        canvas.drawText("ETB " + Data.formatBalance(totalIncome), 160f, 350f, valPaint)
        
        canvas.drawText("Net Cash Flow", 40f, 368f, labelPaint)
        canvas.drawText("ETB " + Data.formatBalance(netCashFlow), 160f, 368f, valPaint)

        // Col 2
        canvas.drawText("Closing Balance", 315f, 332f, labelPaint)
        canvas.drawText("ETB " + Data.formatBalance(closingBalance), 450f, 332f, valPaint)
        
        canvas.drawText("Total Outflow", 315f, 350f, labelPaint)
        canvas.drawText("ETB " + Data.formatBalance(totalExpense), 450f, 350f, valPaint)
        
        canvas.drawText("Transaction Count", 315f, 368f, labelPaint)
        canvas.drawText("${transactions.size} records", 450f, 368f, valPaint)
        // --- 4. TRANSACTION HIGHLIGHTS ---
        canvas.drawText("IMPORTANT HIGHLIGHTS", 40f, 398f, sectionTitlePaint)
        canvas.drawLine(40f, 405f, 555f, 405f, dividerPaint)

        val highestIncome = transactions.filter { it.type == "credit" }.maxOfOrNull { it.amount } ?: 0.0
        val largestExpense = transactions.filter { it.type == "debit" }.maxOfOrNull { it.amount } ?: 0.0
        val mostUsedCategory = transactions.groupBy { it.category }.maxByOrNull { it.value.size }?.key ?: "N/A"

        drawHighlightCard(canvas, 40f, 415f, 115f, 42f, "Highest Income", "ETB " + Data.formatBalance(highestIncome))
        drawHighlightCard(canvas, 168f, 415f, 115f, 42f, "Largest Expense", "ETB " + Data.formatBalance(largestExpense))
        drawHighlightCard(canvas, 296f, 415f, 115f, 42f, "Top Category", mostUsedCategory)
        drawHighlightCard(canvas, 424f, 415f, 131f, 42f, "Total Audited", "${transactions.size} Items")

        // --- 5. ANALYTICS ---
        canvas.drawText("FINANCIAL ANALYTICS", 40f, 485f, sectionTitlePaint)
        canvas.drawLine(40f, 492f, 555f, 492f, dividerPaint)

        // Row 1: Charts 1 & 2
        drawMonthlyIncomeExpenseChart(canvas, 40f, 505f, 238f, 120f, transactions)
        drawExpenseCategoryDonut(canvas, 315f, 505f, 240f, 120f, transactions)

        // Row 2: Charts 3 & 4
        drawMonthlyCashFlowChart(canvas, 40f, 645f, 238f, 120f, transactions)
        drawBalanceTrendChart(canvas, 315f, 645f, 240f, 120f, transactions)

        // Footer Page 1
        drawFooter(canvas, 1, 1, width, height)
    }

    private fun drawSummaryCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        title: String,
        amount: String,
        icon: String,
        themeColorHex: String,
        bgColorHex: String
    ) {
        val r = RectF(x, y, x + w, y + h)
        val bgPaint = Paint().apply {
            color = Color.parseColor(bgColorHex)
            isAntiAlias = true
        }
        canvas.drawRoundRect(r, 10f, 10f, bgPaint)

        val borderPaint = Paint().apply {
            color = Color.parseColor(themeColorHex)
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        canvas.drawRoundRect(r, 10f, 10f, borderPaint)

        // Draw Icon circle
        val circlePaint = Paint().apply {
            color = withAlpha(Color.parseColor(themeColorHex), 35)
            isAntiAlias = true
        }
        canvas.drawCircle(x + 22f, y + h / 2f, 15f, circlePaint)

        val iconPaint = Paint().apply {
            color = Color.parseColor(themeColorHex)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(icon, x + 22f, y + h / 2f + 4f, iconPaint)

        // Text labels
        val lblPaint = Paint().apply {
            color = Color.parseColor("#66736B")
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(title, x + 48f, y + 20f, lblPaint)

        val amtPaint = Paint().apply {
            color = Color.parseColor("#17201B")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(amount, x + 48f, y + 42f, amtPaint)
    }

    private fun drawHighlightCard(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, label: String, value: String) {
        val r = RectF(x, y, x + w, y + h)
        val bgPaint = Paint().apply {
            color = Color.parseColor("#F5F7F6")
            isAntiAlias = true
        }
        canvas.drawRoundRect(r, 6f, 6f, bgPaint)

        val borderPaint = Paint().apply {
            color = Color.parseColor("#DDE5DF")
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            isAntiAlias = true
        }
        canvas.drawRoundRect(r, 6f, 6f, borderPaint)

        val lblPaint = Paint().apply {
            color = Color.parseColor("#66736B")
            textSize = 7.5f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        canvas.drawText(label, x + 10f, y + 16f, lblPaint)

        val valPaint = Paint().apply {
            color = Color.parseColor("#064E3B")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(value, x + 10f, y + 32f, valPaint)
    }

    private fun drawMonthlyIncomeExpenseChart(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, transactions: List<Transaction>) {
        val borderPaint = Paint().apply {
            color = Color.parseColor("#DDE5DF")
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 8f, 8f, borderPaint)

        val titlePaint = Paint().apply {
            color = Color.parseColor("#064E3B")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Monthly Income vs Expense", x + 10f, y + 16f, titlePaint)

        // Process data
        val monthlyData = groupMonthlyData(transactions).takeLast(4)
        if (monthlyData.isEmpty()) return

        val maxVal = (monthlyData.maxOfOrNull { Math.max(it.second.first, it.second.second) } ?: 1.0).coerceAtLeast(1.0)
        
        val baselineY = y + h - 22f
        val chartUsableHeight = h - 45f
        val stepX = (w - 20f) / monthlyData.size
        
        val greenBarPaint = Paint().apply { color = Color.parseColor("#00C853") }
        val redBarPaint = Paint().apply { color = Color.parseColor("#EF5350") }
        
        val labelPaint = Paint().apply {
            color = Color.parseColor("#66736B")
            textSize = 7f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        monthlyData.forEachIndexed { i, data ->
            val monthLabel = data.first
            val income = data.second.first
            val expense = data.second.second

            val barCenterX = x + 15f + i * stepX
            
            // Draw Income Bar
            val incHeight = (income / maxVal * chartUsableHeight).toFloat()
            canvas.drawRect(barCenterX - 6f, baselineY - incHeight, barCenterX - 1f, baselineY, greenBarPaint)

            // Draw Expense Bar
            val expHeight = (expense / maxVal * chartUsableHeight).toFloat()
            canvas.drawRect(barCenterX + 1f, baselineY - expHeight, barCenterX + 6f, baselineY, redBarPaint)

            // Label
            canvas.drawText(monthLabel, barCenterX, baselineY + 12f, labelPaint)
        }
    }

    private fun drawExpenseCategoryDonut(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, transactions: List<Transaction>) {
        val borderPaint = Paint().apply {
            color = Color.parseColor("#DDE5DF")
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 8f, 8f, borderPaint)

        val titlePaint = Paint().apply {
            color = Color.parseColor("#064E3B")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Top Expenses by Category", x + 10f, y + 16f, titlePaint)

        val debits = transactions.filter { it.type == "debit" }
        val categorySums = debits.groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(4)

        if (categorySums.isEmpty()) {
            val emptyPaint = Paint().apply {
                color = Color.parseColor("#66736B")
                textSize = 9f
                isAntiAlias = true
            }
            canvas.drawText("No expense records to analyze", x + 30f, y + h / 2f, emptyPaint)
            return
        }

        val totalDebit = categorySums.sumOf { it.second }
        val cx = x + 55f
        val cy = y + h / 2f + 5f
        val radius = 35f
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        val colors = listOf("#FFA726", "#29B6F6", "#AB47BC", "#EC407A", "#FFCA28", "#B0BEC5")
        var startAngle = 0f

        categorySums.forEachIndexed { index, (cat, amt) ->
            val sweep = ((amt / totalDebit) * 360f).toFloat()
            val sectorPaint = Paint().apply {
                color = Color.parseColor(colors[index % colors.size])
                isAntiAlias = true
            }
            canvas.drawArc(rect, startAngle, sweep, true, sectorPaint)
            startAngle += sweep
        }

        // Draw inner white circle
        val centerWhite = Paint().apply { color = Color.WHITE }
        canvas.drawCircle(cx, cy, 20f, centerWhite)

        // Draw Legend
        val legendLabelPaint = Paint().apply {
            color = Color.parseColor("#17201B")
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val legendColorPaint = Paint().apply { isAntiAlias = true }

        categorySums.forEachIndexed { index, (cat, amt) ->
            val pct = (amt / totalDebit * 100).toInt()
            val ly = y + 28f + (index * 20f)
            legendColorPaint.color = Color.parseColor(colors[index % colors.size])
            
            canvas.drawRect(x + 115f, ly, x + 122f, ly + 7f, legendColorPaint)
            canvas.drawText("$cat ($pct%)", x + 130f, ly + 7f, legendLabelPaint)
        }
    }

    private fun drawMonthlyCashFlowChart(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, transactions: List<Transaction>) {
        val borderPaint = Paint().apply {
            color = Color.parseColor("#DDE5DF")
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 8f, 8f, borderPaint)

        val titlePaint = Paint().apply {
            color = Color.parseColor("#064E3B")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Monthly Net Cash Flow", x + 10f, y + 16f, titlePaint)

        val monthlyData = groupMonthlyData(transactions).takeLast(4)
        if (monthlyData.isEmpty()) return

        val cashFlows = monthlyData.map { it.second.first - it.second.second }
        val maxFlow = cashFlows.maxOfOrNull { Math.abs(it) }?.coerceAtLeast(1.0) ?: 1.0

        val midY = y + h / 2f + 8f
        val usableHeight = (h - 40f) / 2f
        val stepX = (w - 20f) / monthlyData.size

        val greenPaint = Paint().apply { color = Color.parseColor("#00C853") }
        val redPaint = Paint().apply { color = Color.parseColor("#EF5350") }
        
        val labelPaint = Paint().apply {
            color = Color.parseColor("#66736B")
            textSize = 7f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        canvas.drawLine(x + 10f, midY, x + w - 10f, midY, Paint().apply { color = Color.parseColor("#B0BEC5"); strokeWidth = 0.6f })

        monthlyData.forEachIndexed { i, data ->
            val monthLabel = data.first
            val flow = data.second.first - data.second.second
            val barCenterX = x + 15f + i * stepX
            val barHeight = (Math.abs(flow) / maxFlow * usableHeight).toFloat()

            if (flow >= 0) {
                canvas.drawRect(barCenterX - 5f, midY - barHeight, barCenterX + 5f, midY, greenPaint)
            } else {
                canvas.drawRect(barCenterX - 5f, midY, barCenterX + 5f, midY + barHeight, redPaint)
            }

            canvas.drawText(monthLabel, barCenterX, y + h - 6f, labelPaint)
        }
    }

    private fun drawBalanceTrendChart(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, transactions: List<Transaction>) {
        val borderPaint = Paint().apply {
            color = Color.parseColor("#DDE5DF")
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 8f, 8f, borderPaint)

        val titlePaint = Paint().apply {
            color = Color.parseColor("#064E3B")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Balance Trend Progression", x + 10f, y + 16f, titlePaint)

        val balanceHistory = transactions.mapNotNull { it.balance }
        if (balanceHistory.size < 2) {
            val emptyPaint = Paint().apply {
                color = Color.parseColor("#66736B")
                textSize = 9f
                isAntiAlias = true
            }
            canvas.drawText("Insufficient data to plot trend", x + 30f, y + h / 2f, emptyPaint)
            return
        }

        val maxBal = balanceHistory.maxOrNull() ?: 1.0
        val minBal = balanceHistory.minOrNull() ?: 0.0
        val spread = (maxBal - minBal).coerceAtLeast(1.0)

        val paddingLeft = x + 15f
        val chartWidth = w - 30f
        val paddingTop = y + 35f
        val chartHeight = h - 50f
        
        val path = Path()
        val fillPath = Path()

        val pointsCount = balanceHistory.size
        val stepX = chartWidth / (pointsCount - 1)

        balanceHistory.forEachIndexed { index, bal ->
            val px = paddingLeft + index * stepX
            val py = paddingTop + chartHeight - (((bal - minBal) / spread) * chartHeight).toFloat()

            if (index == 0) {
                path.moveTo(px, py)
                fillPath.moveTo(px, paddingTop + chartHeight)
                fillPath.lineTo(px, py)
            } else {
                path.lineTo(px, py)
                fillPath.lineTo(px, py)
            }
            
            if (index == pointsCount - 1) {
                fillPath.lineTo(px, paddingTop + chartHeight)
                fillPath.close()
            }
        }

        // Draw fill gradient
        val fillPaint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(0f, paddingTop, 0f, paddingTop + chartHeight, Color.parseColor("#80FFD54F"), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        }
        canvas.drawPath(fillPath, fillPaint)

        // Draw trend line
        val linePaint = Paint().apply {
            color = Color.parseColor("#FFD54F")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawPath(path, linePaint)
    }

    private fun groupMonthlyData(transactions: List<Transaction>): List<Pair<String, Pair<Double, Double>>> {
        val map = mutableMapOf<String, Pair<Double, Double>>()
        transactions.forEach { t ->
            val parts = t.date.split(" ")
            if (parts.size >= 3) {
                val month = parts[0].take(3)
                val year = parts[2]
                val key = "$month $year"
                val cur = map.getOrDefault(key, Pair(0.0, 0.0))
                if (t.type == "credit") {
                    map[key] = Pair(cur.first + t.amount, cur.second)
                } else {
                    map[key] = Pair(cur.first, cur.second + t.amount)
                }
            }
        }
        return map.toList().sortedBy { pair ->
            runCatching {
                SimpleDateFormat("MMM yyyy", Locale.getDefault()).parse(pair.first)?.time
            }.getOrNull() ?: 0L
        }
    }

    private fun paginateTransactions(
        context: Context,
        document: PdfDocument,
        transactions: List<Transaction>,
        width: Int,
        height: Int
    ) {
        if (transactions.isEmpty()) return
        
        val rowsPerPage = 22
        val totalRows = transactions.size
        val pagesCount = (totalRows + rowsPerPage - 1) / rowsPerPage

        val headerColor = Color.parseColor("#008F3C")
        val borderColor = Color.parseColor("#DDE5DF")
        
        val textPaint = Paint().apply {
            color = Color.parseColor("#17201B")
            textSize = 8.5f
            isAntiAlias = true
        }

        val textHeaderPaint = Paint().apply {
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            color = borderColor
            strokeWidth = 0.6f
        }

        for (p in 0 until pagesCount) {
            val pageInfo = PdfDocument.PageInfo.Builder(width, height, p + 2).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Page Header
            val shpPaint = Paint().apply {
                color = headerColor
            }
            canvas.drawRect(40f, 40f, 555f, 62f, shpPaint)
            canvas.drawText("HABTE ሀብቴ • Personal Financial Statement", 50f, 54f, textHeaderPaint)
            
            // Draw Table headers
            val rowYStart = 85f
            canvas.drawRect(40f, rowYStart, 555f, rowYStart + 20f, Paint().apply { color = Color.parseColor("#F5F7F6") })
            canvas.drawLine(40f, rowYStart, 555f, rowYStart, borderPaint)
            canvas.drawLine(40f, rowYStart + 20f, 555f, rowYStart + 20f, borderPaint)

            val headLabelPaint = Paint().apply {
                color = Color.parseColor("#064E3B")
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            canvas.drawText("Date", 45f, rowYStart + 13f, headLabelPaint)
            canvas.drawText("Description", 110f, rowYStart + 13f, headLabelPaint)
            canvas.drawText("Category", 250f, rowYStart + 13f, headLabelPaint)
            canvas.drawText("Source", 335f, rowYStart + 13f, headLabelPaint)
            canvas.drawText("Type", 410f, rowYStart + 13f, headLabelPaint)
            canvas.drawText("Amount", 480f, rowYStart + 13f, headLabelPaint)

            val startIdx = p * rowsPerPage
            val endIdx = Math.min(startIdx + rowsPerPage, totalRows)

            var cy = rowYStart + 20f

            for (i in startIdx until endIdx) {
                val tx = transactions[i]
                
                // Draw cells
                canvas.drawText(tx.date, 45f, cy + 15f, textPaint)
                
                // Truncate title
                val truncatedTitle = if (tx.title.length > 25) tx.title.take(22) + "..." else tx.title
                canvas.drawText(truncatedTitle, 110f, cy + 15f, textPaint)
                
                // Category badge background
                val catBadgeBgPaint = Paint().apply {
                    color = withAlpha(getCategoryColor(tx.category), 30)
                    isAntiAlias = true
                }
                val catBadgeRect = RectF(250f, cy + 5f, 320f, cy + 18f)
                canvas.drawRoundRect(catBadgeRect, 4f, 4f, catBadgeBgPaint)
                
                val catTextPaint = Paint().apply {
                    color = getCategoryColor(tx.category)
                    textSize = 7.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                canvas.drawText(tx.category, 255f, cy + 14f, catTextPaint)

                canvas.drawText(tx.bankShortName, 335f, cy + 15f, textPaint)

                // Type badge
                val isCredit = tx.type == "credit"
                val typeColorHex = if (isCredit) "#00C853" else "#EF5350"
                val typeLabel = if (isCredit) "INCOME" else "EXPENSE"
                
                val typeBadgeBgPaint = Paint().apply {
                    color = withAlpha(Color.parseColor(typeColorHex), 30)
                    isAntiAlias = true
                }
                val typeBadgeRect = RectF(410f, cy + 5f, 465f, cy + 18f)
                canvas.drawRoundRect(typeBadgeRect, 4f, 4f, typeBadgeBgPaint)

                val typeTextPaint = Paint().apply {
                    color = Color.parseColor(typeColorHex)
                    textSize = 7.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                canvas.drawText(typeLabel, 415f, cy + 14f, typeTextPaint)

                // Amount
                val signedAmt = "${if (isCredit) "+" else "-"} ${Data.formatBalance(tx.amount)}"
                val amtColor = if (isCredit) Color.parseColor("#00C853") else Color.parseColor("#EF5350")
                val amtPaint = Paint().apply {
                    color = amtColor
                    textSize = 8.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                canvas.drawText(signedAmt, 480f, cy + 15f, amtPaint)

                // Divider line
                canvas.drawLine(40f, cy + 24f, 555f, cy + 24f, borderPaint)
                cy += 24f
            }

            // Draw Final Page Summary on last page
            if (p == pagesCount - 1) {
                drawStatementSummaryBlock(canvas, cy + 15f, transactions)
            }

            // Footer
            drawFooter(canvas, p + 2, pagesCount + 1, width, height)

            document.finishPage(page)
        }
    }

    private data class StatementBalances(
        val openingBalance: Double,
        val totalIncome: Double,
        val totalExpense: Double,
        val netCashFlow: Double,
        val closingBalance: Double
    )

    private fun computeStatementBalances(chronological: List<Transaction>): StatementBalances {
        val totalIncome = chronological.filter { it.type == "credit" }.sumOf { it.amount }
        val totalExpense = chronological.filter { it.type == "debit" }.sumOf { it.amount }
        val netCashFlow = totalIncome - totalExpense

        if (chronological.isEmpty()) {
            return StatementBalances(0.0, 0.0, 0.0, 0.0, 0.0)
        }

        val newestTxWithBalance = chronological.lastOrNull { it.balance != null }
        val closingBalance = if (newestTxWithBalance != null) {
            val idx = chronological.indexOf(newestTxWithBalance)
            var bal = newestTxWithBalance.balance!!
            for (i in (idx + 1) until chronological.size) {
                val tx = chronological[i]
                if (tx.type == "credit") bal += tx.amount else bal -= tx.amount
            }
            bal
        } else {
            netCashFlow
        }

        val oldestTxWithBalance = chronological.firstOrNull { it.balance != null }
        val openingBalance = if (oldestTxWithBalance != null) {
            val balBeforeOldest = if (oldestTxWithBalance.type == "credit") {
                oldestTxWithBalance.balance!! - oldestTxWithBalance.amount
            } else {
                oldestTxWithBalance.balance!! + oldestTxWithBalance.amount
            }
            val idx = chronological.indexOf(oldestTxWithBalance)
            var bal = balBeforeOldest
            for (i in (idx - 1) downTo 0) {
                val tx = chronological[i]
                if (tx.type == "credit") bal -= tx.amount else bal += tx.amount
            }
            bal
        } else {
            closingBalance - netCashFlow
        }

        return StatementBalances(
            openingBalance = openingBalance,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netCashFlow = netCashFlow,
            closingBalance = closingBalance
        )
    }

    private fun drawStatementSummaryBlock(canvas: Canvas, y: Float, transactions: List<Transaction>) {
        val summary = computeStatementBalances(transactions)
        val totalIncome = summary.totalIncome
        val totalExpense = summary.totalExpense
        val netCashFlow = summary.netCashFlow
        val openingBalance = summary.openingBalance
        val closingBalance = summary.closingBalance

        val rect = RectF(40f, y, 555f, y + 80f)
        val bgPaint = Paint().apply {
            color = Color.parseColor("#F5F7F6")
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 10f, 10f, bgPaint)

        val borderPaint = Paint().apply {
            color = Color.parseColor("#DDE5DF")
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 10f, 10f, borderPaint)

        val titlePaint = Paint().apply {
            color = Color.parseColor("#064E3B")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("STATEMENT SUMMARY", 55f, y + 20f, titlePaint)

        val labelPaint = Paint().apply {
            color = Color.parseColor("#66736B")
            textSize = 8.5f
            isAntiAlias = true
        }
        val valPaint = Paint().apply {
            color = Color.parseColor("#17201B")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // Col 1
        canvas.drawText("Total Transactions: ${transactions.size}", 55f, y + 40f, labelPaint)
        canvas.drawText("Total Income: ETB ${Data.formatBalance(totalIncome)}", 55f, y + 55f, labelPaint)
        canvas.drawText("Total Expenses: ETB ${Data.formatBalance(totalExpense)}", 55f, y + 70f, labelPaint)

        // Col 2
        canvas.drawText("Net Cash Flow: ETB ${Data.formatBalance(netCashFlow)}", 300f, y + 40f, valPaint)
        canvas.drawText("Opening Balance: ETB ${Data.formatBalance(openingBalance)}", 300f, y + 55f, valPaint)
        canvas.drawText("Closing Balance: ETB ${Data.formatBalance(closingBalance)}", 300f, y + 70f, valPaint)
    }

    private fun getCategoryColor(category: String): Int {
        val colorHex = when (category) {
            "Salary", "Income", "Freelance" -> "#00C853"
            "Bills", "Rent", "Utility" -> "#EF5350"
            "Food", "Food & Dining" -> "#FFA726"
            "Transport" -> "#29B6F6"
            "Shopping" -> "#AB47BC"
            "Transfer" -> "#FFCA28"
            else -> "#78909C"
        }
        return Color.parseColor(colorHex)
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return (color and 0x00FFFFFF) or (alpha shl 24)
    }

    private fun drawFooter(canvas: Canvas, currentPage: Int, totalPages: Int, width: Int, height: Int) {
        val footerPaint = Paint().apply {
            color = Color.parseColor("#66736B")
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        
        val linePaint = Paint().apply {
            color = Color.parseColor("#DDE5DF")
            strokeWidth = 0.6f
        }

        canvas.drawLine(40f, height - 36f, width - 40f, height - 36f, linePaint)
        canvas.drawText("Habte ሀብቴ • Personal Financial Statement", 40f, height - 22f, footerPaint)
        canvas.drawText("Page $currentPage of $totalPages", width - 90f, height - 22f, footerPaint)
    }
}

