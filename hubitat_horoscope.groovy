/**
 *  Horoscope Generator
 */

// Define driver metadata
metadata {
    definition( name: "Horoscope Generator", namespace: "horoscope_jlobe", author: "jlobe", importUrl: "" ) {
        // Driver capabilities (driver will not show under devices in tile creation unless it has at least one capability)
        capability "Refresh"
        // Driver attributes
        attribute "dateRange", "string"
        attribute "currentDate", "string"
        attribute "description", "string"
        attribute "compatibility", "string"
        attribute "mood", "string"
        attribute "color", "string"
        attribute "luckyNumber", "string"
        attribute "luckyTime", "string"
        attribute "tilehtml", "string"
    }
    preferences() {
        section( "User Inputs" ){
            // Define driver options
            input "logEnable", "bool", required: false, title: "Enable Debug Logging<br>(auto off in 15 minutes)", defaultValue: false
            input "sign", "enum", title: "Astrological Sign:", required: true, options: ["Aries", "Aquarius", "Cancer", "Capricorn", "Gemini", "Leo", "Libra", "Pisces", "Sagittarius", "Scorpio", "Taurus", "Virgo"]
            input "day", "enum", title: "Day:", required: false, defaultValue: "Today", options: ["Today", "Tomorrow", "Yesterday"]
        }
    }
}

// Installed (runs when driver first installed (clicked done)
def installed() {
    
	if( logEnable ) log.debug "Installed Horoscope Generator Driver"
    updated()
}

// Refresh (runs when refresh capability is clicked)
def refresh() {
    if( logEnable ) log.debug "Refreshed Horoscope Generator Driver"
    updated()   
}

// Updated (runs when clicked done after original install)
def updated() {
	if( logEnable ) log.debug "Updated Horoscope Generator Driver"
    
    // Unschedule any previous jobs
    unschedule()
    
    // Get horoscope
    getHoroscope()
    
    // Schedule job to execute once every 24 hours
    if( logEnable ) log.info "AutoPolling scheduled for every 24 hours."
    schedule( "0 0 12 1/1 * ? *", getHoroscope )
    
    // If logs are enabled; turn off after 15 minutes
    if( logEnable ) runIn( 900, logsOff )
}

// Turn off logs
def logsOff() {
    log.warn "Debug Logging Disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

// Get horoscope
def getHoroscope() {
    if( logEnable ) log.debug "Polling Horoscope Generator..."
    
    // Get user defined sign and day
    sign = (settings?.sign ?: "Aries")
    day = (settings?.day ?: "Today")
    
    // Map body and parameters for httppost
    Map cmd = ["sign": sign, "day": day]
    Map params = [
      uri: "https://aztro.sameerkumar.website/",
      query: cmd,
      contentType: "application/json"
    ]
    
    // Call api to get horoscope data
    if( logEnable ) log.debug "API Call Parameters:"
    if( logEnable ) log.debug params
	asynchttpPost( "handleHoroscope", params, cmd )
}

// Handle horoscope
def handleHoroscope( resp, data ) {
    
    // Parse response data to JSON format
    parsed = resp.getJson()
    if( logEnable ) log.debug "API Response Parameters:"
    if( logEnable ) log.debug parsed
    
    // Update driver attributes
    sendEvent( name: "dateRange", value: parsed.date_range )
    sendEvent( name: "currentDate", value: parsed.current_date )
    sendEvent( name: "description", value: parsed.description )
    sendEvent( name: "compatibility", value: parsed.compatibility )
    sendEvent( name: "mood", value: parsed.mood )
    sendEvent( name: "color", value: parsed.color )
    sendEvent( name: "luckyNumber", value: parsed.lucky_number )
    sendEvent( name: "luckyTime", value: parsed.lucky_time )
    
    // Build tile html
    html = "<div style='font-size: 12px;'>"
        html += "<p style='font-size: 18px; font-weight: bold; margin-bottom: 20px;'>Daily Horoscope - ${(settings?.sign ?: "Aries")}</p>"
        html += "<p>${parsed.description}</p>"
        html += "<br />"
        html += "<table style='margin-right: auto; margin-left: auto;'>"
            html += "<tr><td style='padding:5px;'>Lucky Number: ${parsed.lucky_number}</td><td style='padding:5px;'>Lucky Time: ${parsed.lucky_time}</td><td style='padding:5px;'>Lucky Color: ${parsed.color}</td><td style='padding:5px;'>Mood: ${parsed.mood}</td></tr>"
            html += "<tr><td style='padding:5px;'>Date: ${parsed.current_date}</td><td style='padding:5px;'>Range: ${parsed.date_range}</td><td style='padding:5px;'>Compatibility: ${parsed.compatibility}</td><td style='padding:5px;'></td></tr>"
        html += "</table>"
    html += "</div>"
    
    // Update driver html attribute
    sendEvent( name: "tilehtml", value: html )
}