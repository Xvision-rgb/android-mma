import Foundation

enum MovingAverage {
    /// Moyenne mobile 7 jours sur une série (date, valeur).
    /// C'est TOUJOURS cette série qu'il faut afficher pour le poids —
    /// jamais le point brut du jour — pour désamorcer l'anxiété liée
    /// aux fluctuations quotidiennes.
    static func sevenDay(points: [(date: Date, value: Double)]) -> [(date: Date, value: Double)] {
        guard !points.isEmpty else { return [] }
        let sorted = points.sorted { $0.date < $1.date }
        var result: [(date: Date, value: Double)] = []

        for point in sorted {
            let windowStart = Calendar.current.date(byAdding: .day, value: -6, to: point.date) ?? point.date
            let window = sorted.filter { $0.date >= windowStart && $0.date <= point.date }
            let avg = window.map(\.value).reduce(0, +) / Double(window.count)
            result.append((date: point.date, value: avg))
        }
        return result
    }
}
