import Foundation

enum DateUtils {
    static let isoDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(identifier: "UTC")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }()

    static let displayDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "fr_FR")
        formatter.dateFormat = "EEEE d MMMM"
        return formatter
    }()

    static func string(from date: Date) -> String {
        isoDateFormatter.string(from: date)
    }

    static func date(from string: String) -> Date? {
        isoDateFormatter.date(from: string)
    }

    static func today() -> String {
        string(from: Date())
    }

    static func daysAgo(_ n: Int) -> String {
        string(from: Calendar.current.date(byAdding: .day, value: -n, to: Date()) ?? Date())
    }

    /// 1 = lundi ... 7 = dimanche, pour matcher `training_plan.jour_semaine`.
    static func weekdayISO(from dateString: String) -> Int {
        guard let date = date(from: dateString) else { return 1 }
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC")!
        let weekday = calendar.component(.weekday, from: date) // 1 = dimanche ... 7 = samedi
        return weekday == 1 ? 7 : weekday - 1
    }

    static func startOfWeek(from date: Date = Date()) -> String {
        var calendar = Calendar(identifier: .gregorian)
        calendar.firstWeekday = 2 // lundi
        let components = calendar.dateComponents([.yearForWeekOfYear, .weekOfYear], from: date)
        let monday = calendar.date(from: components) ?? date
        return string(from: monday)
    }
}
