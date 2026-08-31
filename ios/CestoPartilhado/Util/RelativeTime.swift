import Foundation

/// Texto simples "há X horas/dias" — só as escalas que aparecem nos mockups.
func relativeTimeText(_ date: Date?) -> String {
    guard let date else { return "" }
    let diff = Date().timeIntervalSince(date)
    let hours = Int(diff / 3600)
    let days = Int(diff / 86400)
    if hours < 1 { return L("time_just_now") }
    if hours < 24 { return String(format: L("time_hours_ago"), hours) }
    if days < 2 { return L("time_yesterday") }
    return String(format: L("time_days_ago"), days)
}
