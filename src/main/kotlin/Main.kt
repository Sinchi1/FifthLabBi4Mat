import org.knowm.xchart.*
import org.knowm.xchart.style.lines.SeriesLines
import org.knowm.xchart.style.markers.SeriesMarkers
import java.io.File
import kotlin.math.*

data class Point(val x: Double, val y: Double)

class Interpolation {
    private val points = mutableListOf<Point>()
    private var testFunction: ((Double) -> Double)? = null

    fun inputFromKeyboard() {
        println("Введите количество точек:")
        val n = readLine()?.toIntOrNull() ?: return
        if (n < 2) {
            println("Ошибка: требуется минимум 2 точки.")
            return
        }
        points.clear()
        for (i in 0 until n) {
            println("Введите x[$i]:")
            val x = readLine()?.toDoubleOrNull() ?: continue
            println("Введите y[$i]:")
            val y = readLine()?.toDoubleOrNull() ?: continue
            points.add(Point(x, y))
        }
        validatePoints()
    }

    fun inputFromFile(fileName: String) {
        points.clear()
        try {
            File(fileName).readLines().forEach { line ->
                val (x, y) = line.split(",").map { it.trim().toDouble() }
                points.add(Point(x, y))
            }
            validatePoints()
        } catch (e: Exception) {
            println("Ошибка чтения файла: ${e.message}")
        }
    }

    fun inputFromFunction() {
        println("Выберите функцию: 1) sin(x), 2) x^2")
        val choice = readLine()?.toIntOrNull() ?: return
        testFunction = when (choice) {
            1 -> { x -> sin(x) }
            2 -> { x -> x * x }
            else -> {
                println("Неверный выбор функции.")
                return
            }
        }
        println("Введите начало интервала:")
        val a = readLine()?.toDoubleOrNull() ?: return
        println("Введите конец интервала:")
        val b = readLine()?.toDoubleOrNull() ?: return
        println("Введите количество точек:")
        val n = readLine()?.toIntOrNull() ?: return
        if (n < 2 || a >= b) {
            println("Ошибка: некорректный интервал или количество точек.")
            return
        }
        points.clear()
        val step = (b - a) / (n - 1)
        for (i in 0 until n) {
            val x = a + i * step
            val y = testFunction!!(x)
            points.add(Point(x, y))
        }
        validatePoints()
    }

    private fun validatePoints() {
        if (points.size < 2) {
            println("Ошибка: недостаточно точек для интерполяции.")
            points.clear()
            return
        }
        val xSet = points.map { it.x }.toSet()
        if (xSet.size != points.size) {
            println("Ошибка: дубликаты значений x недопустимы.")
            points.clear()
        }
    }

    fun computeDifferenceTable(): List<List<Double>> {
        if (points.isEmpty()) return emptyList()
        val n = points.size
        val table = MutableList(n) { MutableList(n) { 0.0 } }
        for (i in 0 until n) {
            table[i][0] = points[i].y
        }
        for (j in 1 until n) {
            for (i in 0 until n - j) {
                table[i][j] = table[i + 1][j - 1] - table[i][j - 1]
            }
        }
        return table
    }

    fun printDifferenceTable() {
        val table = computeDifferenceTable()
        if (table.isEmpty()) {
            println("Таблица конечных разностей не может быть построена.")
            return
        }
        println("Таблица конечных разностей:")
        for (i in table.indices) {
            print("y[$i]: ")
            for (j in table[i].indices) {
                print("${table[i][j]}\t")
            }
            println()
        }
    }


    private fun isEquidistant(): Boolean {
        if (points.size < 2) return false
        val h = points[1].x - points[0].x
        val epsilon = max(1e-14, 1e-14 * abs(h))

        return points.zipWithNext().all { (a, b) ->
            abs((b.x - a.x) - h) < epsilon
        }
    }

    private fun lagrangeInterpolation(x: Double): Double {
        if (points.isEmpty()) return 0.0
        var result = 0.0
        for (i in points.indices) {
            var term = points[i].y
            for (j in points.indices) {
                if (i != j) {
                    term *= (x - points[j].x) / (points[i].x - points[j].x)
                }
            }
            result += term
        }
        return result
    }


    fun newtonUneven(x: Double): Double {
        if (isEquidistant()) {
            return 0.0
        }
        if (points.isEmpty()) return 0.0
        val table = computeDifferenceTable()
        var result = table[0][0]
        var product = 1.0

        for (i in table.indices) {
            for (j in 0 until table[i].size-1) {
                if(i + j + 1 > 7){
                    break
                }
                print(" | F(x${i}..x${i+j}) = ${dividedDifference(i,j)}")
            }
            println()
        }
        for (i in 1 until points.size) {
            product *= (x - points[i - 1].x)
            result += product * dividedDifference(0, i)
        }
        return result
    }

    private fun dividedDifference(start: Int, order: Int): Double {
        if (order == 0) return points[start].y
        return (dividedDifference(start + 1, order - 1) - dividedDifference(start, order - 1)) /
                (points[start + order].x - points[start].x)
    }

    fun gaussInterpolation(x: Double): Double {
        if (!isEquidistant()) {
            return 0.0
        }
        val table = computeDifferenceTable()
        val n = points.size
        val h = points[1].x - points[0].x

        if (x <= points[n/2].x) {
            var startIndex = 0
            for (i in 0 until n-1) {
                if (x >= points[i].x && x <= points[i+1].x) {
                    startIndex = i
                    break
                }
            }
            if (x < points[0].x) startIndex = 0
            if (x > points.last().x) startIndex = n-1

            val t = (x - points[startIndex].x) / h
            var result = table[startIndex][0]
            var term = 1.0
            var factorial = 1.0

            for (k in 1 until n) {
                term *= (t - k + 1)
                factorial *= k
                if (startIndex + k >= table.size) break
                println("Таблица : ${table[startIndex][k]}")
                result += (term / factorial) * table[startIndex][k]
            }
            return result
        } else {
            val startIndex = points.lastIndex
            val t = (x - points[startIndex].x) / h
            var result = table[startIndex][0]
            var term = 1.0
            var factorial = 1.0

            for (k in 1 until n) {
                term *= (t + k - 1)
                factorial *= k
                if (startIndex - k < 0) break

                result += (term / factorial) * table[startIndex - k][k]

                println("Таблица : ${table[startIndex - k][k]}")
            }
            return result
        }
    }

    fun plotInterpolation() {
        if (points.isEmpty()) {
            println("Нет данных для построения графика.")
            return
        }
        val chart = XYChartBuilder()
            .width(400)
            .height(400)
            .title("Interpolation")
            .xAxisTitle("X")
            .yAxisTitle("Y")
            .build()

        val xData = points.map { it.x }.toDoubleArray()
        val yData = points.map { it.y }.toDoubleArray()

        if (testFunction != null) {
            val xFunc = DoubleArray(100)
            val yFunc = DoubleArray(100)
            val minX = points.minOf { it.x }
            val maxX = points.maxOf { it.x }
            val step = (maxX - minX) / 99
            for (i in 0 until 100) {
                xFunc[i] = minX + i * step
                yFunc[i] = testFunction!!(xFunc[i])
            }
            chart.addSeries("Заданная функция", xFunc, yFunc).lineColor = java.awt.Color.BLUE
        }

        val xPoly = DoubleArray(100)
        val yLagrange = DoubleArray(100)
        val yNewton = DoubleArray(100)
        val yGauss = DoubleArray(100)
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val step = (maxX - minX) / 99
        for (i in 0 until 100) {
            xPoly[i] = minX + i * step
            yLagrange[i] = lagrangeInterpolation(xPoly[i]+0.001)
            yNewton[i] = newtonUneven(xPoly[i])
            yGauss[i] = gaussInterpolation(xPoly[i])
        }
        chart.addSeries("Лагранж", xPoly, yLagrange).lineColor = java.awt.Color.RED
        if (isEquidistant()){
            chart.addSeries("Ньютон конечных разностей", xPoly, yGauss).lineColor = java.awt.Color.MAGENTA
        }
        else{
            chart.addSeries("Ньютон разделённых разностей", xPoly, yNewton).lineColor = java.awt.Color.GREEN
        }
        val seriesPoints = chart.addSeries("Узлы интрополяции", xData, yData)
        seriesPoints.lineStyle = SeriesLines.NONE
        seriesPoints.marker = SeriesMarkers.CIRCLE

        chart.styler.xAxisMin= points.minOf { it.x }
        chart.styler.xAxisMax = points.maxOf { it.x }
        chart.styler.yAxisMin = points.minOf { it.y } - 1
        chart.styler.yAxisMax = points.maxOf { it.y } + 1


        SwingWrapper(chart).displayChart()
    }

    fun testInterpolation(xTest: Double) {
        if (points.isEmpty()) {
            println("Нет данных для интерполяции.")
            return
        }
        println("Интерполяция для x = $xTest:")
        val lagrange = lagrangeInterpolation(xTest)
        val newton = newtonUneven(xTest)
        val gauss = gaussInterpolation(xTest)
        println("Лагранж: $lagrange")
        println("Ньютона для разделённых разностей: $newton")
        println("Ньютона для конечных разностей: $gauss")
        if (testFunction != null) {
            val exact = testFunction!!(xTest)
            println("Точное значение: $exact")
            println("Ошибка Лагранжа: ${abs(exact - lagrange)}")
            println("Ошибка Ньютона для разделённых разностей: ${if (!isEquidistant()) abs(exact - newton) else "Не может быть подсчитано"}")
            println("Ошибка Ньютона для конечных разностей: ${if (isEquidistant()) abs(exact - gauss) else "Не может быть подсчитано"}")
        }
    }

}

fun main() {
    val interpolation = Interpolation()
    while (true) {
        println("\n1. Ввод с клавиатуры")
        println("2. Ввод из файла")
        println("3. Ввод по функции")
        println("4. Вывести таблицу конечных разностей")
        println("5. Вычислить интерполяцию")
        println("6. Построить график")
        println("7. Выход")
        when (readLine()?.toIntOrNull()) {
            1 -> interpolation.inputFromKeyboard()
            2 -> {
                println("Введите имя файла (например, test1.txt):")
                val fileName = readLine() ?: ""
                interpolation.inputFromFile(fileName)
            }
            3 -> interpolation.inputFromFunction()
            4 -> interpolation.printDifferenceTable()
            5 -> {
                println("Введите значение x для интерполяции:")
                val x = readLine()?.toDoubleOrNull() ?: 0.0
                interpolation.testInterpolation(x)
            }
            6 -> interpolation.plotInterpolation()
            7 -> break
            else -> println("Неверный выбор.")
        }
    }
}
