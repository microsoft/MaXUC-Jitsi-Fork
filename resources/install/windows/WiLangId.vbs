' Windows Installer utility to report the language and codepage for a package
' For use with Windows Scripting Host, CScript.exe or WScript.exe
' Copyright (c) Microsoft Corporation. All rights reserved.
' Demonstrates the access of language and codepage values                 
'
Option Explicit

Const msiOpenDatabaseModeReadOnly     = 0
Const msiOpenDatabaseModeTransact     = 1
Const ForReading = 1
Const ForWriting = 2
Const TristateFalse = 0

Const msiViewModifyInsert         = 1
Const msiViewModifyUpdate         = 2
Const msiViewModifyAssign         = 3
Const msiViewModifyReplace        = 4
Const msiViewModifyDelete         = 6

Dim argCount:argCount = Wscript.Arguments.Count
If argCount > 0 Then If InStr(1, Wscript.Arguments(0), "?", vbTextCompare) > 0 Then argCount = 0
If (argCount = 0) Then
	message = "Windows Installer utility to manage language and codepage values for a package." &_
		vbNewLine & "The package language is a summary information property that designates the" &_
		vbNewLine & " primary language and any language transforms that are available, comma delim." &_
		vbNewLine & "The ProductLanguage in the database Property table is the language that is" &_
		vbNewLine & " registered for the product and determines the language used to load resources." &_
		vbNewLine & "The codepage is the ANSI codepage of the database strings, 0 if all ASCII data," &_
		vbNewLine & " and must represent the text data to avoid loss when persisting the database." &_
		vbNewLine & "The 1st argument is the path to MSI database (installer package)" &_
		vbNewLine & "To update a value, the 2nd argument contains the keyword and the 3rd the value:" &_
		vbNewLine & "   Package  {base LangId optionally followed by list of language transforms}" &_
		vbNewLine & "   Product  {LangId of the product (could be updated by language transforms)}" &_
		vbNewLine & "   Codepage {ANSI codepage of text data (use with caution when text exists!)}" &_
		vbNewLine &_
		vbNewLine & "Copyright (C) Microsoft Corporation.  All rights reserved."
	Wscript.Echo message
	Wscript.Quit 1
End If

' Connect to Windows Installer object
On Error Resume Next
Dim installer : Set installer = Nothing
Set installer = Wscript.CreateObject("WindowsInstaller.Installer") : CheckError


' Open database
Dim databasePath:databasePath = Wscript.Arguments(0)
Dim openMode : If argCount >= 3 Then openMode = msiOpenDatabaseModeTransact Else openMode = msiOpenDatabaseModeReadOnly
Dim database : Set database = installer.OpenDatabase(databasePath, openMode) : CheckError

' Update value if supplied
If argCount >= 3 Then
	Dim value:value = Wscript.Arguments(2)
	Select Case UCase(Wscript.Arguments(1))
		Case "PACKAGE"  : SetPackageLanguage database, value
		Case "PRODUCT"  : SetProductLanguage database, value
		Case "CODEPAGE" : SetDatabaseCodepage database, value
		Case Else       : Fail "Invalid value keyword"
	End Select
	CheckError
End If

' Extract language info and compose report message
Dim message:message = "Package language = "         & PackageLanguage(database) &_
					", ProductLanguage = " & ProductLanguage(database) &_
					", Database codepage = "        & DatabaseCodepage(database)
database.Commit : CheckError  ' no effect if opened ReadOnly
Set database = nothing
Wscript.Echo message
Wscript.Quit 0

' Get language list from summary information
Function PackageLanguage(database)
	On Error Resume Next
	Dim sumInfo  : Set sumInfo = database.SummaryInformation(0) : CheckError
	Dim template : template = sumInfo.Property(7) : CheckError
	Dim iDelim:iDelim = InStr(1, template, ";", vbTextCompare)
	If iDelim = 0 Then template = "Not specified!"
	PackageLanguage = Right(template, Len(template) - iDelim)
	If Len(PackageLanguage) = 0 Then PackageLanguage = "0"
End Function

' Get ProductLanguge property from Property table
Function ProductLanguage(database)
	On Error Resume Next
	Dim view : Set view = database.OpenView("SELECT `Value` FROM `Property` WHERE `Property` = 'ProductLanguage'")
	view.Execute : CheckError
	Dim record : Set record = view.Fetch : CheckError
	If record Is Nothing Then ProductLanguage = "Not specified!" Else ProductLanguage = record.IntegerData(1)
End Function

' Get ANSI codepage of database text data
Function DatabaseCodepage(database)
	On Error Resume Next
	Dim WshShell : Set WshShell = Wscript.CreateObject("Wscript.Shell") : CheckError
	Dim tempPath:tempPath = WshShell.ExpandEnvironmentStrings("%TEMP%") : CheckError
	database.Export "_ForceCodepage", tempPath, "codepage.idt" : CheckError
	Dim fileSys : Set fileSys = CreateObject("Scripting.FileSystemObject") : CheckError
	Dim file : Set file = fileSys.OpenTextFile(tempPath & "\codepage.idt", ForReading, False, TristateFalse) : CheckError
	file.ReadLine ' skip column name record
	file.ReadLine ' skip column defn record
	DatabaseCodepage = file.ReadLine
	file.Close
	Dim iDelim:iDelim = InStr(1, DatabaseCodepage, vbTab, vbTextCompare)
	If iDelim = 0 Then Fail "Failure in codepage export file"
	DatabaseCodepage = Left(DatabaseCodepage, iDelim - 1)
	fileSys.DeleteFile(tempPath & "\codepage.idt")
End Function

' Set ProductLanguge property in Property table
Sub SetProductLanguage(database, language)
	On Error Resume Next
	If Not IsNumeric(language) Then Fail "ProductLanguage must be numeric"
	Dim view : Set view = database.OpenView("SELECT `Property`,`Value` FROM `Property`")
	view.Execute : CheckError
	Dim record : Set record = installer.CreateRecord(2)
	record.StringData(1) = "ProductLanguage"
	record.StringData(2) = CStr(language)
	view.Modify msiViewModifyAssign, record : CheckError
End Sub

' Set ANSI codepage of database text data
Sub SetDatabaseCodepage(database, codepage)
	On Error Resume Next
	If Not IsNumeric(codepage) Then Fail "Codepage must be numeric"
	Dim WshShell : Set WshShell = Wscript.CreateObject("Wscript.Shell") : CheckError
	Dim tempPath:tempPath = WshShell.ExpandEnvironmentStrings("%TEMP%") : CheckError
	Dim fileSys : Set fileSys = CreateObject("Scripting.FileSystemObject") : CheckError
	Dim file : Set file = fileSys.OpenTextFile(tempPath & "\codepage.idt", ForWriting, True, TristateFalse) : CheckError
	file.WriteLine ' dummy column name record
	file.WriteLine ' dummy column defn record
	file.WriteLine codepage & vbTab & "_ForceCodepage"
	file.Close : CheckError
	database.Import tempPath, "codepage.idt" : CheckError
	fileSys.DeleteFile(tempPath & "\codepage.idt")
End Sub     

' Set language list in summary information
Sub SetPackageLanguage(database, language)
	On Error Resume Next
	Dim sumInfo  : Set sumInfo = database.SummaryInformation(1) : CheckError
	Dim template : template = sumInfo.Property(7) : CheckError
	Dim iDelim:iDelim = InStr(1, template, ";", vbTextCompare)
	Dim platform : If iDelim = 0 Then platform = ";" Else platform = Left(template, iDelim)
	sumInfo.Property(7) = platform & language
	sumInfo.Persist : CheckError
End Sub

Sub CheckError
	Dim message, errRec
	If Err = 0 Then Exit Sub
	message = Err.Source & " " & Hex(Err) & ": " & Err.Description
	If Not installer Is Nothing Then
		Set errRec = installer.LastErrorRecord
		If Not errRec Is Nothing Then message = message & vbNewLine & errRec.FormatText
	End If
	Fail message
End Sub

Sub Fail(message)
	Wscript.Echo message
	Wscript.Quit 2
End Sub

'' SIG '' Begin signature block
'' SIG '' MIIl2QYJKoZIhvcNAQcCoIIlyjCCJcYCAQExDzANBglg
'' SIG '' hkgBZQMEAgEFADB3BgorBgEEAYI3AgEEoGkwZzAyBgor
'' SIG '' BgEEAYI3AgEeMCQCAQEEEE7wKRaZJ7VNj+Ws4Q8X66sC
'' SIG '' AQACAQACAQACAQACAQAwMTANBglghkgBZQMEAgEFAAQg
'' SIG '' P5ZR+tRLXw+tvFB7cXDc0jFoO6HhZPDQciZh+dfNY5qg
'' SIG '' ggtnMIIE7zCCA9egAwIBAgITMwAABQAn1jJvQ3N7hwAA
'' SIG '' AAAFADANBgkqhkiG9w0BAQsFADB+MQswCQYDVQQGEwJV
'' SIG '' UzETMBEGA1UECBMKV2FzaGluZ3RvbjEQMA4GA1UEBxMH
'' SIG '' UmVkbW9uZDEeMBwGA1UEChMVTWljcm9zb2Z0IENvcnBv
'' SIG '' cmF0aW9uMSgwJgYDVQQDEx9NaWNyb3NvZnQgQ29kZSBT
'' SIG '' aWduaW5nIFBDQSAyMDEwMB4XDTIzMDIxNjIwMTExMVoX
'' SIG '' DTI0MDEzMTIwMTExMVowdDELMAkGA1UEBhMCVVMxEzAR
'' SIG '' BgNVBAgTCldhc2hpbmd0b24xEDAOBgNVBAcTB1JlZG1v
'' SIG '' bmQxHjAcBgNVBAoTFU1pY3Jvc29mdCBDb3Jwb3JhdGlv
'' SIG '' bjEeMBwGA1UEAxMVTWljcm9zb2Z0IENvcnBvcmF0aW9u
'' SIG '' MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA
'' SIG '' xZG5LUzwCcLo1qngBfRIvaoxHBx4YAznAhlyj2RbHnLe
'' SIG '' j+9v3xg+or/b6vesUC5EiND4X15wcARi1JbWcIuTyWgO
'' SIG '' yBcmkD4y2+UwfRBtEe/DHCLjIMkcHiN4w3HueFjzmiQh
'' SIG '' XX4t4Qbx/wKFu7UB9FGvtkMnMWx2YIPPxKZXAWi1jPz6
'' SIG '' 1yE9zdGZg20glsf5mbv8yRA00u2d+0nOWr5AXTmyuB9V
'' SIG '' 1TS4e+IqKd+Mgc4hTV4UPH0drrMugdrn943JD6IB8MpH
'' SIG '' b4dD4m2PC4KueSJbY71fSpR3ekB8XkSejNBGSoCFH3AB
'' SIG '' dMOV1hSWc3jh1gehOTZnclObBOp0LhqRoQIDAQABo4IB
'' SIG '' bjCCAWowHwYDVR0lBBgwFgYKKwYBBAGCNz0GAQYIKwYB
'' SIG '' BQUHAwMwHQYDVR0OBBYEFJ0K2XcwHGE1ocy2q2IIwzoq
'' SIG '' NSkjMEUGA1UdEQQ+MDykOjA4MR4wHAYDVQQLExVNaWNy
'' SIG '' b3NvZnQgQ29ycG9yYXRpb24xFjAUBgNVBAUTDTIzMDg2
'' SIG '' NSs1MDAyMzEwHwYDVR0jBBgwFoAU5vxfe7siAFjkck61
'' SIG '' 9CF0IzLm76wwVgYDVR0fBE8wTTBLoEmgR4ZFaHR0cDov
'' SIG '' L2NybC5taWNyb3NvZnQuY29tL3BraS9jcmwvcHJvZHVj
'' SIG '' dHMvTWljQ29kU2lnUENBXzIwMTAtMDctMDYuY3JsMFoG
'' SIG '' CCsGAQUFBwEBBE4wTDBKBggrBgEFBQcwAoY+aHR0cDov
'' SIG '' L3d3dy5taWNyb3NvZnQuY29tL3BraS9jZXJ0cy9NaWND
'' SIG '' b2RTaWdQQ0FfMjAxMC0wNy0wNi5jcnQwDAYDVR0TAQH/
'' SIG '' BAIwADANBgkqhkiG9w0BAQsFAAOCAQEA4dJD1I1GLc5T
'' SIG '' xLzKBTVx6OGl+UT6XWeK28q1N1K+CyuIVy16DIp18YEp
'' SIG '' 0sbrCcpV3XpqL4N/EZcYmZYGGHNGHO2IHQVkZfc5ngPq
'' SIG '' 4ENLK30ehdc7YKG62MbRzo6E4YlrwXi5mTo1Fba5ryYB
'' SIG '' rtnoXxXg9q5g8/QoCzpMNnhuPdrydKaABUSEWfAbaYAg
'' SIG '' 8M2YJroQKe4SqMMEcjJP6RETgrQNOESzEoZSJE+DSQQx
'' SIG '' NjlQ+Uz9Pw8za9yPIxBgVc6m/0AJSX9TDAUrR82MU0P1
'' SIG '' Hh/Ty/4K9osi1BEd5uPIswZYtePscr4gVQu3AilwAL9e
'' SIG '' 3PPkEdzSny+ceQI6NfGHRTCCBnAwggRYoAMCAQICCmEM
'' SIG '' UkwAAAAAAAMwDQYJKoZIhvcNAQELBQAwgYgxCzAJBgNV
'' SIG '' BAYTAlVTMRMwEQYDVQQIEwpXYXNoaW5ndG9uMRAwDgYD
'' SIG '' VQQHEwdSZWRtb25kMR4wHAYDVQQKExVNaWNyb3NvZnQg
'' SIG '' Q29ycG9yYXRpb24xMjAwBgNVBAMTKU1pY3Jvc29mdCBS
'' SIG '' b290IENlcnRpZmljYXRlIEF1dGhvcml0eSAyMDEwMB4X
'' SIG '' DTEwMDcwNjIwNDAxN1oXDTI1MDcwNjIwNTAxN1owfjEL
'' SIG '' MAkGA1UEBhMCVVMxEzARBgNVBAgTCldhc2hpbmd0b24x
'' SIG '' EDAOBgNVBAcTB1JlZG1vbmQxHjAcBgNVBAoTFU1pY3Jv
'' SIG '' c29mdCBDb3Jwb3JhdGlvbjEoMCYGA1UEAxMfTWljcm9z
'' SIG '' b2Z0IENvZGUgU2lnbmluZyBQQ0EgMjAxMDCCASIwDQYJ
'' SIG '' KoZIhvcNAQEBBQADggEPADCCAQoCggEBAOkOZFB5Z7XE
'' SIG '' 4/0JAEyelKz3VmjqRNjPxVhPqaV2fG1FutM5krSkHvn5
'' SIG '' ZYLkF9KP/UScCOhlk84sVYS/fQjjLiuoQSsYt6JLbklM
'' SIG '' axUH3tHSwokecZTNtX9LtK8I2MyI1msXlDqTziY/7Ob+
'' SIG '' NJhX1R1dSfayKi7VhbtZP/iQtCuDdMorsztG4/BGScEX
'' SIG '' ZlTJHL0dxFViV3L4Z7klIDTeXaallV6rKIDN1bKe5QO1
'' SIG '' Y9OyFMjByIomCll/B+z/Du2AEjVMEqa+Ulv1ptrgiwtI
'' SIG '' d9aFR9UQucboqu6Lai0FXGDGtCpbnCMcX0XjGhQebzfL
'' SIG '' GTOAaolNo2pmY3iT1TDPlR8CAwEAAaOCAeMwggHfMBAG
'' SIG '' CSsGAQQBgjcVAQQDAgEAMB0GA1UdDgQWBBTm/F97uyIA
'' SIG '' WORyTrX0IXQjMubvrDAZBgkrBgEEAYI3FAIEDB4KAFMA
'' SIG '' dQBiAEMAQTALBgNVHQ8EBAMCAYYwDwYDVR0TAQH/BAUw
'' SIG '' AwEB/zAfBgNVHSMEGDAWgBTV9lbLj+iiXGJo0T2UkFvX
'' SIG '' zpoYxDBWBgNVHR8ETzBNMEugSaBHhkVodHRwOi8vY3Js
'' SIG '' Lm1pY3Jvc29mdC5jb20vcGtpL2NybC9wcm9kdWN0cy9N
'' SIG '' aWNSb29DZXJBdXRfMjAxMC0wNi0yMy5jcmwwWgYIKwYB
'' SIG '' BQUHAQEETjBMMEoGCCsGAQUFBzAChj5odHRwOi8vd3d3
'' SIG '' Lm1pY3Jvc29mdC5jb20vcGtpL2NlcnRzL01pY1Jvb0Nl
'' SIG '' ckF1dF8yMDEwLTA2LTIzLmNydDCBnQYDVR0gBIGVMIGS
'' SIG '' MIGPBgkrBgEEAYI3LgMwgYEwPQYIKwYBBQUHAgEWMWh0
'' SIG '' dHA6Ly93d3cubWljcm9zb2Z0LmNvbS9QS0kvZG9jcy9D
'' SIG '' UFMvZGVmYXVsdC5odG0wQAYIKwYBBQUHAgIwNB4yIB0A
'' SIG '' TABlAGcAYQBsAF8AUABvAGwAaQBjAHkAXwBTAHQAYQB0
'' SIG '' AGUAbQBlAG4AdAAuIB0wDQYJKoZIhvcNAQELBQADggIB
'' SIG '' ABp071dPKXvEFoV4uFDTIvwJnayCl/g0/yosl5US5eS/
'' SIG '' z7+TyOM0qduBuNweAL7SNW+v5X95lXflAtTx69jNTh4b
'' SIG '' YaLCWiMa8IyoYlFFZwjjPzwek/gwhRfIOUCm1w6zISnl
'' SIG '' paFpjCKTzHSY56FHQ/JTrMAPMGl//tIlIG1vYdPfB9XZ
'' SIG '' cgAsaYZ2PVHbpjlIyTdhbQfdUxnLp9Zhwr/ig6sP4Gub
'' SIG '' ldZ9KFGwiUpRpJpsyLcfShoOaanX3MF+0Ulwqratu3JH
'' SIG '' Yxf6ptaipobsqBBEm2O2smmJBsdGhnoYP+jFHSHVe/kC
'' SIG '' Iy3FQcu/HUzIFu+xnH/8IktJim4V46Z/dlvRU3mRhZ3V
'' SIG '' 0ts9czXzPK5UslJHasCqE5XSjhHamWdeMoz7N4XR3HWF
'' SIG '' nIfGWleFwr/dDY+Mmy3rtO7PJ9O1Xmn6pBYEAackZ3PP
'' SIG '' TU+23gVWl3r36VJN9HcFT4XG2Avxju1CCdENduMjVngi
'' SIG '' Jja+yrGMbqod5IXaRzNij6TJkTNfcR5Ar5hlySLoQiEl
'' SIG '' ihwtYNk3iUGJKhYP12E8lGhgUu/WR5mggEDuFYF3Ppzg
'' SIG '' UxgaUB04lZseZjMTJzkXeIc2zk7DX7L1PUdTtuDl2wth
'' SIG '' PSrXkizON1o+QEIxpB8QCMJWnL8kXVECnWp50hfT2sGU
'' SIG '' jgd7JXFEqwZq5tTG3yOalnXFMYIZyjCCGcYCAQEwgZUw
'' SIG '' fjELMAkGA1UEBhMCVVMxEzARBgNVBAgTCldhc2hpbmd0
'' SIG '' b24xEDAOBgNVBAcTB1JlZG1vbmQxHjAcBgNVBAoTFU1p
'' SIG '' Y3Jvc29mdCBDb3Jwb3JhdGlvbjEoMCYGA1UEAxMfTWlj
'' SIG '' cm9zb2Z0IENvZGUgU2lnbmluZyBQQ0EgMjAxMAITMwAA
'' SIG '' BQAn1jJvQ3N7hwAAAAAFADANBglghkgBZQMEAgEFAKCC
'' SIG '' AQQwGQYJKoZIhvcNAQkDMQwGCisGAQQBgjcCAQQwHAYK
'' SIG '' KwYBBAGCNwIBCzEOMAwGCisGAQQBgjcCARUwLwYJKoZI
'' SIG '' hvcNAQkEMSIEIACfHEUjpbb6OMsQM89w72uZFdkYNMUa
'' SIG '' oednrszUaXmzMDwGCisGAQQBgjcKAxwxLgwsc1BZN3hQ
'' SIG '' QjdoVDVnNUhIcll0OHJETFNNOVZ1WlJ1V1phZWYyZTIy
'' SIG '' UnM1ND0wWgYKKwYBBAGCNwIBDDFMMEqgJIAiAE0AaQBj
'' SIG '' AHIAbwBzAG8AZgB0ACAAVwBpAG4AZABvAHcAc6EigCBo
'' SIG '' dHRwOi8vd3d3Lm1pY3Jvc29mdC5jb20vd2luZG93czAN
'' SIG '' BgkqhkiG9w0BAQEFAASCAQBUK6E2EOfyERHUsk1SGcvH
'' SIG '' w9yTmi8YuxPICjjKnpF/aPhzkQEkDwzSDb4VZ2cDJzL3
'' SIG '' +x5zh4CKx+JpoDQ+UVIoGPUQMp4/IPbK4QEnMzi1g+kX
'' SIG '' aok/YQ6AbL33/EIdZF2lvlAoTvIXltoDrzexoJJSeo5L
'' SIG '' 7WA+ZQdCy0fiBoXo8ui0v/4/gx6MkpWN6BItrp3TIazY
'' SIG '' 2/eXNvJhSQ79WJntUCNquF9+QO/IDC+GU17CPye4XZxS
'' SIG '' GtA/maQ100B+ri1YRBj/Bojsq+3CS4FcRZ6gxcejSJLn
'' SIG '' j/ib2iWkT+OsvvBDjKO8lng56yaqFtS0L7WBYjrtCM64
'' SIG '' H/Brbc8IR+cjoYIW/TCCFvkGCisGAQQBgjcDAwExghbp
'' SIG '' MIIW5QYJKoZIhvcNAQcCoIIW1jCCFtICAQMxDzANBglg
'' SIG '' hkgBZQMEAgEFADCCAVEGCyqGSIb3DQEJEAEEoIIBQASC
'' SIG '' ATwwggE4AgEBBgorBgEEAYRZCgMBMDEwDQYJYIZIAWUD
'' SIG '' BAIBBQAEILtTERItCwYeD7bybxf6fyvZF82Y+XWAehe0
'' SIG '' YGjoKeG9AgZkXNxQhUQYEzIwMjMwNTEyMTEzMDQyLjY5
'' SIG '' NFowBIACAfSggdCkgc0wgcoxCzAJBgNVBAYTAlVTMRMw
'' SIG '' EQYDVQQIEwpXYXNoaW5ndG9uMRAwDgYDVQQHEwdSZWRt
'' SIG '' b25kMR4wHAYDVQQKExVNaWNyb3NvZnQgQ29ycG9yYXRp
'' SIG '' b24xJTAjBgNVBAsTHE1pY3Jvc29mdCBBbWVyaWNhIE9w
'' SIG '' ZXJhdGlvbnMxJjAkBgNVBAsTHVRoYWxlcyBUU1MgRVNO
'' SIG '' OjNCQkQtRTMzOC1FOUExMSUwIwYDVQQDExxNaWNyb3Nv
'' SIG '' ZnQgVGltZS1TdGFtcCBTZXJ2aWNloIIRVDCCBwwwggT0
'' SIG '' oAMCAQICEzMAAAHGMM0u1tOhwPQAAQAAAcYwDQYJKoZI
'' SIG '' hvcNAQELBQAwfDELMAkGA1UEBhMCVVMxEzARBgNVBAgT
'' SIG '' Cldhc2hpbmd0b24xEDAOBgNVBAcTB1JlZG1vbmQxHjAc
'' SIG '' BgNVBAoTFU1pY3Jvc29mdCBDb3Jwb3JhdGlvbjEmMCQG
'' SIG '' A1UEAxMdTWljcm9zb2Z0IFRpbWUtU3RhbXAgUENBIDIw
'' SIG '' MTAwHhcNMjIxMTA0MTkwMTM0WhcNMjQwMjAyMTkwMTM0
'' SIG '' WjCByjELMAkGA1UEBhMCVVMxEzARBgNVBAgTCldhc2hp
'' SIG '' bmd0b24xEDAOBgNVBAcTB1JlZG1vbmQxHjAcBgNVBAoT
'' SIG '' FU1pY3Jvc29mdCBDb3Jwb3JhdGlvbjElMCMGA1UECxMc
'' SIG '' TWljcm9zb2Z0IEFtZXJpY2EgT3BlcmF0aW9uczEmMCQG
'' SIG '' A1UECxMdVGhhbGVzIFRTUyBFU046M0JCRC1FMzM4LUU5
'' SIG '' QTExJTAjBgNVBAMTHE1pY3Jvc29mdCBUaW1lLVN0YW1w
'' SIG '' IFNlcnZpY2UwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAw
'' SIG '' ggIKAoICAQDvvSI6vq/geTWbdJmP7UFH+K6h+/5/p5Vv
'' SIG '' sdzbVjHMDOujgbqQpcXjtgCwSCtZZPWiC+nQiugWbwJ1
'' SIG '' FlN/8OVDR9s0072kIDTUonTfMfzYKWaT3N72vWM8nVkl
'' SIG '' oyexmYDLtWlj2Y2pf12E++dbX9nFtuIe/urDCDD1TZJP
'' SIG '' mZ9yk+62wj9Cv+AsLppMjdQJjOJU9n9B9qDw1CEqSkdk
'' SIG '' 7cqvmvzdzLuPPg5Y/LkzZaK1a/lsknmsFNbnXxA8TMXD
'' SIG '' Orx7w/vbYJYpkkWM3x60GCwrTmAd4do32SaWlgkkvzi/
'' SIG '' 0mJpfs0UmQ5GECkQVmJQhpmgvEm3ilwEPN/5YP1QCNEo
'' SIG '' KsCx4n9yTNC86f3lfg63hqyc642FwJ1xBZytmjKQWYRq
'' SIG '' hiSuwPuf/icUUfAkMpRoFhlkvA+Pu7HjxLVh75wxxwzF
'' SIG '' 1FKO6gbiuomqkR3qDN/Pbf2/fov4u06VCF8vlydyWE1J
'' SIG '' Z2YrDVMfJ6Qf3pE206kgTtz71Oey/VoT2GmF6Ms4nF+x
'' SIG '' dOTLDQUh2KVzQI/vPNSypoIYXaYVdHAviN9fVHJXtAYo
'' SIG '' R46m8ZmpAosdVlssPfbO1bwt+/33FDbh39MjE70tF64e
'' SIG '' yfCi2f7wGwKvO77/bi85wD1dyl3uQh5bjOZTGEWy/goJ
'' SIG '' +Koym1mGEwADRKoO6PbdyPXSyZdE4tSeFQIDAQABo4IB
'' SIG '' NjCCATIwHQYDVR0OBBYEFHFf+UeJKEQKnWfaUxrobW4u
'' SIG '' 82CUMB8GA1UdIwQYMBaAFJ+nFV0AXmJdg/Tl0mWnG1M1
'' SIG '' GelyMF8GA1UdHwRYMFYwVKBSoFCGTmh0dHA6Ly93d3cu
'' SIG '' bWljcm9zb2Z0LmNvbS9wa2lvcHMvY3JsL01pY3Jvc29m
'' SIG '' dCUyMFRpbWUtU3RhbXAlMjBQQ0ElMjAyMDEwKDEpLmNy
'' SIG '' bDBsBggrBgEFBQcBAQRgMF4wXAYIKwYBBQUHMAKGUGh0
'' SIG '' dHA6Ly93d3cubWljcm9zb2Z0LmNvbS9wa2lvcHMvY2Vy
'' SIG '' dHMvTWljcm9zb2Z0JTIwVGltZS1TdGFtcCUyMFBDQSUy
'' SIG '' MDIwMTAoMSkuY3J0MAwGA1UdEwEB/wQCMAAwEwYDVR0l
'' SIG '' BAwwCgYIKwYBBQUHAwgwDQYJKoZIhvcNAQELBQADggIB
'' SIG '' AAvMXvbiNe6ANTjzo8wFhHsJzpoevackOcayeSrBliaN
'' SIG '' GLbyq/pLUvLvvbPCbkMjXz3OABD33GESNbq5iStflSu1
'' SIG '' W7slRA/psEEEn3xzbwUAg8grd+RA0K/avFGN9AwlJ1zC
'' SIG '' wl5Mrst3T064DmFjg9YIGAml9jvUtxpfPcVHwA08VfrN
'' SIG '' wphuBg5mt6C2kO5vfg3RCFHvBz8VyZX6Dgjch1MCgwPb
'' SIG '' 9Yjlmx8pPMFSf9TcClSE3Bs6XlhIL5/1LUtK1tkvA/Mx
'' SIG '' L58s9clRJ7tJK+yl9Kyv9UR7ShCGZpH7m9yr7swvDzrV
'' SIG '' YFWFikntMHlgFLk5E71d0htylsEXBwc+ZvyJmpIipb0m
'' SIG '' mAbvr7k1BQs9XNnvnPlbZHlmLJCS2IekzCNfY47b1nz6
'' SIG '' dPDa06xUJzDMf0ugQt52/c+NylvA7IuO2bVPhcdh3ept
'' SIG '' 30NegGM1iRKN2Lfuk2nny76shOW0so6ONAInCPUWme4F
'' SIG '' jzbkHkLS4L81gRIQqxOJwSOFL/i6MFctw0YOFUGXa8cT
'' SIG '' qpj9hbiTLW9zKm9SuwbzWCm/b7z+KE7CDjBMs7teqKR4
'' SIG '' iJTdlYBQCg6lOXXi151CrFsdMO94lhHc5TTIoHbHB/zs
'' SIG '' RYIBvQImKaEObJBooS9JXR8tb2JXIjTBhwbhXZpU3pOt
'' SIG '' niav599qoNAP0X4ek+E/SmUDMIIHcTCCBVmgAwIBAgIT
'' SIG '' MwAAABXF52ueAptJmQAAAAAAFTANBgkqhkiG9w0BAQsF
'' SIG '' ADCBiDELMAkGA1UEBhMCVVMxEzARBgNVBAgTCldhc2hp
'' SIG '' bmd0b24xEDAOBgNVBAcTB1JlZG1vbmQxHjAcBgNVBAoT
'' SIG '' FU1pY3Jvc29mdCBDb3Jwb3JhdGlvbjEyMDAGA1UEAxMp
'' SIG '' TWljcm9zb2Z0IFJvb3QgQ2VydGlmaWNhdGUgQXV0aG9y
'' SIG '' aXR5IDIwMTAwHhcNMjEwOTMwMTgyMjI1WhcNMzAwOTMw
'' SIG '' MTgzMjI1WjB8MQswCQYDVQQGEwJVUzETMBEGA1UECBMK
'' SIG '' V2FzaGluZ3RvbjEQMA4GA1UEBxMHUmVkbW9uZDEeMBwG
'' SIG '' A1UEChMVTWljcm9zb2Z0IENvcnBvcmF0aW9uMSYwJAYD
'' SIG '' VQQDEx1NaWNyb3NvZnQgVGltZS1TdGFtcCBQQ0EgMjAx
'' SIG '' MDCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIB
'' SIG '' AOThpkzntHIhC3miy9ckeb0O1YLT/e6cBwfSqWxOdcjK
'' SIG '' NVf2AX9sSuDivbk+F2Az/1xPx2b3lVNxWuJ+Slr+uDZn
'' SIG '' hUYjDLWNE893MsAQGOhgfWpSg0S3po5GawcU88V29YZQ
'' SIG '' 3MFEyHFcUTE3oAo4bo3t1w/YJlN8OWECesSq/XJprx2r
'' SIG '' rPY2vjUmZNqYO7oaezOtgFt+jBAcnVL+tuhiJdxqD89d
'' SIG '' 9P6OU8/W7IVWTe/dvI2k45GPsjksUZzpcGkNyjYtcI4x
'' SIG '' yDUoveO0hyTD4MmPfrVUj9z6BVWYbWg7mka97aSueik3
'' SIG '' rMvrg0XnRm7KMtXAhjBcTyziYrLNueKNiOSWrAFKu75x
'' SIG '' qRdbZ2De+JKRHh09/SDPc31BmkZ1zcRfNN0Sidb9pSB9
'' SIG '' fvzZnkXftnIv231fgLrbqn427DZM9ituqBJR6L8FA6PR
'' SIG '' c6ZNN3SUHDSCD/AQ8rdHGO2n6Jl8P0zbr17C89XYcz1D
'' SIG '' TsEzOUyOArxCaC4Q6oRRRuLRvWoYWmEBc8pnol7XKHYC
'' SIG '' 4jMYctenIPDC+hIK12NvDMk2ZItboKaDIV1fMHSRlJTY
'' SIG '' uVD5C4lh8zYGNRiER9vcG9H9stQcxWv2XFJRXRLbJbqv
'' SIG '' UAV6bMURHXLvjflSxIUXk8A8FdsaN8cIFRg/eKtFtvUe
'' SIG '' h17aj54WcmnGrnu3tz5q4i6tAgMBAAGjggHdMIIB2TAS
'' SIG '' BgkrBgEEAYI3FQEEBQIDAQABMCMGCSsGAQQBgjcVAgQW
'' SIG '' BBQqp1L+ZMSavoKRPEY1Kc8Q/y8E7jAdBgNVHQ4EFgQU
'' SIG '' n6cVXQBeYl2D9OXSZacbUzUZ6XIwXAYDVR0gBFUwUzBR
'' SIG '' BgwrBgEEAYI3TIN9AQEwQTA/BggrBgEFBQcCARYzaHR0
'' SIG '' cDovL3d3dy5taWNyb3NvZnQuY29tL3BraW9wcy9Eb2Nz
'' SIG '' L1JlcG9zaXRvcnkuaHRtMBMGA1UdJQQMMAoGCCsGAQUF
'' SIG '' BwMIMBkGCSsGAQQBgjcUAgQMHgoAUwB1AGIAQwBBMAsG
'' SIG '' A1UdDwQEAwIBhjAPBgNVHRMBAf8EBTADAQH/MB8GA1Ud
'' SIG '' IwQYMBaAFNX2VsuP6KJcYmjRPZSQW9fOmhjEMFYGA1Ud
'' SIG '' HwRPME0wS6BJoEeGRWh0dHA6Ly9jcmwubWljcm9zb2Z0
'' SIG '' LmNvbS9wa2kvY3JsL3Byb2R1Y3RzL01pY1Jvb0NlckF1
'' SIG '' dF8yMDEwLTA2LTIzLmNybDBaBggrBgEFBQcBAQROMEww
'' SIG '' SgYIKwYBBQUHMAKGPmh0dHA6Ly93d3cubWljcm9zb2Z0
'' SIG '' LmNvbS9wa2kvY2VydHMvTWljUm9vQ2VyQXV0XzIwMTAt
'' SIG '' MDYtMjMuY3J0MA0GCSqGSIb3DQEBCwUAA4ICAQCdVX38
'' SIG '' Kq3hLB9nATEkW+Geckv8qW/qXBS2Pk5HZHixBpOXPTEz
'' SIG '' tTnXwnE2P9pkbHzQdTltuw8x5MKP+2zRoZQYIu7pZmc6
'' SIG '' U03dmLq2HnjYNi6cqYJWAAOwBb6J6Gngugnue99qb74p
'' SIG '' y27YP0h1AdkY3m2CDPVtI1TkeFN1JFe53Z/zjj3G82jf
'' SIG '' ZfakVqr3lbYoVSfQJL1AoL8ZthISEV09J+BAljis9/kp
'' SIG '' icO8F7BUhUKz/AyeixmJ5/ALaoHCgRlCGVJ1ijbCHcNh
'' SIG '' cy4sa3tuPywJeBTpkbKpW99Jo3QMvOyRgNI95ko+ZjtP
'' SIG '' u4b6MhrZlvSP9pEB9s7GdP32THJvEKt1MMU0sHrYUP4K
'' SIG '' WN1APMdUbZ1jdEgssU5HLcEUBHG/ZPkkvnNtyo4JvbMB
'' SIG '' V0lUZNlz138eW0QBjloZkWsNn6Qo3GcZKCS6OEuabvsh
'' SIG '' VGtqRRFHqfG3rsjoiV5PndLQTHa1V1QJsWkBRH58oWFs
'' SIG '' c/4Ku+xBZj1p/cvBQUl+fpO+y/g75LcVv7TOPqUxUYS8
'' SIG '' vwLBgqJ7Fx0ViY1w/ue10CgaiQuPNtq6TPmb/wrpNPgk
'' SIG '' NWcr4A245oyZ1uEi6vAnQj0llOZ0dFtq0Z4+7X6gMTN9
'' SIG '' vMvpe784cETRkPHIqzqKOghif9lwY1NNje6CbaUFEMFx
'' SIG '' BmoQtB1VM1izoXBm8qGCAsswggI0AgEBMIH4oYHQpIHN
'' SIG '' MIHKMQswCQYDVQQGEwJVUzETMBEGA1UECBMKV2FzaGlu
'' SIG '' Z3RvbjEQMA4GA1UEBxMHUmVkbW9uZDEeMBwGA1UEChMV
'' SIG '' TWljcm9zb2Z0IENvcnBvcmF0aW9uMSUwIwYDVQQLExxN
'' SIG '' aWNyb3NvZnQgQW1lcmljYSBPcGVyYXRpb25zMSYwJAYD
'' SIG '' VQQLEx1UaGFsZXMgVFNTIEVTTjozQkJELUUzMzgtRTlB
'' SIG '' MTElMCMGA1UEAxMcTWljcm9zb2Z0IFRpbWUtU3RhbXAg
'' SIG '' U2VydmljZaIjCgEBMAcGBSsOAwIaAxUALTXK5iYhW+yi
'' SIG '' RJpwmZZ7wy7ZAW2ggYMwgYCkfjB8MQswCQYDVQQGEwJV
'' SIG '' UzETMBEGA1UECBMKV2FzaGluZ3RvbjEQMA4GA1UEBxMH
'' SIG '' UmVkbW9uZDEeMBwGA1UEChMVTWljcm9zb2Z0IENvcnBv
'' SIG '' cmF0aW9uMSYwJAYDVQQDEx1NaWNyb3NvZnQgVGltZS1T
'' SIG '' dGFtcCBQQ0EgMjAxMDANBgkqhkiG9w0BAQUFAAIFAOgI
'' SIG '' A4wwIhgPMjAyMzA1MTIwODE1MDhaGA8yMDIzMDUxMzA4
'' SIG '' MTUwOFowdDA6BgorBgEEAYRZCgQBMSwwKjAKAgUA6AgD
'' SIG '' jAIBADAHAgEAAgIN9jAHAgEAAgIRzDAKAgUA6AlVDAIB
'' SIG '' ADA2BgorBgEEAYRZCgQCMSgwJjAMBgorBgEEAYRZCgMC
'' SIG '' oAowCAIBAAIDB6EgoQowCAIBAAIDAYagMA0GCSqGSIb3
'' SIG '' DQEBBQUAA4GBAAG4Om7J3FeJvvl3+GC5NB8lODoP1W3g
'' SIG '' OKFdqSlkYkn0/TqzsUi1kDlkOPRGdI1GUxPncPOYz/lW
'' SIG '' fCQkmTGvkYsW0A6tBpi2jYEilo6ensgniuNNKt/yfmg1
'' SIG '' QlDtjMnh0vgbETUvl3OFqNlPFiwmAIxq7hO8Nsn5q4/e
'' SIG '' AcRT0SuCMYIEDTCCBAkCAQEwgZMwfDELMAkGA1UEBhMC
'' SIG '' VVMxEzARBgNVBAgTCldhc2hpbmd0b24xEDAOBgNVBAcT
'' SIG '' B1JlZG1vbmQxHjAcBgNVBAoTFU1pY3Jvc29mdCBDb3Jw
'' SIG '' b3JhdGlvbjEmMCQGA1UEAxMdTWljcm9zb2Z0IFRpbWUt
'' SIG '' U3RhbXAgUENBIDIwMTACEzMAAAHGMM0u1tOhwPQAAQAA
'' SIG '' AcYwDQYJYIZIAWUDBAIBBQCgggFKMBoGCSqGSIb3DQEJ
'' SIG '' AzENBgsqhkiG9w0BCRABBDAvBgkqhkiG9w0BCQQxIgQg
'' SIG '' MduvpCDKpdVaVpU5UfvmUyqVhQtrQ0eHUGFPatQhS8Aw
'' SIG '' gfoGCyqGSIb3DQEJEAIvMYHqMIHnMIHkMIG9BCBWMRNc
'' SIG '' Vcm9mCnGJmqT8HANYDk/HDqF6FQumQWv2uOvLTCBmDCB
'' SIG '' gKR+MHwxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpXYXNo
'' SIG '' aW5ndG9uMRAwDgYDVQQHEwdSZWRtb25kMR4wHAYDVQQK
'' SIG '' ExVNaWNyb3NvZnQgQ29ycG9yYXRpb24xJjAkBgNVBAMT
'' SIG '' HU1pY3Jvc29mdCBUaW1lLVN0YW1wIFBDQSAyMDEwAhMz
'' SIG '' AAABxjDNLtbTocD0AAEAAAHGMCIEID3Oii/OzPXnMY18
'' SIG '' Zx522mBkZs2PINjV4Mt13pIt9ujDMA0GCSqGSIb3DQEB
'' SIG '' CwUABIICAEGkND7Mpn/svhYm3K2Uvm+naPlm2Pl6B9G4
'' SIG '' KPZ2YVX1DtBMx8A71+bPMoNQvNX55ih0E5bPYEIn8M5t
'' SIG '' p7xnTlP9xkEcxOqTSqeKxZu/6pkKZEDQBPm/8PgAcr2x
'' SIG '' MpOLqxEc237RSNrzHuPfWGdOrezCfGQrzbROMQT/QDRC
'' SIG '' 0chdlwTftpeRBAq3Y80UifT42G9yeH4RldqoNYLKSJTz
'' SIG '' GDJT0yUw7o1yuiKV+a4ap3MUmWFsGS74e+BckJ6nLmTW
'' SIG '' rXZ7ICNzV8I0hC3iVFeoDTAUP/QTWCPDfsHsCN5LEfRB
'' SIG '' kBOXE94ypDijhO2T04U4CWQiRAmTeFyA40Wn9/A1l8fH
'' SIG '' KccrNUGqMTnL8ZmGldNObmQFGcmldj8kqKhoyK8NF8j1
'' SIG '' C7mIoZseUAyMSSr20p9aIvwHwmUedAPJLJMx+jA5qWM4
'' SIG '' dwFjczo6oBW1XoG/HQZ0IPgiigaTu+Evv2oD1e9tOG/9
'' SIG '' v/7srlwb5ayqHt8bcut8ed3E2pu5W15ZvX5nCJ2y6K8+
'' SIG '' GGovr8yGh9FbcyL8uLWsHcCKgCLGew8BuPMO57RedmT4
'' SIG '' TAHgz6T1MRF3OdS49h7tuJ9g3yZq547igyT8fjdjUJ3H
'' SIG '' K+3sz2OSVrYchjWkHv3APyqDqZZiA1N+jHKq/Hkc7MvO
'' SIG '' iVzstvhLpl2hE2inelEscpUZt/119vGs
'' SIG '' End signature block
