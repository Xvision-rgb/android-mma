import SwiftUI
import Charts

/// N'affiche QUE la moyenne mobile 7 jours — jamais le point brut du jour —
/// conformément au principe UX non négociable : ne jamais montrer un poids
/// brut comme donnée principale sans le contextualiser par la tendance.
struct WeightTrendChart: View {
    let points: [(date: Date, value: Double)]

    var body: some View {
        if points.isEmpty {
            ContentUnavailableView(
                "Pas encore de pesée",
                systemImage: "scalemass",
                description: Text("Ajoute une pesée du matin pour voir la tendance.")
            )
        } else {
            Chart(points, id: \.date) { point in
                LineMark(
                    x: .value("Date", point.date),
                    y: .value("Poids (kg, moy. 7j)", point.value)
                )
                .interpolationMethod(.catmullRom)
                .foregroundStyle(.blue)

                AreaMark(
                    x: .value("Date", point.date),
                    y: .value("Poids (kg, moy. 7j)", point.value)
                )
                .interpolationMethod(.catmullRom)
                .foregroundStyle(.blue.opacity(0.08))
            }
            .chartYScale(domain: .automatic(includesZero: false))
            .chartXAxis {
                AxisMarks(values: .stride(by: .day, count: 7))
            }
        }
    }
}
