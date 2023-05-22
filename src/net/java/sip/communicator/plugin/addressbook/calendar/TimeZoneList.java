// The MIT License (MIT)
//
// Copyright (c) 2015 Neil Ferguson
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.
//
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.addressbook.calendar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * @author Neil Ferguson
 * A class containing mappings between timezone strings used by Microsoft
 * programs, standard Olson timezone strings and Java TimeZone objects.
 *
 * This code is taken from https://github.com/nfergu/Java-Time-Zone-List, under
 * the license above.
 *
 * Parts of the code are currently not used, but are left in for potential
 * future use.
 */
public class TimeZoneList {

    // Currently only used to map Microsoft to Olson timezone strings.
    // If a unique mapping of Olson to Microsoft timezone strings is required,
    // this List will need to be updated to match the latest CLDR mapping (see
    // http://unicode.org/repos/cldr/trunk/common/supplemental/windowsZones.xml)
    private static final List<TimeZoneMapping> ZONEMAPPINGS = new ArrayList<>();
    static {
        ZONEMAPPINGS.add(new TimeZoneMapping("Afghanistan Standard Time", "Asia/Kabul", "(GMT +04:30) Kabul"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Alaska Standard Time", "America/Anchorage", "(GMT -09:00) Alaska"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Alaskan Standard Time", "America/Anchorage", "(GMT -09:00) Alaska"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Aleutian Standard Time", "America/Adak", "(GMT -10:00) Aleutian Islands"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Altai Standard Time", "Asia/Barnaul", "(GMT +07:00) Barnaul, Gorno-Altaysk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Arab Standard Time", "Asia/Riyadh", "(GMT +03:00) Kuwait, Riyadh"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Arabian Standard Time", "Asia/Dubai", "(GMT +04:00) Abu Dhabi, Muscat"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Arabic Standard Time", "Asia/Baghdad", "(GMT +03:00) Baghdad"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Argentina Standard Time", "America/Buenos_Aires", "(GMT -03:00) Buenos Aires"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Argentine Time", "America/Buenos_Aires", "(GMT -03:00) Buenos Aires"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Atlantic Standard Time", "America/Halifax", "(GMT -04:00) Atlantic Time (Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Astrakhan Standard Time", "Europe/Astrakhan", "(GMT +04:00) Astrakhan, Ulyanovsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("AUS Central Standard Time", "Australia/Darwin", "(GMT +09:30) Darwin"));
        ZONEMAPPINGS.add(new TimeZoneMapping("AUS Eastern Standard Time", "Australia/Sydney", "(GMT +10:00) Canberra, Melbourne, Sydney"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Aus Central W. Standard Time", "Australia/Eucla", "(GMT +08:45) Eucla"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Australian Eastern Standard Time (New South Wales)", "Australia/Sydney", "(GMT +10:00) Canberra, Melbourne, Sydney"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Australian Eastern Standard Time (Queensland)", "Australia/Brisbane", "(GMT +10:00) Brisbane"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Azerbaijan Standard Time", "Asia/Baku", "(GMT +04:00) Baku"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Azores Standard Time", "Atlantic/Azores", "(GMT -01:00) Azores"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Bahia Standard Time", "America/Bahia", "(GMT -03:00) Salvador"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Bangladesh Standard Time", "Asia/Dhaka", "(GMT +06:00) Dhaka"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Belarus Standard Time", "Europe/Minsk", "(GMT +03:00) Minsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Bolivia Time", "America/La_Paz", "(GMT -04:00) Georgetown, La Paz, Manaus, San Juan"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Bougainville Standard Time", "Pacific/Bougainville", "(GMT +11:00) Bougainville Island"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Canada Central Standard Time", "America/Regina", "(GMT -06:00) Saskatchewan"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Cape Verde Standard Time", "Atlantic/Cape_Verde", "(GMT -01:00) Cabo Verde Is."));
        ZONEMAPPINGS.add(new TimeZoneMapping("Cape Verde Standard Time", "Atlantic/Cape_Verde", "(GMT -01:00) Cape Verde Is."));
        ZONEMAPPINGS.add(new TimeZoneMapping("Caucasus Standard Time", "Asia/Yerevan", "(GMT +04:00) Yerevan"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Cen. Australia Standard Time", "Australia/Adelaide", "(GMT +09:30) Adelaide"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central America Standard Time", "America/Guatemala", "(GMT -06:00) Central America"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Asia Standard Time", "Asia/Almaty", "(GMT +06:00) Astana"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Brazilian Standard Time", "America/Cuiaba", "(GMT -04:00) Cuiaba"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Europe Standard Time", "Europe/Budapest", "(GMT +01:00) Belgrade, Bratislava, Budapest, Ljubljana, Prague"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central European Standard Time", "Europe/Warsaw", "(GMT +01:00) Sarajevo, Skopje, Warsaw, Zagreb"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central European Time", "Europe/Warsaw", "(GMT +01:00) Sarajevo, Skopje, Warsaw, Zagreb"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Pacific Standard Time", "Pacific/Guadalcanal", "(GMT +11:00) Solomon Is., New Caledonia"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time (Mexico)", "America/Mexico_City", "(GMT -06:00) Guadalajara, Mexico City, Monterrey"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "(GMT -06:00) Central Time (US & Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "(GMT -06:00) Central Time (US %2A Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "(GMT -06:00) Central Time (US * Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "(GMT -06.00) Central Time (US & Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "(GMT -06.00) Central Time (US %2A Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "(GMT-06.00) Central Time (US * Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "Central Time (US & Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "Central Time (US %2A Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "Central Time (US * Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "Central"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "GMT -0600 (Standard) / GMT -0500 (Daylight)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "(GMT -06:00) Central Standard Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "(GMT -06:00) America/Chicago"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Chatham Islands Standard Time", "Pacific/Chatham", "(GMT +12:45) Chatham Islands"));
        ZONEMAPPINGS.add(new TimeZoneMapping("China Standard Time", "Asia/Shanghai", "(GMT +08:00) Beijing, Chongqing, Hong Kong, Urumqi"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Colombia Time", "America/Bogota", "(GMT -05:00) Bogota, Lima, Quito"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Colombia Time", "America/Bogota", "(GMT -05:00) Bogot\u00e1, Lima, Quito, Rio Branco"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Cuba Standard Time", "America/Havana", "(GMT -05:00) Havana"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Dateline Standard Time", "Etc/GMT+12", "(GMT -12:00) International Date Line West"));
        ZONEMAPPINGS.add(new TimeZoneMapping("E. Africa Standard Time", "Africa/Nairobi", "(GMT +03:00) Nairobi"));
        ZONEMAPPINGS.add(new TimeZoneMapping("E. Australia Standard Time", "Australia/Brisbane", "(GMT +10:00) Brisbane"));
        ZONEMAPPINGS.add(new TimeZoneMapping("E. Europe Standard Time", "Europe/Minsk", "(GMT +02:00) Minsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("E. Europe Standard Time", "Europe/Chisinau", "(GMT +02:00) Chisinau"));
        ZONEMAPPINGS.add(new TimeZoneMapping("E. Europe Standard Time", "Europe/Chisinau", "(GMT +02:00) E. Europe"));
        ZONEMAPPINGS.add(new TimeZoneMapping("E. South America Standard Time", "America/Sao_Paulo", "(GMT -03:00) Brasilia"));
        ZONEMAPPINGS.add(new TimeZoneMapping("E. South America Standard Time", "America/Sao_Paulo", "(GMT -03:00) Bras\u00edlia"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern European Time", "Europe/Chisinau", "(GMT +02:00) Chisinau"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "Eastern Time (US & Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "Eastern Time (US %2A Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "Eastern Time (US * Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "(GMT -05:00) Eastern Time (US & Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "(GMT -05:00) Eastern Time (US %2A Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "(GMT -05:00) Eastern Time (US * Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "(GMT -05.00) America/New_York"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "(GMT -05:00) Eastern Standard Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "GMT -0500 (Standard) / GMT -0400 (Daylight)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time (Mexico)", "America/Cancun", "(GMT -05:00) Chetumal"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Easter Island Standard Time", "Chile/EasterIsland", "(GMT -06:00) Easter Island"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Egypt Standard Time", "Africa/Cairo", "(GMT +02:00) Cairo"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Ekaterinburg Standard Time", "Asia/Yekaterinburg", "(GMT +05:00) Ekaterinburg"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Fiji Standard Time", "Pacific/Fiji", "(GMT +12:00) Fiji, Marshall Is."));
        ZONEMAPPINGS.add(new TimeZoneMapping("FLE Standard Time", "Europe/Kiev", "(GMT +02:00) Helsinki, Kyiv, Riga, Sofia, Tallinn, Vilnius"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Georgian Standard Time", "Asia/Tbilisi", "(GMT +04:00) Tbilisi"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT Standard Time", "Europe/London", "(GMT) Dublin, Edinburgh, Lisbon, London"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT Standard Time", "Europe/London", "(GMT +00:00) Dublin, Edinburgh, Lisbon, London"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT Standard Time", "Europe/London", "(GMT) Greenwich Mean Time : Dublin, Edinburgh, Lisbon, London"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT Standard Time", "Europe/London", "GMT Standard Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT Standard Time", "Europe/London", "(GMT +00:00) Greenland (Danmarkshavn)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Greenland Standard Time", "America/Godthab", "(GMT -03:00) Greenland"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Greenwich Mean Time", "Atlantic/Reykjavik", "(GMT) Monrovia, Reykjavik"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Greenwich Mean Time", "Europe/London", "GMT -0000 (Standard) / GMT +0100 (Daylight)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Greenwich Mean Time", "Europe/London", "(GMT -00:00) Europe/London"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Greenwich Standard Time", "Atlantic/Reykjavik", "(GMT) Monrovia, Reykjavik"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Greenwich Standard Time", "Atlantic/Reykjavik", "(GMT +00:00) Monrovia, Reykjavik"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GTB Standard Time", "Europe/Istanbul", "(GMT +02:00) Athens, Bucharest, Istanbul"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GTB Standard Time", "Europe/Athens", "(GMT +02:00) Athens, Bucharest"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Haiti Standard Time", "America/Port-au-Prince", "(GMT -05:00) Haiti"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Hawaiian Standard Time", "Pacific/Honolulu", "(GMT -10:00) Hawaii"));
        ZONEMAPPINGS.add(new TimeZoneMapping("India Standard Time", "Asia/Calcutta", "(GMT +05:30) Chennai, Kolkata, Mumbai, New Delhi"));
        ZONEMAPPINGS.add(new TimeZoneMapping("India Standard Time", "Asia/Calcutta", "India Standard Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Indochina Time", "Asia/Bangkok", "(UTC+07:00) Bangkok, Hanoi, Jakarta"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Iran Standard Time", "Asia/Tehran", "(GMT +03:30) Tehran"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Israel Standard Time", "Asia/Jerusalem", "(GMT +02:00) Jerusalem"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Jordan Standard Time", "Asia/Amman", "(GMT +02:00) Amman"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Kaliningrad Standard Time", "Europe/Kaliningrad", "(GMT +02:00) Kaliningrad (RTZ 1)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Kaliningrad Standard Time", "Europe/Kaliningrad", "(GMT +02:00) Kaliningrad"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Kamchatka Standard Time", "Asia/Kamchatka", "(GMT +12:00) Petropavlovsk-Kamchatsky - Old"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Korea Standard Time", "Asia/Seoul", "(GMT +09:00) Seoul"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Libya Standard Time", "Africa/Tripoli", "(GMT +02:00) Tripoli"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Line Islands Standard Time", "Pacific/Kiritimati", "(GMT +14:00) Kiritimati Island"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Lord Howe Standard Time", "Australia/Lord_Howe", "(GMT +10:30) Lord Howe Island"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Magadan Standard Time", "Asia/Magadan", "(GMT +11:00) Magadan"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Magallanes Standard Time", "America/Punta_Arenas", "(GMT -03:00) Punta Arenas"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Marquesas Standard Time", "Pacific/Marquesas", "(GMT -09:30) Marquesas Islands"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Mauritius Standard Time", "Indian/Mauritius", "(GMT +04:00) Port Louis"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Mid-Atlantic Standard Time", "Etc/GMT+2", "(GMT -02:00) Mid-Atlantic"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Middle East Standard Time", "Asia/Beirut", "(GMT +02:00) Beirut"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Montevideo Standard Time", "America/Montevideo", "(GMT -03:00) Montevideo"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Morocco Standard Time", "Africa/Casablanca", "(GMT) Casablanca"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Morocco Standard Time", "Africa/Casablanca", "(GMT -00:00) Casablanca"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Morocco Standard Time", "Africa/Casablanca", "(GMT +00:00) Casablanca"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Morocco Standard Time", "Africa/Casablanca", "(GMT +01:00) Casablanca"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Mountain Standard Time (Mexico)", "America/Chihuahua", "(GMT -07:00) Chihuahua, La Paz, Mazatlan"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Mountain Standard Time", "America/Denver", "(GMT -07:00) Mountain Time (US & Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Mountain Standard Time", "America/Denver", "(GMT -07:00) Mountain Time (US %2A Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Mountain Standard Time", "America/Denver", "(GMT -07:00) Mountain Time (US * Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Myanmar Standard Time", "Asia/Rangoon", "(GMT +06:30) Yangon (Rangoon)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("N. Central Asia Standard Time", "Asia/Novosibirsk", "(GMT +06:00) Novosibirsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Namibia Standard Time", "Africa/Windhoek", "(GMT +02:00) Windhoek"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Nepal Standard Time", "Asia/Katmandu", "(GMT +05:45) Kathmandu"));
        ZONEMAPPINGS.add(new TimeZoneMapping("New Zealand Standard Time", "Pacific/Auckland", "(GMT +12:00) Auckland, Wellington"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Newfoundland Standard Time", "America/St_Johns", "(GMT -03:30) Newfoundland"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Norfolk Standard Time", "Pacific/Norfolk", "(GMT +11:00) Norfolk Island"));
        ZONEMAPPINGS.add(new TimeZoneMapping("North Asia East Standard Time", "Asia/Irkutsk", "(GMT +08:00) Irkutsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("North Asia Standard Time", "Asia/Krasnoyarsk", "(GMT +07:00) Krasnoyarsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("North Korea Standard Time", "Asia/Pyongyang", "(GMT +08:30) Pyongyang"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific SA Standard Time", "America/Santiago", "(GMT -04:00) Santiago"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time (Mexico)", "America/Tijuana", "(GMT -08:00) Baja California"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time", "America/Los_Angeles", "(GMT -08:00) Pacific Time (US & Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time", "America/Los_Angeles", "(GMT -08:00) Pacific Time (US &2A Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time", "America/Los_Angeles", "(GMT -08:00) Pacific Time (US * Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time", "America/Los_Angeles", "(GMT -08:00) Pacific Time (US & Canada); Tijuana"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time", "America/Los_Angeles", "(GMT -08:00) Pacific Time (US %2A Canada); Tijuana"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time", "America/Los_Angeles", "(GMT -08:00) Pacific Time (US * Canada); Tijuana"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time", "America/Los_Angeles", "(GMT -08:00) Pacific Standard Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pakistan Standard Time", "Asia/Karachi", "(GMT +05:00) Islamabad, Karachi"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Paraguay Standard Time", "America/Asuncion", "(GMT -04:00) Asuncion"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Romance Standard Time", "Europe/Paris", "(GMT +01:00) Brussels, Copenhagen, Madrid, Paris"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Romance Standard Time", "Europe/Paris", "(UTC+01:00) Bruxelles, Copenhague, Madrid, Paris"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Romance Standard Time", "Europe/Paris", "(UTC+01:00) Brussels, Copenhagen"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Russian Standard Time", "Europe/Moscow", "(GMT +03:00) Moscow, St. Petersburg, Volgograd"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Russian Standard Time", "Europe/Moscow", "(GMT +03:00) Moscow, St. Petersburg"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Russia Time Zone 3", "Europe/Samara", "(GMT +04:00) Izhevsk, Samara (RTZ 3)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Russia Time Zone 3", "Europe/Samara", "(GMT +04:00) Izhevsk, Samara"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Russia Time Zone 10", "Asia/Srednekolymsk", "(GMT +11:00) Chokurdakh (RTZ 10)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Russia Time Zone 10", "Asia/Srednekolymsk", "(GMT +11:00) Chokurdakh"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Russia Time Zone 11", "Asia/Anadyr", "(GMT +12:00) Anadyr, Petropavlovsk-Kamchatsky"));
        ZONEMAPPINGS.add(new TimeZoneMapping("SA Eastern Standard Time", "America/Cayenne", "(GMT -03:00) Cayenne, Fortaleza"));
        ZONEMAPPINGS.add(new TimeZoneMapping("SA Pacific Standard Time", "America/Bogota", "(GMT -05:00) Bogota, Lima, Quito"));
        ZONEMAPPINGS.add(new TimeZoneMapping("SA Pacific Standard Time", "America/Bogota", "(GMT -05:00) Bogot\u00e1, Lima, Quito, Rio Branco"));
        ZONEMAPPINGS.add(new TimeZoneMapping("SA Western Standard Time", "America/La_Paz", "(GMT -04:00) Georgetown, La Paz, Manaus, San Juan"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Saint Pierre Standard Time", "America/Miquelon", "(GMT -03:00) Saint Pierre and Miquelon"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Sakhalin Standard Time", "Asia/Sakhalin", "(GMT +11:00) Sakhalin"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Samoa Standard Time", "Pacific/Apia", "(GMT -11:00) Samoa"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Samoa Standard Time", "Pacific/Apia", "(GMT +13:00) Samoa"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Saratov Standard Time", "Europe/Saratov", "(GMT +04:00) Saratov"));
        ZONEMAPPINGS.add(new TimeZoneMapping("SE Asia Standard Time", "Asia/Bangkok", "(GMT +07:00) Bangkok, Hanoi, Jakarta"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Singapore Standard Time", "Asia/Singapore", "(GMT +08:00) Kuala Lumpur, Singapore"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Singapore Time", "Asia/Singapore", "(GMT +08:00) Kuala Lumpur, Singapore"));
        ZONEMAPPINGS.add(new TimeZoneMapping("South Africa Standard Time", "Africa/Johannesburg", "(GMT +02:00) Harare, Pretoria"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Sri Lanka Standard Time", "Asia/Colombo", "(GMT +05:30) Sri Jayawardenepura"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Sudan Standard Time", "Africa/Khartoum", "(GMT +02:00) Khartoum"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Syria Standard Time", "Asia/Damascus", "(GMT +02:00) Damascus"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Taipei Standard Time", "Asia/Taipei", "(GMT +08:00) Taipei"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Tasmania Standard Time", "Australia/Hobart", "(GMT +10:00) Hobart"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Tocantins Standard Time", "America/Araguaina", "(GMT -03:00) Araguaina"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Tokyo Standard Time", "Asia/Tokyo", "(GMT +09:00) Osaka, Sapporo, Tokyo"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Tonga Standard Time", "Pacific/Tongatapu", "(GMT +13:00) Nuku'alofa"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Tonga Standard Time", "Pacific/Tongatapu", "(GMT +14:00) Nuku'alofa Island"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Tomsk Standard Time", "Asia/Tomsk", "(GMT +07:00) Tomsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Transbaikal Standard Time", "Asia/Chita", "(GMT +09:00) Chita"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Turkey Standard Time", "Europe/Istanbul", "(GMT +03:00) Istanbul"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Turks And Caicos Standard Time", "America/Grand_Turk", "(GMT -04:00) Turks and Caicos"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Turks And Caicos Standard Time", "America/Grand_Turk", "(GMT -05:00) Turks and Caicos"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Ulaanbaatar Standard Time", "Asia/Ulaanbaatar", "(GMT +08:00) Ulaanbaatar"));
        ZONEMAPPINGS.add(new TimeZoneMapping("US Eastern Standard Time", "America/Indianapolis", "(GMT -05:00) Indiana (East)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("US Mountain Standard Time", "America/Phoenix", "(GMT -07:00) Arizona"));
        ZONEMAPPINGS.add(new TimeZoneMapping("UTC-08", "Etc/GMT+8", "(GMT -08:00) Coordinated Universal Time-08"));
        ZONEMAPPINGS.add(new TimeZoneMapping("UTC-09", "Etc/GMT+9", "(GMT -09:00) Coordinated Universal Time-09"));
        ZONEMAPPINGS.add(new TimeZoneMapping("UTC+13", "Etc/GMT-13", "(GMT +13:00) Coordinated Universal Time+13"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT ", "Etc/GMT", "(GMT) Coordinated Universal Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT +12", "Etc/GMT-12", "(GMT +12:00) Coordinated Universal Time+12"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT -02", "Etc/GMT+2", "(GMT -02:00) Coordinated Universal Time-02"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT -11", "Etc/GMT+11", "(GMT -11:00) Coordinated Universal Time-11"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Venezuela Standard Time", "America/Caracas", "(GMT -04:30) Caracas"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Venezuela Standard Time", "America/Caracas", "(GMT -04:00) Caracas"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Venezuela Time", "America/Caracas", "(GMT -04:00) Caracas"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Vladivostok Standard Time", "Asia/Vladivostok", "(GMT +10:00) Vladivostok"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Australia Standard Time", "Australia/Perth", "(GMT +08:00) Perth"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Central Africa Standard Time", "Africa/Lagos", "(GMT +01:00) West Central Africa"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Mongolia Standard Time", "Asia/Hovd", "(GMT +07:00) Hovd"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Europe Standard Time", "Europe/Berlin", "(GMT +01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Europe Standard Time", "Europe/Berlin", "(GMT +01:00) Amsterdam, Berlin, Berne, Rome, Stockholm, Vienne"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Europe Standard Time", "Europe/Berlin", "(GMT +01:00) Amsterdam, Berlino, Berna, Roma, Stoccolma, Vienna"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Europe Standard Time", "Europe/Berlin", "(GMT + 1.00 h) Amsterdam, Berlino, Berna, Roma, Stoccolma, Vienna"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Europe Standard Time", "Europe/Berlin", "W. Europe Standard Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("West Asia Standard Time", "Asia/Tashkent", "(GMT +05:00) Tashkent"));
        ZONEMAPPINGS.add(new TimeZoneMapping("West Bank Standard Time", "Asia/Hebron", "(GMT +02:00) Gaza, Hebron"));
        ZONEMAPPINGS.add(new TimeZoneMapping("West Pacific Standard Time", "Pacific/Port_Moresby", "(GMT +10:00) Guam, Port Moresby"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Yakutsk Standard Time", "Asia/Yakutsk", "(GMT +09:00) Yakutsk"));
        // Add versions without spaces between GMT and the time offset.
        // This means we have more than one mapping for each Olson timezone.
        // This isn't a problem at the moment, as we only care about mapping
        // from Microsoft to Olson timezones, not the other direction. If a
        // unique mapping is required, this List should be updated to match the
        // latest CLDR mapping.
        ZONEMAPPINGS.add(new TimeZoneMapping("Afghanistan Standard Time", "Asia/Kabul", "(GMT+04:30) Kabul"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Alaska Standard Time", "America/Anchorage", "(GMT-09:00) Alaska"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Alaskan Standard Time", "America/Anchorage", "(GMT-09:00) Alaska"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Aleutian Standard Time", "America/Adak", "(GMT-10:00) Aleutian Islands"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Altai Standard Time", "Asia/Barnaul", "(GMT+07:00) Barnaul, Gorno-Altaysk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Arab Standard Time", "Asia/Riyadh", "(GMT+03:00) Kuwait, Riyadh"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Arabian Standard Time", "Asia/Dubai", "(GMT+04:00) Abu Dhabi, Muscat"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Arabic Standard Time", "Asia/Baghdad", "(GMT+03:00) Baghdad"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Argentina Standard Time", "America/Buenos_Aires", "(GMT-03:00) Buenos Aires"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Argentine Time", "America/Buenos_Aires", "(GMT-03:00) Buenos Aires"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Atlantic Standard Time", "America/Halifax", "(GMT-04:00) Atlantic Time (Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Astrakhan Standard Time", "Europe/Astrakhan", "(GMT+04:00) Astrakhan, Ulyanovsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("AUS Central Standard Time", "Australia/Darwin", "(GMT+09:30) Darwin"));
        ZONEMAPPINGS.add(new TimeZoneMapping("AUS Eastern Standard Time", "Australia/Sydney", "(GMT+10:00) Canberra, Melbourne, Sydney"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Aus Central W. Standard Time", "Australia/Eucla", "(GMT+08:45) Eucla"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Australian Eastern Standard Time (New South Wales)", "Australia/Sydney", "(GMT+10:00) Canberra, Melbourne, Sydney"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Australian Eastern Standard Time (Queensland)", "Australia/Brisbane", "(GMT+10:00) Brisbane"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Azerbaijan Standard Time", "Asia/Baku", "(GMT+04:00) Baku"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Azores Standard Time", "Atlantic/Azores", "(GMT-01:00) Azores"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Bahia Standard Time", "America/Bahia", "(GMT-03:00) Salvador"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Bangladesh Standard Time", "Asia/Dhaka", "(GMT+06:00) Dhaka"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Belarus Standard Time", "Europe/Minsk", "(GMT+03:00) Minsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Bolivia Time", "America/La_Paz", "(GMT-04:00) Georgetown, La Paz, Manaus, San Juan"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Bougainville Standard Time", "Pacific/Bougainville", "(GMT+11:00) Bougainville Island"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Canada Central Standard Time", "America/Regina", "(GMT-06:00) Saskatchewan"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Cape Verde Standard Time", "Atlantic/Cape_Verde", "(GMT-01:00) Cabo Verde Is."));
        ZONEMAPPINGS.add(new TimeZoneMapping("Cape Verde Standard Time", "Atlantic/Cape_Verde", "(GMT-01:00) Cape Verde Is."));
        ZONEMAPPINGS.add(new TimeZoneMapping("Caucasus Standard Time", "Asia/Yerevan", "(GMT+04:00) Yerevan"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Cen. Australia Standard Time", "Australia/Adelaide", "(GMT+09:30) Adelaide"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central America Standard Time", "America/Guatemala", "(GMT-06:00) Central America"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Asia Standard Time", "Asia/Almaty", "(GMT+06:00) Astana"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Brazilian Standard Time", "America/Cuiaba", "(GMT-04:00) Cuiaba"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Europe Standard Time", "Europe/Budapest", "(GMT+01:00) Belgrade, Bratislava, Budapest, Ljubljana, Prague"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central European Standard Time", "Europe/Warsaw", "(GMT+01:00) Sarajevo, Skopje, Warsaw, Zagreb"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central European Time", "Europe/Warsaw", "(GMT+01:00) Sarajevo, Skopje, Warsaw, Zagreb"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Pacific Standard Time", "Pacific/Guadalcanal", "(GMT+11:00) Solomon Is., New Caledonia"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time (Mexico)", "America/Mexico_City", "(GMT-06:00) Guadalajara, Mexico City, Monterrey"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "(GMT-06:00) Central Time (US & Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "(GMT-06:00) Central Time (US %2A Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "(GMT-06:00) Central Time (US * Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "(GMT-06.00) Central Time (US & Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "(GMT-06.00) Central Time (US %2A Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "(GMT-06.00) Central Time (US * Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "GMT-0600 (Standard) / GMT-0500 (Daylight)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "(GMT-06:00) Central Standard Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "(GMT-06:00) America/Chicago"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Chatham Islands Standard Time", "Pacific/Chatham", "(GMT+12:45) Chatham Islands"));
        ZONEMAPPINGS.add(new TimeZoneMapping("China Standard Time", "Asia/Shanghai", "(GMT+08:00) Beijing, Chongqing, Hong Kong, Urumqi"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Colombia Time", "America/Bogota", "(GMT-05:00) Bogota, Lima, Quito"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Colombia Time", "America/Bogota", "(GMT-05:00) Bogot\u00e1, Lima, Quito, Rio Branco"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Cuba Standard Time", "America/Havana", "(GMT-05:00) Havana"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Dateline Standard Time", "Etc/GMT+12", "(GMT-12:00) International Date Line West"));
        ZONEMAPPINGS.add(new TimeZoneMapping("E. Africa Standard Time", "Africa/Nairobi", "(GMT+03:00) Nairobi"));
        ZONEMAPPINGS.add(new TimeZoneMapping("E. Australia Standard Time", "Australia/Brisbane", "(GMT+10:00) Brisbane"));
        ZONEMAPPINGS.add(new TimeZoneMapping("E. Europe Standard Time", "Europe/Minsk", "(GMT+02:00) Minsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("E. Europe Standard Time", "Europe/Chisinau", "(GMT+02:00) Chisinau"));
        ZONEMAPPINGS.add(new TimeZoneMapping("E. Europe Standard Time", "Europe/Chisinau", "(GMT+02:00) E. Europe"));
        ZONEMAPPINGS.add(new TimeZoneMapping("E. South America Standard Time", "America/Sao_Paulo", "(GMT-03:00) Brasilia"));
        ZONEMAPPINGS.add(new TimeZoneMapping("E. South America Standard Time", "America/Sao_Paulo", "(GMT-03:00) Bras\u00edlia"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern European Time", "Europe/Chisinau", "(GMT+02:00) Chisinau"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "(GMT-05:00) Eastern Time (US & Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "(GMT-05:00) Eastern Time (US %2A Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "(GMT-05:00) Eastern Time (US * Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "(GMT-05.00) Eastern Time (US & Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "(GMT-05.00) Eastern Time (US %2A Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "(GMT-05.00) Eastern Time (US * Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "(GMT-05:00) America/New_York"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "(GMT-05.00) America/New_York"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "(GMT-05:00) Eastern Standard Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "GMT-0500 (Standard) / GMT-0400 (Daylight)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time (Mexico)", "America/Cancun", "(GMT-05:00) Chetumal"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Easter Island Standard Time", "Chile/EasterIsland", "(GMT-06:00) Easter Island"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Egypt Standard Time", "Africa/Cairo", "(GMT+02:00) Cairo"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Ekaterinburg Standard Time", "Asia/Yekaterinburg", "(GMT+05:00) Ekaterinburg"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Fiji Standard Time", "Pacific/Fiji", "(GMT+12:00) Fiji, Marshall Is."));
        ZONEMAPPINGS.add(new TimeZoneMapping("FLE Standard Time", "Europe/Kiev", "(GMT+02:00) Helsinki, Kyiv, Riga, Sofia, Tallinn, Vilnius"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Georgian Standard Time", "Asia/Tbilisi", "(GMT+04:00) Tbilisi"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT Standard Time", "Europe/London", "(GMT) Dublin, Edinburgh, Lisbon, London"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT Standard Time", "Europe/London", "(GMT+00:00) Dublin, Edinburgh, Lisbon, London"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT Standard Time", "Europe/London", "(GMT) Greenwich Mean Time : Dublin, Edinburgh, Lisbon, London"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT Standard Time", "Europe/London", "GMTStandard Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Greenland Standard Time", "America/Godthab", "(GMT-03:00) Greenland"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Greenwich Mean Time", "Atlantic/Reykjavik", "(GMT) Monrovia, Reykjavik"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Greenwich Mean Time", "Europe/London", "GMT-0000 (Standard) / GMT+0100 (Daylight)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Greenwich Mean Time", "Europe/London", "(GMT-00:00) Europe/London"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Greenwich Standard Time", "Atlantic/Reykjavik", "(GMT) Monrovia, Reykjavik"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Greenwich Standard Time", "Atlantic/Reykjavik", "(GMT+00:00) Monrovia, Reykjavik"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GTB Standard Time", "Europe/Istanbul", "(GMT+02:00) Athens, Bucharest, Istanbul"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GTB Standard Time", "Europe/Athens", "(GMT+02:00) Athens, Bucharest"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Haiti Standard Time", "America/Port-au-Prince", "(GMT-05:00) Haiti"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Hawaiian Standard Time", "Pacific/Honolulu", "(GMT-10:00) Hawaii"));
        ZONEMAPPINGS.add(new TimeZoneMapping("India Standard Time", "Asia/Calcutta", "(GMT+05:30) Chennai, Kolkata, Mumbai, New Delhi"));
        ZONEMAPPINGS.add(new TimeZoneMapping("India Standard Time", "Asia/Calcutta", "(GMT+05:30) India Standard Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("India Standard Time", "Asia/Calcutta", "India Standard Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Indochina Time", "Asia/Bangkok", "(UTC+07:00) Bangkok, Hanoi, Jakarta"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Iran Standard Time", "Asia/Tehran", "(GMT+03:30) Tehran"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Israel Standard Time", "Asia/Jerusalem", "(GMT+02:00) Jerusalem"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Jordan Standard Time", "Asia/Amman", "(GMT+02:00) Amman"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Kaliningrad Standard Time", "Europe/Kaliningrad", "(GMT+02:00) Kaliningrad (RTZ 1)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Kaliningrad Standard Time", "Europe/Kaliningrad", "(GMT+02:00) Kaliningrad"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Kamchatka Standard Time", "Asia/Kamchatka", "(GMT+12:00) Petropavlovsk-Kamchatsky- Old"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Korea Standard Time", "Asia/Seoul", "(GMT+09:00) Seoul"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Libya Standard Time", "Africa/Tripoli", "(GMT+02:00) Tripoli"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Line Islands Standard Time", "Pacific/Kiritimati", "(GMT+14:00) Kiritimati Island"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Lord Howe Standard Time", "Australia/Lord_Howe", "(GMT+10:30) Lord Howe Island"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Magadan Standard Time", "Asia/Magadan", "(GMT+11:00) Magadan"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Magallanes Standard Time", "America/Punta_Arenas", "(GMT-03:00) Punta Arenas"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Marquesas Standard Time", "Pacific/Marquesas", "(GMT-09:30) Marquesas Islands"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Mauritius Standard Time", "Indian/Mauritius", "(GMT+04:00) Port Louis"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Mid-Atlantic Standard Time", "Etc/GMT+2", "(GMT-02:00) Mid-Atlantic"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Middle East Standard Time", "Asia/Beirut", "(GMT+02:00) Beirut"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Montevideo Standard Time", "America/Montevideo", "(GMT-03:00) Montevideo"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Morocco Standard Time", "Africa/Casablanca", "(GMT) Casablanca"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Morocco Standard Time", "Africa/Casablanca", "(GMT-00:00) Casablanca"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Morocco Standard Time", "Africa/Casablanca", "(GMT+00:00) Casablanca"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Morocco Standard Time", "Africa/Casablanca", "(GMT+01:00) Casablanca"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Mountain Standard Time (Mexico)", "America/Chihuahua", "(GMT-07:00) Chihuahua, La Paz, Mazatlan"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Mountain Standard Time", "America/Denver", "(GMT-07:00) Mountain Time (US & Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Mountain Standard Time", "America/Denver", "(GMT-07:00) Mountain Time (US %2A Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Mountain Standard Time", "America/Denver", "(GMT-07:00) Mountain Time (US 8 Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Mountain Standard Time", "America/Denver", "(GMT-07:00) America/Denver"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Mountain Standard Time", "America/Denver", "(GMT-07:00) Mountain Standard Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Myanmar Standard Time", "Asia/Rangoon", "(GMT+06:30) Yangon (Rangoon)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("N. Central Asia Standard Time", "Asia/Novosibirsk", "(GMT+06:00) Novosibirsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Namibia Standard Time", "Africa/Windhoek", "(GMT+02:00) Windhoek"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Nepal Standard Time", "Asia/Katmandu", "(GMT+05:45) Kathmandu"));
        ZONEMAPPINGS.add(new TimeZoneMapping("New Zealand Standard Time", "Pacific/Auckland", "(GMT+12:00) Auckland, Wellington"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Newfoundland Standard Time", "America/St_Johns", "(GMT-03:30) Newfoundland"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Norfolk Standard Time", "Pacific/Norfolk", "(GMT+11:00) Norfolk Island"));
        ZONEMAPPINGS.add(new TimeZoneMapping("North Asia East Standard Time", "Asia/Irkutsk", "(GMT+08:00) Irkutsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("North Asia Standard Time", "Asia/Krasnoyarsk", "(GMT+07:00) Krasnoyarsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("North Korea Standard Time", "Asia/Pyongyang", "(GMT+08:30) Pyongyang"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific SA Standard Time", "America/Santiago", "(GMT-04:00) Santiago"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time (Mexico)", "America/Tijuana", "(GMT-08:00) Baja California"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time", "America/Los_Angeles", "(GMT-08:00) Pacific Time (US & Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time", "America/Los_Angeles", "(GMT-08:00) Pacific Time (US %2A Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time", "America/Los_Angeles", "(GMT-08:00) Pacific Time (US * Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time", "America/Los_Angeles", "(GMT-08:00) Pacific Time (US & Canada); Tijuana"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time", "America/Los_Angeles", "(GMT-08:00) Pacific Time (US %2A Canada); Tijuana"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time", "America/Los_Angeles", "(GMT-08:00) Pacific Time (US * Canada); Tijuana"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time", "America/Los_Angeles", "(GMT-08:00) Pacific Standard Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time", "America/Los_Angeles", "GMT -0800 (Standard) / GMT -0700 (Daylight)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pakistan Standard Time", "Asia/Karachi", "(GMT+05:00) Islamabad, Karachi"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Paraguay Standard Time", "America/Asuncion", "(GMT-04:00) Asuncion"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Romance Standard Time", "Europe/Paris", "(GMT+01:00) Brussels, Copenhagen, Madrid, Paris"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Romance Standard Time", "Europe/Paris", "(GMT+01:00) Bruxelles, Copenhague, Madrid, Paris"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Russian Standard Time", "Europe/Moscow", "(GMT+03:00) Moscow, St. Petersburg, Volgograd"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Russian Standard Time", "Europe/Moscow", "(GMT+03:00) Moscow, St. Petersburg"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Russia Time Zone 3", "Europe/Samara", "(GMT+04:00) Izhevsk, Samara (RTZ 3)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Russia Time Zone 3", "Europe/Samara", "(GMT+04:00) Izhevsk, Samara"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Russia Time Zone 10", "Asia/Srednekolymsk", "(GMT+11:00) Chokurdakh (RTZ 10)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Russia Time Zone 10", "Asia/Srednekolymsk", "(GMT+11:00) Chokurdakh"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Russia Time Zone 11", "Asia/Anadyr", "(GMT+12:00) Anadyr, Petropavlovsk-Kamchatsky"));
        ZONEMAPPINGS.add(new TimeZoneMapping("SA Eastern Standard Time", "America/Cayenne", "(GMT-03:00) Cayenne, Fortaleza"));
        ZONEMAPPINGS.add(new TimeZoneMapping("SA Pacific Standard Time", "America/Bogota", "(GMT-05:00) Bogota, Lima, Quito"));
        ZONEMAPPINGS.add(new TimeZoneMapping("SA Pacific Standard Time", "America/Bogota", "(GMT-05:00) Bogot\u00e1, Lima, Quito, Rio Branco"));
        ZONEMAPPINGS.add(new TimeZoneMapping("SA Western Standard Time", "America/La_Paz", "(GMT-04:00) Georgetown, La Paz, Manaus, San Juan"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Saint Pierre Standard Time", "America/Miquelon", "(GMT-03:00) Saint Pierre and Miquelon"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Sakhalin Standard Time", "Asia/Sakhalin", "(GMT+11:00) Sakhalin"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Samoa Standard Time", "Pacific/Apia", "(GMT-11:00) Samoa"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Samoa Standard Time", "Pacific/Apia", "(GMT+13:00) Samoa"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Saratov Standard Time", "Europe/Saratov", "(GMT+04:00) Saratov"));
        ZONEMAPPINGS.add(new TimeZoneMapping("SE Asia Standard Time", "Asia/Bangkok", "(GMT+07:00) Bangkok, Hanoi, Jakarta"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Singapore Standard Time", "Asia/Singapore", "(GMT+08:00) Kuala Lumpur, Singapore"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Singapore Time", "Asia/Singapore", "(GMT+08:00) Kuala Lumpur, Singapore"));
        ZONEMAPPINGS.add(new TimeZoneMapping("South Africa Standard Time", "Africa/Johannesburg", "(GMT+02:00) Harare, Pretoria"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Sri Lanka Standard Time", "Asia/Colombo", "(GMT+05:30) Sri Jayawardenepura"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Sudan Standard Time", "Africa/Khartoum", "(GMT+02:00) Khartoum"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Syria Standard Time", "Asia/Damascus", "(GMT+02:00) Damascus"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Taipei Standard Time", "Asia/Taipei", "(GMT+08:00) Taipei"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Tasmania Standard Time", "Australia/Hobart", "(GMT+10:00) Hobart"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Tocantins Standard Time", "America/Araguaina", "(GMT-03:00) Araguaina"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Tokyo Standard Time", "Asia/Tokyo", "(GMT+09:00) Osaka, Sapporo, Tokyo"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Tomsk Standard Time", "Asia/Tomsk", "(GMT+07:00) Tomsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Tonga Standard Time", "Pacific/Tongatapu", "(GMT+13:00) Nuku'alofa"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Tonga Standard Time", "Pacific/Tongatapu", "(GMT+14:00) Nuku'alofa Island"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Tomsk Standard Time", "Asia/Tomsk", "(GMT+07:00) Tomsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Transbaikal Standard Time", "Asia/Chita", "(GMT+09:00) Chita"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Turkey Standard Time", "Europe/Istanbul", "(GMT+03:00) Istanbul"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Turks And Caicos Standard Time", "America/Grand_Turk", "(GMT-04:00) Turks and Caicos"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Turks And Caicos Standard Time", "America/Grand_Turk", "(GMT-05:00) Turks and Caicos"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Ulaanbaatar Standard Time", "Asia/Ulaanbaatar", "(GMT+08:00) Ulaanbaatar"));
        ZONEMAPPINGS.add(new TimeZoneMapping("US Eastern Standard Time", "America/Indianapolis", "(GMT-05:00) Indiana (East)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("US Mountain Standard Time", "America/Phoenix", "(GMT-07:00) Arizona"));
        ZONEMAPPINGS.add(new TimeZoneMapping("UTC-08", "Etc/GMT+8", "(GMT-08:00) Coordinated Universal Time-08"));
        ZONEMAPPINGS.add(new TimeZoneMapping("UTC-09", "Etc/GMT+9", "(GMT-09:00) Coordinated Universal Time-09"));
        ZONEMAPPINGS.add(new TimeZoneMapping("UTC+13", "Etc/GMT-13", "(GMT+13:00) Coordinated Universal Time+13"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT ", "Etc/GMT", "(GMT) Coordinated Universal Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT +12", "Etc/GMT-12", "(GMT+12:00) Coordinated Universal Time+12"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT -02", "Etc/GMT+2", "(GMT-02:00) Coordinated Universal Time-02"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT -11", "Etc/GMT+11", "(GMT-11:00) Coordinated Universal Time-11"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Venezuela Standard Time", "America/Caracas", "(GMT-04:30) Caracas"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Venezuela Standard Time", "America/Caracas", "(GMT-04:00) Caracas"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Venezuela Time", "America/Caracas", "(GMT-04:00) Caracas"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Vladivostok Standard Time", "Asia/Vladivostok", "(GMT+10:00) Vladivostok"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Australia Standard Time", "Australia/Perth", "(GMT+08:00) Perth"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Central Africa Standard Time", "Africa/Lagos", "(GMT+01:00) West Central Africa"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Mongolia Standard Time", "Asia/Hovd", "(GMT+07:00) Hovd"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Europe Standard Time", "Europe/Berlin", "(GMT+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Europe Standard Time", "Europe/Berlin", "(GMT+01:00) Amsterdam, Berlin, Berne, Rome, Stockholm, Vienne"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Europe Standard Time", "Europe/Berlin", "(GMT+01:00) Amsterdam, Berlino, Berna, Roma, Stoccolma, Vienna"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Europe Standard Time", "Europe/Berlin", "W. Europe Standard Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("West Asia Standard Time", "Asia/Tashkent", "(GMT+05:00) Tashkent"));
        ZONEMAPPINGS.add(new TimeZoneMapping("West Bank Standard Time", "Asia/Hebron", "(GMT+02:00) Gaza, Hebron"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Mongolia Standard Time", "Asia/Hovd", "(GMT+07:00) Hovd"));
        ZONEMAPPINGS.add(new TimeZoneMapping("West Pacific Standard Time", "Pacific/Port_Moresby", "(GMT+10:00) Guam, Port Moresby"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Yakutsk Standard Time", "Asia/Yakutsk", "(GMT+09:00) Yakutsk"));
        // Add up-to-date versions, which use UTC instead of GMT and have some
        // other slight changes. Data retrieved from
        // http://unicode.org/repos/cldr/trunk/common/supplemental/windowsZones.xml
        // Keep old versions in case they're needed for old versions of Outlook.
        //
        // This means we have more than one mapping for each Olson timezone.
        // This isn't a problem at the moment, as we only care about mapping
        // from Microsoft to Olson timezones, not the other direction. If a
        // unique mapping is required, this List should be updated to match the
        // latest CLDR mapping.
        ZONEMAPPINGS.add(new TimeZoneMapping("Afghanistan Standard Time", "Asia/Kabul", "(UTC+04:30) Kabul"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Alaska Standard Time", "America/Anchorage", "(UTC-09:00) Alaska"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Alaskan Standard Time", "America/Anchorage", "(UTC-09:00) Alaska"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Aleutian Standard Time", "America/Adak", "(UTC-10:00) Aleutian Islands"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Altai Standard Time", "Asia/Barnaul", "(UTC+07:00) Barnaul, Gorno-Altaysk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Arab Standard Time", "Asia/Riyadh", "(UTC+03:00) Kuwait, Riyadh"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Arabian Standard Time", "Asia/Dubai", "(UTC+04:00) Abu Dhabi, Muscat"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Arabic Standard Time", "Asia/Baghdad", "(UTC+03:00) Baghdad"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Argentina Standard Time", "America/Buenos_Aires", "(UTC-03:00) Buenos Aires"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Argentina Standard Time", "America/Buenos_Aires", "(UTC-03:00) City of Buenos Aires"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Argentine Time", "America/Buenos_Aires", "(UTC-03:00) Buenos Aires"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Atlantic Standard Time", "America/Halifax", "(UTC-04:00) Atlantic Time (Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Astrakhan Standard Time", "Europe/Astrakhan", "(UTC+04:00) Astrakhan, Ulyanovsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("AUS Central Standard Time", "Australia/Darwin", "(UTC+09:30) Darwin"));
        ZONEMAPPINGS.add(new TimeZoneMapping("AUS Eastern Standard Time", "Australia/Sydney", "(UTC+10:00) Canberra, Melbourne, Sydney"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Aus Central W. Standard Time", "Australia/Eucla", "(UTC+08:45) Eucla"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Australian Eastern Standard Time (New South Wales)", "Australia/Sydney", "(UTC+10:00) Canberra, Melbourne, Sydney"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Australian Eastern Standard Time (Queensland)", "Australia/Brisbane", "(UTC+10:00) Brisbane"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Azerbaijan Standard Time", "Asia/Baku", "(UTC+04:00) Baku"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Azores Standard Time", "Atlantic/Azores", "(UTC-01:00) Azores"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Bahia Standard Time", "America/Bahia", "(UTC-03:00) Salvador"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Bangladesh Standard Time", "Asia/Dhaka", "(UTC+06:00) Dhaka"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Belarus Standard Time", "Europe/Minsk", "(UTC+03:00) Minsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Bolivia Time", "America/La_Paz", "(UTC-04:00) Georgetown, La Paz, Manaus, San Juan"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Bougainville Standard Time", "Pacific/Bougainville", "(UTC+11:00) Bougainville Island"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Canada Central Standard Time", "America/Regina", "(UTC-06:00) Saskatchewan"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Cape Verde Standard Time", "Atlantic/Cape_Verde", "(UTC-01:00) Cabo Verde Is."));
        ZONEMAPPINGS.add(new TimeZoneMapping("Cape Verde Standard Time", "Atlantic/Cape_Verde", "(UTC-01:00) Cape Verde Is."));
        ZONEMAPPINGS.add(new TimeZoneMapping("Caucasus Standard Time", "Asia/Yerevan", "(UTC+04:00) Yerevan"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Cen. Australia Standard Time", "Australia/Adelaide", "(UTC+09:30) Adelaide"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central America Standard Time", "America/Guatemala", "(UTC-06:00) Central America"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Asia Standard Time", "Asia/Almaty", "(UTC+06:00) Astana"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Brazilian Standard Time", "America/Cuiaba", "(UTC-04:00) Cuiaba"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Europe Standard Time", "Europe/Budapest", "(UTC+01:00) Belgrade, Bratislava, Budapest, Ljubljana, Prague"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central European Standard Time", "Europe/Warsaw", "(UTC+01:00) Sarajevo, Skopje, Warsaw, Zagreb"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central European Time", "Europe/Warsaw", "(UTC+01:00) Sarajevo, Skopje, Warsaw, Zagreb"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Pacific Standard Time", "Pacific/Guadalcanal", "(UTC+11:00) Solomon Is., New Caledonia"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time (Mexico)", "America/Mexico_City", "(UTC-06:00) Guadalajara, Mexico City, Monterrey"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "(UTC-06:00) Central Time (US & Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "(UTC-06:00) Central Time (US %2A Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "(UTC-06:00) Central Time (US * Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "(UTC-06.00) Central Time (US & Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "(UTC-06.00) Central Time (US %2A Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "(UTC-06.00) Central Time (US * Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "UTC-0600 (Standard) / UTC-0500 (Daylight)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "(UTC-06:00) Central Standard Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Central Standard Time", "America/Chicago", "(UTC-06:00) America/Chicago"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Chatham Islands Standard Time", "Pacific/Chatham", "(UTC+12:45) Chatham Islands"));
        ZONEMAPPINGS.add(new TimeZoneMapping("China Standard Time", "Asia/Shanghai", "(UTC+08:00) Beijing, Chongqing, Hong Kong, Urumqi"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Colombia Time", "America/Bogota", "(UTC-05:00) Bogota, Lima, Quito"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Colombia Time", "America/Bogota", "(UTC-05:00) Bogot\u00e1, Lima, Quito, Rio Branco"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Cuba Standard Time", "America/Havana", "(UTC-05:00) Havana"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Dateline Standard Time", "Etc/GMT+12", "(UTC-12:00) International Date Line West"));
        ZONEMAPPINGS.add(new TimeZoneMapping("E. Africa Standard Time", "Africa/Nairobi", "(UTC+03:00) Nairobi"));
        ZONEMAPPINGS.add(new TimeZoneMapping("E. Australia Standard Time", "Australia/Brisbane", "(UTC+10:00) Brisbane"));
        ZONEMAPPINGS.add(new TimeZoneMapping("E. Europe Standard Time", "Europe/Minsk", "(UTC+02:00) Minsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("E. Europe Standard Time", "Europe/Chisinau", "(UTC+02:00) Chisinau"));
        ZONEMAPPINGS.add(new TimeZoneMapping("E. Europe Standard Time", "Europe/Chisinau", "(UTC+02:00) E. Europe"));
        ZONEMAPPINGS.add(new TimeZoneMapping("E. South America Standard Time", "America/Sao_Paulo", "(UTC-03:00) Brasilia"));
        ZONEMAPPINGS.add(new TimeZoneMapping("E. South America Standard Time", "America/Sao_Paulo", "(UTC-03:00) Bras\u00edlia"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern European Time", "Europe/Minsk", "(UTC+02:00) Minsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "(UTC-05:00) Eastern Time (US & Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "(UTC-05:00) Eastern Time (US %2A Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "(UTC-05:00) Eastern Time (US * Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "(UTC-05:00) America/New_York"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "(UTC-05.00) America/New_York"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "(UTC-05:00) Eastern Standard Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "(UTC-05:00) Est (É.-U. et Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time", "America/New_York", "UTC-0500 (Standard) / UTC-0400 (Daylight)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Eastern Standard Time (Mexico)", "America/Cancun", "(UTC-05:00) Chetumal"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Easter Island Standard Time", "Chile/EasterIsland", "(UTC-06:00) Easter Island"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Egypt Standard Time", "Africa/Cairo", "(UTC+02:00) Cairo"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Ekaterinburg Standard Time", "Asia/Yekaterinburg", "(UTC+05:00) Ekaterinburg"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Ekaterinburg Standard Time", "Asia/Yekaterinburg", "(UTC+05:00) Ekaterinburg (RTZ 4)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Fiji Standard Time", "Pacific/Fiji", "(UTC+12:00) Fiji, Marshall Is."));
        ZONEMAPPINGS.add(new TimeZoneMapping("Fiji Standard Time", "Pacific/Fiji", "(UTC+12:00) Fiji"));
        ZONEMAPPINGS.add(new TimeZoneMapping("FLE Standard Time", "Europe/Kiev", "(UTC+02:00) Helsinki, Kyiv, Riga, Sofia, Tallinn, Vilnius"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Georgian Standard Time", "Asia/Tbilisi", "(UTC+04:00) Tbilisi"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT Standard Time", "Europe/London", "(UTC) Dublin, Edinburgh, Lisbon, London"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT Standard Time", "Europe/London", "(UTC+00:00) Dublin, Edinburgh, Lisbon, London"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT Standard Time", "Europe/London", "(UTC) Greenwich Mean Time : Dublin, Edinburgh, Lisbon, London"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT Standard Time", "Europe/London", "UTC Standard Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT Standard Time", "Europe/London", "(UTC) Co-ordinated Universal Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT Standard Time", "Europe/London", "(UTC) Temps universel coordonné"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Greenland Standard Time", "America/Godthab", "(UTC-03:00) Greenland"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Greenwich Mean Time", "Europe/London", "UTC Standard Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Greenwich Mean Time", "Europe/London", "UTC-0000 (Standard) / UTC +0100 (Daylight)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Greenwich Mean Time", "Europe/London", "(UTC-00:00) Europe/London"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Greenwich Standard Time", "Atlantic/Reykjavik", "(UTC) Monrovia, Reykjavik"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Greenwich Standard Time", "Atlantic/Reykjavik", "(UTC+00:00) Monrovia, Reykjavik"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GTB Standard Time", "Europe/Istanbul", "(UTC+02:00) Athens, Bucharest, Istanbul"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GTB Standard Time", "Europe/Athens", "(UTC+02:00) Athens, Bucharest"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Haiti Standard Time", "America/Port-au-Prince", "(UTC-05:00) Haiti"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Hawaiian Standard Time", "Pacific/Honolulu", "(UTC-10:00) Hawaii"));
        ZONEMAPPINGS.add(new TimeZoneMapping("India Standard Time", "Asia/Calcutta", "(UTC+05:30) Chennai, Kolkata, Mumbai, New Delhi"));
        ZONEMAPPINGS.add(new TimeZoneMapping("India Standard Time", "Asia/Calcutta", "India Standard Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Indochina Time", "Asia/Bangkok", "(UTC+07:00) Bangkok, Hanoi, Jakarta"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Iran Standard Time", "Asia/Tehran", "(UTC+03:30) Tehran"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Israel Standard Time", "Asia/Jerusalem", "(UTC+02:00) Jerusalem"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Jordan Standard Time", "Asia/Amman", "(UTC+02:00) Amman"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Kaliningrad Standard Time", "Europe/Kaliningrad", "(UTC+02:00) Kaliningrad (RTZ 1)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Kaliningrad Standard Time", "Europe/Kaliningrad", "(UTC+02:00) Kaliningrad"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Kamchatka Standard Time", "Asia/Kamchatka", "(UTC+12:00) Petropavlovsk-Kamchatsky - Old"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Kamchatka Standard Time", "Asia/Kamchatka", "(UTC+12:00) Anadyr, Petropavlovsk-Kamchatsky (RTZ 11)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Korea Standard Time", "Asia/Seoul", "(UTC+09:00) Seoul"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Libya Standard Time", "Africa/Tripoli", "(UTC+02:00) Tripoli"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Line Islands Standard Time", "Pacific/Kiritimati", "(UTC+14:00) Kiritimati Island"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Lord Howe Standard Time", "Australia/Lord_Howe", "(UTC+10:30) Lord Howe Island"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Magadan Standard Time", "Asia/Magadan", "(UTC+11:00) Magadan"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Magadan Standard Time", "Asia/Magadan", "(UTC+10:00) Magadan"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Magallanes Standard Time", "America/Punta_Arenas", "(UTC-03:00) Punta Arenas"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Marquesas Standard Time", "Pacific/Marquesas", "(UTC-09:30) Marquesas Islands"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Marquesas Standard Time", "Pacific/Marquesas", "(UTC-09:30) Islas Marquesas"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Mauritius Standard Time", "Indian/Mauritius", "(UTC+04:00) Port Louis"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Mid-Atlantic Standard Time", "Etc/GMT+2", "(UTC-02:00) Mid-Atlantic"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Mid-Atlantic Standard Time", "Etc/GMT+2", "(UTC-02:00) Mid-Atlantic - Old"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Middle East Standard Time", "Asia/Beirut", "(UTC+02:00) Beirut"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Montevideo Standard Time", "America/Montevideo", "(UTC-03:00) Montevideo"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Morocco Standard Time", "Africa/Casablanca", "(UTC) Casablanca"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Morocco Standard Time", "Africa/Casablanca", "(UTC-00:00) Casablanca"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Morocco Standard Time", "Africa/Casablanca", "(UTC+00:00) Casablanca"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Morocco Standard Time", "Africa/Casablanca", "(UTC+01:00) Casablanca"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Mountain Standard Time (Mexico)", "America/Chihuahua", "(UTC-07:00) Chihuahua, La Paz, Mazatlan"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Mountain Standard Time", "America/Denver", "(UTC-07:00) Mountain Time (US & Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Mountain Standard Time", "America/Denver", "(UTC-07:00) Mountain Time (US %2A Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Mountain Standard Time", "America/Denver", "(UTC-07:00) Mountain Time (US * Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Myanmar Standard Time", "Asia/Rangoon", "(UTC+06:30) Yangon (Rangoon)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("N. Central Asia Standard Time", "Asia/Novosibirsk", "(UTC+06:00) Novosibirsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("N. Central Asia Standard Time", "Asia/Novosibirsk", "(UTC+06:00) Novosibirsk (RTZ 5)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Namibia Standard Time", "Africa/Windhoek", "(UTC+02:00) Windhoek"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Namibia Standard Time", "Africa/Windhoek", "(UTC+01:00) Windhoek"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Nepal Standard Time", "Asia/Katmandu", "(UTC+05:45) Kathmandu"));
        ZONEMAPPINGS.add(new TimeZoneMapping("New Zealand Standard Time", "Pacific/Auckland", "(UTC+12:00) Auckland, Wellington"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Newfoundland Standard Time", "America/St_Johns", "(UTC-03:30) Newfoundland"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Norfolk Standard Time", "Pacific/Norfolk", "(UTC+11:00) Norfolk Island"));
        ZONEMAPPINGS.add(new TimeZoneMapping("North Asia East Standard Time", "Asia/Irkutsk", "(UTC+08:00) Irkutsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("North Asia East Standard Time", "Asia/Irkutsk", "(UTC+08:00) Irkutsk (RTZ 7)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("North Asia Standard Time", "Asia/Krasnoyarsk", "(UTC+07:00) Krasnoyarsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("North Asia Standard Time", "Asia/Krasnoyarsk", "(UTC+07:00) Krasnoyarsk (RTZ 6)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("North Korea Standard Time", "Asia/Pyongyang", "(UTC+08:30) Pyongyang"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific SA Standard Time", "America/Santiago", "(UTC-04:00) Santiago"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific SA Standard Time", "America/Santiago", "(UTC-03:00) Santiago"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time (Mexico)", "America/Tijuana", "(UTC-08:00) Baja California"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time", "America/Los_Angeles", "(UTC-08:00) Pacific Time (US & Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time", "America/Los_Angeles", "(UTC-08:00) Pacific Time (US %2A Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time", "America/Los_Angeles", "(UTC-08:00) Pacific Time (US * Canada)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time", "America/Los_Angeles", "(UTC-08:00) Pacific Standard Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time", "America/Los_Angeles", "(UTC-08:00) Pacific Time (US & Canada); Tijuana"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time", "America/Los_Angeles", "(UTC-08:00) Pacific Time (US %2A Canada); Tijuana"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time", "America/Los_Angeles", "(UTC-08:00) Pacific Time (US * Canada); Tijuana"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pacific Standard Time", "America/Los_Angeles", "(UTC-08:00) Pacific Standard Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Pakistan Standard Time", "Asia/Karachi", "(UTC+05:00) Islamabad, Karachi"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Paraguay Standard Time", "America/Asuncion", "(UTC-04:00) Asuncion"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Romance Standard Time", "Europe/Paris", "(UTC+01:00) Brussels, Copenhagen, Madrid, Paris"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Russian Standard Time", "Europe/Moscow", "(UTC+03:00) Moscow, St. Petersburg, Volgograd"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Russian Standard Time", "Europe/Moscow", "(UTC+03:00) Moscow, St. Petersburg, Volgograd (RTZ 2)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Russian Standard Time", "Europe/Moscow", "(UTC+03:00) Moscow, St. Petersburg"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Russia Time Zone 3", "Europe/Samara", "(UTC+04:00) Izhevsk, Samara (RTZ 3)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Russia Time Zone 3", "Europe/Samara", "(UTC+04:00) Izhevsk, Samara"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Russia Time Zone 10", "Asia/Srednekolymsk", "(UTC+11:00) Chokurdakh (RTZ 10)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Russia Time Zone 10", "Asia/Srednekolymsk", "(UTC+11:00) Chokurdakh"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Russia Time Zone 11", "Asia/Anadyr", "(UTC+12:00) Anadyr, Petropavlovsk-Kamchatsky"));
        ZONEMAPPINGS.add(new TimeZoneMapping("SA Eastern Standard Time", "America/Cayenne", "(UTC-03:00) Cayenne, Fortaleza"));
        ZONEMAPPINGS.add(new TimeZoneMapping("SA Pacific Standard Time", "America/Bogota", "(UTC-05:00) Bogota, Lima, Quito"));
        ZONEMAPPINGS.add(new TimeZoneMapping("SA Pacific Standard Time", "America/Bogota", "(UTC-05:00) Bogota, Lima, Quito, Rio Branco"));
        ZONEMAPPINGS.add(new TimeZoneMapping("SA Pacific Standard Time", "America/Bogota", "(GMT -05:00) Bogot\u00e1, Lima, Quito, Rio Branco"));
        ZONEMAPPINGS.add(new TimeZoneMapping("SA Western Standard Time", "America/La_Paz", "(UTC-04:00) Georgetown, La Paz, Manaus, San Juan"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Saint Pierre Standard Time", "America/Miquelon", "(UTC-03:00) Saint Pierre and Miquelon"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Sakhalin Standard Time", "Asia/Sakhalin", "(UTC+11:00) Sakhalin"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Samoa Standard Time", "Pacific/Apia", "(UTC-11:00) Samoa"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Samoa Standard Time", "Pacific/Apia", "(UTC+13:00) Samoa"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Saratov Standard Time", "Europe/Saratov", "(UTC+04:00) Saratov"));
        ZONEMAPPINGS.add(new TimeZoneMapping("SE Asia Standard Time", "Asia/Bangkok", "(UTC+07:00) Bangkok, Hanoi, Jakarta"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Singapore Standard Time", "Asia/Singapore", "(UTC+08:00) Kuala Lumpur, Singapore"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Singapore Time", "Asia/Singapore", "(UTC+08:00) Kuala Lumpur, Singapore"));
        ZONEMAPPINGS.add(new TimeZoneMapping("South Africa Standard Time", "Africa/Johannesburg", "(UTC+02:00) Harare, Pretoria"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Sri Lanka Standard Time", "Asia/Colombo", "(UTC+05:30) Sri Jayawardenepura"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Sudan Standard Time", "Africa/Khartoum", "(UTC+02:00) Khartoum"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Syria Standard Time", "Asia/Damascus", "(UTC+02:00) Damascus"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Taipei Standard Time", "Asia/Taipei", "(UTC+08:00) Taipei"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Tasmania Standard Time", "Australia/Hobart", "(UTC+10:00) Hobart"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Tocantins Standard Time", "America/Araguaina", "(UTC-03:00) Araguaina"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Tokyo Standard Time", "Asia/Tokyo", "(UTC+09:00) Osaka, Sapporo, Tokyo"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Tomsk Standard Time", "Asia/Tomsk", "(UTC+07:00) Tomsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Tonga Standard Time", "Pacific/Tongatapu", "(UTC+13:00) Nuku'alofa"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Tonga Standard Time", "Pacific/Tongatapu", "(UTC+14:00) Nuku'alofa Island"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Tomsk Standard Time", "Asia/Tomsk", "(UTC+07:00) Tomsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Transbaikal Standard Time", "Asia/Chita", "(UTC+09:00) Chita"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Turkey Standard Time", "Europe/Istanbul", "(UTC+03:00) Istanbul"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Turks And Caicos Standard Time", "America/Grand_Turk", "(UTC-04:00) Turks and Caicos"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Turks And Caicos Standard Time", "America/Grand_Turk", "(UTC-05:00) Turks and Caicos"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Ulaanbaatar Standard Time", "Asia/Ulaanbaatar", "(UTC+08:00) Ulaanbaatar"));
        ZONEMAPPINGS.add(new TimeZoneMapping("US Eastern Standard Time", "America/Indianapolis", "(UTC-05:00) Indiana (East)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("US Mountain Standard Time", "America/Phoenix", "(UTC-07:00) Arizona"));
        ZONEMAPPINGS.add(new TimeZoneMapping("US Mountain Standard Time", "America/Phoenix", "(UTC-07:00) Yukon"));
        ZONEMAPPINGS.add(new TimeZoneMapping("UTC-08", "Etc/GMT+8", "(UTC-08:00) Coordinated Universal Time-08"));
        ZONEMAPPINGS.add(new TimeZoneMapping("UTC-09", "Pacific/Gambier", "(UTC-09:00) Coordinated Universal Time-09"));
        ZONEMAPPINGS.add(new TimeZoneMapping("UTC+13", "Etc/GMT-13", "(UTC+13:00) Coordinated Universal Time+13"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT ", "Etc/GMT", "(UTC) Coordinated Universal Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT ", "Etc/GMT", "(UTC+00:00) Coordinated Universal Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT ", "Etc/GMT", "UTC"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT +12", "Etc/GMT-12", "(UTC+12:00) Coordinated Universal Time+12"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT -02", "Etc/GMT+2", "(UTC-02:00) Coordinated Universal Time-02"));
        ZONEMAPPINGS.add(new TimeZoneMapping("GMT -11", "Etc/GMT+11", "(UTC-11:00) Coordinated Universal Time-11"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Venezuela Standard Time", "America/Caracas", "(UTC-04:30) Caracas"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Venezuela Standard Time", "America/Caracas", "(UTC-04:00) Caracas"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Venezuela Time", "America/Caracas", "(UTC-04:00) Caracas"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Vladivostok Standard Time", "Asia/Vladivostok", "(UTC+10:00) Vladivostok"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Vladivostok Standard Time", "Asia/Vladivostok", "(UTC+10:00) Vladivostok, Magadan (RTZ 9)"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Australia Standard Time", "Australia/Perth", "(UTC+08:00) Perth"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Central Africa Standard Time", "Africa/Lagos", "(UTC+01:00) West Central Africa"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Mongolia Standard Time", "Asia/Hovd", "(UTC+07:00) Hovd"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Europe Standard Time", "Europe/Berlin", "(UTC+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Europe Standard Time", "Europe/Berlin", "(UTC+01:00) Amsterdam, Berlin, Berne, Rome, Stockholm, Vienne"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Europe Standard Time", "Europe/Berlin", "(UTC+01:00) Amsterdam, Berlino, Berna, Roma, Stoccolma, Vienna"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Europe Standard Time", "Europe/Berlin", "(UTC+01:00) Amsterdam, Berlin, Bern, Rom, Stockholm, Wien"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Europe Standard Time", "Europe/Berlin", "W. Europe Standard Time"));
        ZONEMAPPINGS.add(new TimeZoneMapping("West Asia Standard Time", "Asia/Tashkent", "(UTC+05:00) Tashkent"));
        ZONEMAPPINGS.add(new TimeZoneMapping("West Asia Standard Time", "Asia/Tashkent", "(UTC+05:00) Ashgabat, Tashkent"));
        ZONEMAPPINGS.add(new TimeZoneMapping("West Asia Standard Time", "Asia/Tashkent", "(UTC+05:00) Ashgabat, Toshkent"));
        ZONEMAPPINGS.add(new TimeZoneMapping("West Bank Standard Time", "Asia/Hebron", "(UTC+02:00) Gaza, Hebron"));
        ZONEMAPPINGS.add(new TimeZoneMapping("W. Mongolia Standard Time", "Asia/Hovd", "(UTC+07:00) Hovd"));
        ZONEMAPPINGS.add(new TimeZoneMapping("West Pacific Standard Time", "Pacific/Port_Moresby", "(UTC+10:00) Guam, Port Moresby"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Yakutsk Standard Time", "Asia/Yakutsk", "(UTC+09:00) Yakutsk"));
        ZONEMAPPINGS.add(new TimeZoneMapping("Yakutsk Standard Time", "Asia/Yakutsk", "(UTC+09:00) Yakutsk (RTZ 8)"));
    }

    /**
     * A set of time zone IDs that the JVM supports
     */
    private static final HashSet<String> TIME_ZONE_AVAILABLE_IDS =
                       new HashSet<>(Arrays.asList(TimeZone.getAvailableIDs()));

    /**
     * @return a set of the available time zones in the JDK
     */
    public static Set<String> getAvailableTimeZones()
    {
        return Collections.unmodifiableSet(TIME_ZONE_AVAILABLE_IDS);
    }

    /**
     * A map from the Windows display name to the Olson time zone string.
     */
    private static final Map<String, String> WINDOWS_DISPLAY_TO_OLSON_MAP =
        ZONEMAPPINGS.stream()
                    .filter(zm -> TIME_ZONE_AVAILABLE_IDS.contains(zm.getOlsonName()))
                    .collect(Collectors.toMap(zm -> zm.getWindowsDisplayName(),
                                              zm -> zm.getOlsonName(),
                                              (value1, value2) -> value2));

    /**
     * Gets a map from the Windows display name to the Olson timezone string
     * @return timeZoneMap the map
     */
    public static Map<String, String> getWindowsDisplayToOlsonMap()
    {
        return Collections.unmodifiableMap(WINDOWS_DISPLAY_TO_OLSON_MAP);
    }

    /**
     * A map from the Windows Standard name to the Olson time zone string.
     */
    private static final Map<String, String> WINDOWS_STANDARD_TO_OLSON_MAP =
        ZONEMAPPINGS.stream()
                    .filter(zm -> TIME_ZONE_AVAILABLE_IDS.contains(zm.getOlsonName()))
                    .collect(Collectors.toMap(zm -> zm.getWindowsStandardName(),
                                              zm -> zm.getOlsonName(),
                                              (value1, value2) -> value2));

    /**
     * Gets a map from the Windows standard name to the Olson timezone string
     * @return timeZoneMap the map
     */
    public static Map<String, String> getWindowsStandardToOlsonMap()
    {
        return Collections.unmodifiableMap(WINDOWS_STANDARD_TO_OLSON_MAP);
    }

    private static final class TimeZoneMapping {
        private final String windowsStandardName;
        private final String olsonName;
        private final String windowsDisplayName;
        public TimeZoneMapping(String windowsStandardName, String olsonName,
                String windowsDisplayName) {
            this.windowsStandardName = windowsStandardName;
            this.olsonName = olsonName;
            this.windowsDisplayName = windowsDisplayName;
        }
        public String getWindowsStandardName() {
            return windowsStandardName;
        }
        public String getOlsonName() {
            return olsonName;
        }
        public String getWindowsDisplayName() {
            return windowsDisplayName;
        }
    }
}
